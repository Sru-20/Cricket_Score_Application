# Boundary Cricket

A full-stack live cricket scoring demo with a responsive dark-mode React/TypeScript dashboard and a Spring Boot API. The backend stores match snapshots and every recorded delivery in PostgreSQL, simulates deliveries, exposes score and commentary REST endpoints, and broadcasts updates over STOMP/WebSocket.

## Requirements

- Node.js 20+
- Java 21+
- Maven 3.9+

## Run locally

Start PostgreSQL:

```powershell
docker compose up -d postgres
```

Start the API (Flyway creates the tables and the first run seeds demo fixtures):

```powershell
cd backend
mvn spring-boot:run
```

In another terminal, start the dashboard:

```powershell
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. The dashboard is dark by default and uses sample browser data until the API at `http://localhost:8080` responds. Set `VITE_API_URL` to override the REST base URL (for example, `https://scores.example.com/api`).

The local PostgreSQL defaults are database `boundary`, user `boundary`, and password `boundary`. Override the backend connection with `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD`; set `DATABASE_PASSWORD` in the environment before starting Compose to change the local container password too. These defaults are for local development only—use managed secrets and non-default credentials in deployed environments. `DATABASE_POOL_SIZE` controls the JDBC connection pool (default 20).

## API

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/matches` | List live and upcoming matches |
| `GET` | `/api/matches/{id}` | Read a match summary |
| `GET` | `/api/matches/{id}/scorecard` | Read current batting and bowling figures |
| `GET` | `/api/matches/{id}/commentary` | Read recent ball-by-ball commentary |
| `POST` | `/api/matches/{id}/balls?runs=4&wicket=false` | Record a delivery (demo scorer endpoint) |
| WebSocket/STOMP | `/ws`, subscribe `/topic/matches` | Receive match-list updates |
| WebSocket/STOMP | `/ws`, subscribe `/topic/matches/{id}` | Receive updates for one match |

The simulator advances live matches every eight seconds by default. Override the interval using `boundary.simulation.interval-ms`.

## Project structure

- `backend/` — Spring Boot REST API, PostgreSQL match snapshots and ball history, scheduled simulator, and STOMP broker.
- `frontend/` — Vite, React, TypeScript, responsive dashboard, and live-feed client.

Ball results are appended to `ball_results`, and the latest complete match view is saved as JSONB in `match_snapshots`. Flyway owns the schema. The scheduled simulator is a single-process demo; for multi-instance production deployment, move scheduling to one elected worker or a queue, use a shared message broker for WebSocket fan-out, and add authenticated/idempotent scorekeeper writes.
