# OhMann Backend

Spring Boot REST API for OhMann, a launch trajectory optimization and interplanetary transfer planning app (ESE3104 Full Stack Java-1). The React client lives in the separate **ohmann-frontend** repository. Full project documentation: [docs/PROJECT.md](docs/PROJECT.md).

**Stack:** Spring Boot 4.1.1 (Web MVC, Data JPA, Validation) · Java 21 · Maven · MySQL / PostgreSQL / H2 · optional Python aero-service (FastAPI)

## Layout

```
src/            Spring Boot application (com.anurag.ECE)
aero-service/   Optional FastAPI drag-curve service (shockFLOW wrapper with analytic fallback)
database/       MySQL schema (tables are also created automatically by JPA)
postman/        Postman collection for the REST API
Dockerfile      Container build used by Render
render.yaml     Render Blueprint (backend + aero-service)
```

## Run locally

```bash
mvn spring-boot:run                                   # MySQL on localhost:3306 (root/root)
mvn spring-boot:run -Dspring-boot.run.profiles=h2     # no database needed, in-memory
```

API base: `http://localhost:8080/api`

## Deploy on Render

**Option A, Blueprint:** Render > New > Blueprint > pick this repo. It creates `ohmann-backend` (Docker) and `ohmann-aero` (Python).

**Option B, manual:** Render > New > Web Service > this repo > Runtime **Docker** > Instance **Free**, then add the environment variables below.

| Variable | Value |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `h2` for the in-memory demo database; leave unset when using a real DB |
| `DB_URL` | e.g. `jdbc:mysql://host:3306/ohmann?useSSL=true` or `jdbc:postgresql://host:5432/ohmann` |
| `DB_USERNAME` / `DB_PASSWORD` | database credentials |
| `CORS_ALLOWED_ORIGINS` | frontend Static Site URL, e.g. `https://ohmann-frontend.onrender.com`; default `https://*.onrender.com,http://localhost:5173` (comma-separated, wildcards allowed) |
| `AERO_SERVICE_URL` | optional, public URL of the aero-service, e.g. `https://ohmann-aero.onrender.com`. Without it, "Refine aero" uses the same analytic drag model built into the backend |
| `OPTIMIZER_THREADS` | `2` suits the free instance |

`PORT` is injected by Render automatically.

### Database notes

- Render does not host MySQL. Use Render PostgreSQL (copy the host, database, user and password into `DB_URL` in `jdbc:postgresql://...` form) or an external MySQL provider such as Aiven or Railway.
- With `SPRING_PROFILES_ACTIVE=h2` the app runs with seeded sample data but loses anything added on each restart or sleep.

### After deploying

Copy the backend URL into the frontend Static Site's variable `VITE_API_URL` as `https://<service>.onrender.com/api` and redeploy the frontend.
