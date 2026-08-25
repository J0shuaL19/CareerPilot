# Production deployment

CareerPilot currently has no authentication or per-user data isolation. Deploy it only as a private, single-user service. The default Compose configuration binds the web port to `127.0.0.1`; keep that default and place an authenticated VPN or trusted HTTPS reverse proxy in front of it. Do not expose this release directly to the public internet.

## Requirements

- Docker Engine with Docker Compose v2
- A host with persistent disk space for PostgreSQL
- A private DNS name or local host access
- Optional OpenAI API credentials for AI match analysis

## Configure

From the repository root, create the local environment file:

```bash
cp .env.example .env
```

At minimum, replace `DB_PASSWORD` with a long random value. Set `APP_TIME_ZONE` to an IANA time-zone name such as `America/Los_Angeles` so reminder dates follow the intended local day. Keep `CAREERPILOT_BIND_ADDRESS=127.0.0.1` unless the host is on a trusted private network and direct LAN access is intentional.

`.env` is ignored by Git. Never commit real database passwords or API keys.

## Build and start

```bash
docker compose config
docker compose build --pull
docker compose up -d
docker compose ps
```

The default local URL is `http://127.0.0.1:8080`. The browser talks to Nginx, which serves the React application and proxies `/api` to Spring Boot on the private Compose network. PostgreSQL and the backend are not published on host ports.

Check health and logs:

```bash
curl --fail http://127.0.0.1:8080/healthz
curl --fail http://127.0.0.1:8080/api/health
docker compose logs --tail=200 backend frontend database
```

The API health endpoint verifies database connectivity. It does not call the OpenAI API.

## HTTPS and access control

Terminate HTTPS at a managed load balancer, a trusted reverse proxy, or a private access gateway. Forward traffic to `127.0.0.1:8080` and preserve the standard `X-Forwarded-*` headers. Add authentication at that outer layer until CareerPilot implements its own login and user isolation.

If direct LAN access is required, set `CAREERPILOT_BIND_ADDRESS=0.0.0.0` and restrict the port with the host firewall to explicitly trusted source addresses. This is still a shared single-user data store, not a multi-user deployment.

## Back up PostgreSQL

Create a backup directory outside the container and write a compressed PostgreSQL archive:

```bash
mkdir -p backups
docker compose exec -T database sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' > "backups/careerpilot-$(date +%Y%m%d-%H%M%S).dump"
```

Copy backups to storage that is separate from the deployment host and test restoration periodically. The `backups/` directory is ignored by Git.

## Restore PostgreSQL

Restoration replaces the current database. Take a fresh backup first and verify the selected archive path.

```bash
docker compose stop frontend backend
docker compose exec -T database sh -c 'dropdb -U "$POSTGRES_USER" --if-exists "$POSTGRES_DB" && createdb -U "$POSTGRES_USER" "$POSTGRES_DB"'
docker compose exec -T database sh -c 'pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --no-owner --exit-on-error' < backups/selected-backup.dump
docker compose start backend frontend
curl --fail http://127.0.0.1:8080/api/health
```

## Upgrade

Before every upgrade, create a database backup and record the Git revision currently deployed. Then:

```bash
git pull --ff-only
docker compose build --pull
docker compose up -d
docker compose ps
curl --fail http://127.0.0.1:8080/api/health
```

Flyway applies pending database migrations when the backend starts. Never edit a migration that has already run. Roll back application code only after checking whether the newer release applied a database migration that the older code cannot read.

## Stop or remove

```bash
docker compose stop
```

`docker compose down` removes containers and the private network but preserves the named PostgreSQL volume. Do not run `docker compose down -v` unless permanent database deletion is explicitly intended and a verified backup exists.

## Release checklist

- CI backend tests, frontend lint, and frontend build are green.
- `.env` contains no placeholder database password.
- The service remains private or is protected by an authenticated access layer.
- HTTPS is enabled for any traffic leaving the deployment host.
- `/healthz` and `/api/health` return success.
- A fresh off-host database backup exists and its restore procedure has been tested.
- Container logs contain no startup errors or exposed secrets.
