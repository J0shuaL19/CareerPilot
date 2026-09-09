use std::{
    env,
    fs::{self, OpenOptions},
    io::{Read, Write},
    net::{IpAddr, Ipv4Addr, SocketAddr, TcpListener, TcpStream},
    path::{Path, PathBuf},
    process::{Child, Command, Stdio},
    sync::Mutex,
    thread,
    time::{Duration, Instant},
};
use tauri::{path::BaseDirectory, App, Manager};

const STARTUP_TIMEOUT: Duration = Duration::from_secs(60);
const HEALTH_RETRY_DELAY: Duration = Duration::from_millis(250);
const HEALTH_IO_TIMEOUT: Duration = Duration::from_secs(1);

pub(crate) struct BackendLaunch {
    pub(crate) child: Child,
    pub(crate) port: u16,
}

#[derive(Default)]
pub(crate) struct BackendState {
    child: Mutex<Option<Child>>,
}

impl BackendState {
    pub(crate) fn store(&self, child: Child) -> Result<(), String> {
        let mut slot = self
            .child
            .lock()
            .map_err(|_| "backend process state is unavailable".to_string())?;
        if slot.is_some() {
            return Err("backend process is already running".to_string());
        }
        *slot = Some(child);
        Ok(())
    }

    fn exit_description(&self) -> Result<Option<String>, String> {
        let mut slot = self
            .child
            .lock()
            .map_err(|_| "backend process state is unavailable".to_string())?;
        match slot.as_mut() {
            Some(child) => child
                .try_wait()
                .map(|status| status.map(|value| value.to_string()))
                .map_err(|error| format!("could not inspect the backend process: {error}")),
            None => Ok(Some("not running".to_string())),
        }
    }

    pub(crate) fn stop(&self) {
        let Ok(mut slot) = self.child.lock() else {
            return;
        };
        let Some(mut child) = slot.take() else {
            return;
        };

        if child.try_wait().ok().flatten().is_none() {
            let _ = child.kill();
        }
        let _ = child.wait();
    }
}

pub(crate) fn launch(app: &App) -> Result<BackendLaunch, String> {
    let app_data_root = desktop_data_root(app)?;
    let data_directory = app_data_root.join("data");
    let log_directory = app_data_root.join("logs");
    fs::create_dir_all(&data_directory)
        .map_err(|error| format!("could not create the data directory: {error}"))?;
    fs::create_dir_all(&log_directory)
        .map_err(|error| format!("could not create the log directory: {error}"))?;

    let java = resolve_resource(app, "runtime/bin/java.exe")?;
    let backend = resolve_resource(app, "backend/careerpilot-backend.jar")?;
    let port = available_port()?;
    let database_url = h2_database_url(&data_directory.join("careerpilot"));
    let application_log = log_directory.join("careerpilot.log");
    let launcher_log = log_directory.join("launcher.log");
    let mut stdout = OpenOptions::new()
        .create(true)
        .append(true)
        .open(&launcher_log)
        .map_err(|error| format!("could not open the launcher log: {error}"))?;
    writeln!(
        stdout,
        "Starting bundled backend\nJava: {}\nJAR: {}\nPort: {port}",
        java.display(),
        backend.display()
    )
    .and_then(|_| stdout.flush())
    .map_err(|error| format!("could not write the launcher log: {error}"))?;
    let stderr = stdout
        .try_clone()
        .map_err(|error| format!("could not open the launcher error log: {error}"))?;

    let mut command = Command::new(java);
    command
        .arg("-Dfile.encoding=UTF-8")
        .arg("-jar")
        .arg(backend)
        .arg("--spring.profiles.active=desktop")
        .arg(format!("--server.port={port}"))
        .env("CAREERPILOT_DESKTOP_DB_URL", database_url)
        .env("CAREERPILOT_DESKTOP_LOG_FILE", application_log)
        .stdin(Stdio::null())
        .stdout(Stdio::from(stdout))
        .stderr(Stdio::from(stderr));

    #[cfg(windows)]
    {
        use std::os::windows::process::CommandExt;
        const CREATE_NO_WINDOW: u32 = 0x0800_0000;
        command.creation_flags(CREATE_NO_WINDOW);
    }

    let child = command
        .spawn()
        .map_err(|error| format!("could not start the bundled backend: {error}"))?;
    Ok(BackendLaunch { child, port })
}

pub(crate) fn wait_until_ready(state: &BackendState, port: u16) -> Result<(), String> {
    let deadline = Instant::now() + STARTUP_TIMEOUT;
    while Instant::now() < deadline {
        if let Some(status) = state.exit_description()? {
            return Err(format!(
                "the local service exited before startup completed ({status})"
            ));
        }
        if health_check(port).unwrap_or(false) {
            return Ok(());
        }
        thread::sleep(HEALTH_RETRY_DELAY);
    }
    Err("the local service did not become ready within 60 seconds".to_string())
}

