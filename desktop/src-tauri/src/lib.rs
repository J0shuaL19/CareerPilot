mod backend;

use std::thread;
use tauri::{Manager, RunEvent};

fn focus_existing_window(app: &tauri::AppHandle) {
    if let Some(window) = app.get_webview_window("main") {
        if window.is_visible().unwrap_or(false) {
            let _ = window.unminimize();
            let _ = window.set_focus();
        }
    }
}

fn show_ready_window(app: &tauri::AppHandle, port: u16) -> Result<(), String> {
    let window = app
        .get_webview_window("main")
        .ok_or_else(|| "main window is unavailable".to_string())?;
    let url = tauri::Url::parse(&format!("http://127.0.0.1:{port}/"))
        .map_err(|error| format!("invalid backend URL: {error}"))?;

    window
        .navigate(url)
        .map_err(|error| format!("could not load the backend UI: {error}"))?;
    window
        .show()
        .map_err(|error| format!("could not show the main window: {error}"))?;
    window
        .set_focus()
        .map_err(|error| format!("could not focus the main window: {error}"))?;
    Ok(())
}

fn show_startup_error(app: &tauri::AppHandle, error: &str) {
    eprintln!("CareerPilot startup failed: {error}");

    if let Some(window) = app.get_webview_window("main") {
        let _ = window.set_title("CareerPilot - Startup Error");
        let _ = window.eval(
            r#"
            document.documentElement.innerHTML = `
              <head><meta charset="utf-8"><title>CareerPilot - Startup Error</title></head>
              <body style="margin:0;background:#f5f7fb;color:#1f2937;font-family:Segoe UI,sans-serif">
                <main style="max-width:680px;margin:12vh auto;padding:40px;background:white;border-radius:16px;box-shadow:0 12px 40px rgba(15,23,42,.12)">
                  <h1 style="margin-top:0">CareerPilot could not start</h1>
                  <p>The local service did not become ready. Close the app and try again.</p>
                  <p style="color:#64748b">Details are saved in <code>%LOCALAPPDATA%\\CareerPilot\\logs\\launcher.log</code>.</p>
                </main>
              </body>`;
            "#,
        );
        let _ = window.show();
        let _ = window.set_focus();
    }
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    let app = tauri::Builder::default()
        .plugin(tauri_plugin_single_instance::init(
            |app, _arguments, _working_directory| focus_existing_window(app),
        ))
        .on_window_event(|window, event| {
            if matches!(event, tauri::WindowEvent::CloseRequested { .. }) {
                let app_handle = window.app_handle();
                app_handle.state::<backend::BackendState>().stop();
                app_handle.exit(0);
            }
        })
        .setup(|app| {
            app.manage(backend::BackendState::default());

            let launch = match backend::launch(app) {
                Ok(launch) => launch,
                Err(error) => {
                    show_startup_error(app.handle(), &error);
                    return Ok(());
                }
            };

            let port = launch.port;
            app.state::<backend::BackendState>().store(launch.child)?;

            let app_handle = app.handle().clone();
            thread::spawn(move || {
                let state = app_handle.state::<backend::BackendState>();
                match backend::wait_until_ready(&state, port) {
                    Ok(()) => {
                        if let Err(error) = show_ready_window(&app_handle, port) {
                            state.stop();
                            show_startup_error(&app_handle, &error);
                        }
                    }
                    Err(error) => {
                        state.stop();
                        show_startup_error(&app_handle, &error);
                    }
                }
            });

            Ok(())
        })
        .build(tauri::generate_context!())
        .expect("failed to build CareerPilot desktop application");

    app.run(|app_handle, event| {
        if matches!(event, RunEvent::Exit) {
            app_handle.state::<backend::BackendState>().stop();
        }
    });
}