fn desktop_data_root(app: &App) -> Result<PathBuf, String> {
    if let Some(override_path) = env::var_os("CAREERPILOT_DESKTOP_HOME") {
        return Ok(PathBuf::from(override_path));
    }
    app.path()
        .local_data_dir()
        .map(|path| path.join("CareerPilot"))
        .map_err(|error| format!("could not resolve the local data directory: {error}"))
}

fn resolve_resource(app: &App, relative_path: &str) -> Result<PathBuf, String> {
    let bundled_path = app
        .path()
        .resolve(relative_path, BaseDirectory::Resource)
        .map_err(|error| format!("could not resolve resource {relative_path}: {error}"))?;
    if bundled_path.is_file() {
        return Ok(process_compatible_path(bundled_path));
    }

    #[cfg(debug_assertions)]
    {
        let development_path = PathBuf::from(env!("CARGO_MANIFEST_DIR"))
            .join("resources")
            .join(relative_path);
        if development_path.is_file() {
            return Ok(process_compatible_path(development_path));
        }
    }

    Err(format!("bundled resource is missing: {relative_path}"))
}

#[cfg(windows)]
fn process_compatible_path(path: PathBuf) -> PathBuf {
    let value = path.to_string_lossy();
    if let Some(unc_path) = value.strip_prefix(r"\\?\UNC\") {
        return PathBuf::from(format!(r"\\{unc_path}"));
    }
    if let Some(local_path) = value.strip_prefix(r"\\?\") {
        return PathBuf::from(local_path);
    }
    path
}

#[cfg(not(windows))]
fn process_compatible_path(path: PathBuf) -> PathBuf {
    path
}
fn available_port() -> Result<u16, String> {
    let listener = TcpListener::bind((Ipv4Addr::LOCALHOST, 0))
        .map_err(|error| format!("could not reserve a local port: {error}"))?;
    listener
        .local_addr()
        .map(|address| address.port())
        .map_err(|error| format!("could not read the reserved local port: {error}"))
}

fn h2_database_url(database_path: &Path) -> String {
    let normalized_path = database_path.to_string_lossy().replace('\\', "/");
    format!(
        "jdbc:h2:file:{normalized_path};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_ON_EXIT=FALSE"
    )
}

fn health_check(port: u16) -> std::io::Result<bool> {
    let address = SocketAddr::new(IpAddr::V4(Ipv4Addr::LOCALHOST), port);
    let mut stream = TcpStream::connect_timeout(&address, HEALTH_IO_TIMEOUT)?;
    stream.set_read_timeout(Some(HEALTH_IO_TIMEOUT))?;
    stream.set_write_timeout(Some(HEALTH_IO_TIMEOUT))?;
    stream
        .write_all(b"GET /api/health HTTP/1.1\r\nHost: 127.0.0.1\r\nConnection: close\r\n\r\n")?;

    let mut response = String::new();
    stream.take(16 * 1024).read_to_string(&mut response)?;
    Ok(is_healthy_response(&response))
}

fn is_healthy_response(response: &str) -> bool {
    let Some((headers, body)) = response.split_once("\r\n\r\n") else {
        return false;
    };
    headers
        .lines()
        .next()
        .is_some_and(|status| status.contains(" 200 "))
        && body.contains("\"status\":\"ok\"")
        && body.contains("\"service\":\"careerpilot-backend\"")
}

#[cfg(test)]
mod tests {
    use super::{h2_database_url, is_healthy_response, process_compatible_path};
    use std::path::{Path, PathBuf};

    #[test]
    fn recognizes_the_expected_health_response() {
        let response = concat!(
            "HTTP/1.1 200 OK\r\n",
            "Content-Type: application/json\r\n",
            "\r\n",
            "{\"service\":\"careerpilot-backend\",\"status\":\"ok\"}"
        );
        assert!(is_healthy_response(response));
    }

    #[test]
    fn rejects_an_unhealthy_or_unrelated_response() {
        assert!(!is_healthy_response(
            "HTTP/1.1 503 Service Unavailable\r\n\r\n{\"status\":\"ok\"}"
        ));
        assert!(!is_healthy_response(
            "HTTP/1.1 200 OK\r\n\r\n{\"status\":\"ok\",\"service\":\"another-app\"}"
        ));
    }

    #[test]
    fn normalizes_windows_database_paths_for_h2() {
        let url = h2_database_url(Path::new(r"C:\Users\pilot\CareerPilot\data\careerpilot"));
        assert!(url.starts_with("jdbc:h2:file:C:/Users/pilot/CareerPilot/data/careerpilot;"));
        assert!(url.ends_with("DB_CLOSE_ON_EXIT=FALSE"));
    }

    #[test]
    fn removes_windows_verbatim_prefixes_for_java() {
        assert_eq!(
            process_compatible_path(PathBuf::from(r"\\?\C:\CareerPilot\backend.jar")),
            PathBuf::from(r"C:\CareerPilot\backend.jar")
        );
        assert_eq!(
            process_compatible_path(PathBuf::from(r"\\?\UNC\server\share\backend.jar")),
            PathBuf::from(r"\\server\share\backend.jar")
        );
    }
}
