# OhMann

**Launch trajectory optimization and interplanetary transfer planning**

Full Stack Java-1 (ESE3104) course project
Shaun, Roll No. 24EG104B27, B.Tech III Year I Semester, ECE, Anurag University

| | |
|---|---|
| **Live app** | https://ohmann-frontend.onrender.com |
| **REST API** | https://ohmann-backend.onrender.com/api (try [`/api/vehicles`](https://ohmann-backend.onrender.com/api/vehicles)) |
| **Demo login** | `demo@ohmann.app` / `ohmann-demo` (or register your own account) |
| **Frontend code** | [D285x/ohmann-frontend](https://github.com/D285x/ohmann-frontend) |
| **Backend code** | this repository |

> **Before you start:** the backend runs on Render's free tier, which sleeps after about 15 minutes without traffic. The first request after a pause takes 30 to 60 seconds while it wakes up; after that everything responds normally. The hosted demo uses an in-memory database that is reset on every restart, so anything you add disappears after a sleep. The sample vehicles, sites, planets and the demo login are recreated automatically each time.

OhMann is a web application for planning space missions. Operators enter their own launch vehicles, launch sites and celestial bodies, then use two planners to see whether a mission is possible and when it can fly.

**Stack:** React (Vite, Axios, React Router, Recharts, Three.js) · Spring Boot 4.1.1 (Web MVC, Data JPA, Validation) · Java 21 · Maven · MySQL 8.4 locally, H2 on the hosted demo · optional Python aero-service (FastAPI, wrapping [shockFLOW](https://github.com/DKrasauskas/shockFLOW) GPU CFD)

---

## 1. What the application does

| Screen | What you can do |
|---|---|
| **Register / Log in** | Create an operator account and log in. Plans you save are recorded under your name. |
| **Vehicles** | Add, edit and delete launch vehicles and their stages (propellant, dry mass, thrust, Isp). The table shows liftoff mass and ideal delta-v. **Refine aero** replaces the constant drag coefficient with a Mach-indexed drag curve (see section 7). |
| **Sites** | Add, edit and delete launch sites (latitude, longitude, allowed launch azimuth corridor). |
| **Bodies** | Add, edit and delete Sun-orbiting bodies (planets, dwarf planets, asteroids). Orbital period, surface gravity and current position are calculated for you. |
| **Launch Planner** | Choose a vehicle, site and target orbit (LEO, SSO or GTO), payload and optional orbital plane. The server simulates and optimizes the ascent and shows feasibility, maximum payload, delta-v budget, launch azimuth, next launch window and trajectory charts. |
| **Transfer Planner** | Choose an origin and destination body. The server lists upcoming Hohmann transfer windows with C3, departure and capture delta-v and flight time, and can estimate how much payload a chosen vehicle can send. |
| **History** | Browse saved launch and transfer plans, filter by orbit, open a plan to view its charts again, export its trajectory as CSV, or delete it. |
| **Overview** | Landing page with an interactive 3D globe of your launch sites, statistics and per-vehicle performance. |
| **Operators** | List registered operators and remove accounts. |

A typical session:
1. Log in with the demo account (or register).
2. Use one of the four pre-loaded vehicles on **Vehicles**, or add your own; likewise for **Sites**.
3. Open **Launch Planner**, pick the vehicle, site and orbit, and click **Optimise trajectory**. A plan takes about 5 to 15 seconds on the free hosting.
4. Add a destination such as Ceres on **Bodies**, then find windows to it on **Transfer Planner**.
5. Review everything later on **History** and the **Overview**.

On every start the database is filled with four vehicles (Falcon 9, PSLV-XL, LVM3, Electron), six launch sites, the eight planets and the demo operator, so the planners work immediately.

### Interface

The interface follows Apple's design language: San Francisco (SF Pro) type, generous spacing, rounded surfaces, pill buttons, segmented controls and a light theme with a dark mode (sun/moon icon in the navigation bar, remembered per browser). SF Pro is used automatically on macOS and iOS, or on any computer where it is installed; other systems fall back to Inter.

### 3D visualization

The physics results are rendered in the browser with Three.js (WebGL):

- **Home globe:** a dotted-continent Earth that shows every launch site in the database, with animated arcs between them and a satellite in orbit. Drag to rotate.
- **3D flight replay** (Launch Planner results and saved plans): the simulated trajectory is wrapped onto the Earth along the launch azimuth, coloured by stage, with the target orbit drawn as a ring. Play, pause, scrub and change speed; a live panel shows time, altitude, velocity, downrange distance and flight phase. Altitude can be exaggerated so the thin ascent layer is visible.
- **Orbital simulation** (Transfer Planner results): every body moves on its orbit in calendar time while the spacecraft follows the Hohmann ellipse, positioned by solving Kepler's equation at each frame. Click any listed window to simulate it. A live panel shows date, mission day, distance from the Sun and heliocentric speed.

Continent outlines come from the public-domain Natural Earth dataset, pre-sampled into `src/assets/landDots.json` in the frontend repository.

## 2. Architecture

The project is split into two repositories and deployed on Render:

```
Browser
  │
  ▼
ohmann-frontend  (React, Render Static Site)
  │  Axios / JSON over HTTPS
  ▼
ohmann-backend   (Spring Boot, Render Web Service, Docker)
  │  JPA + JDBC
  ▼
Database         (H2 in-memory on the demo; MySQL or PostgreSQL via environment variables)
  ┊  optional HTTP
  ▼
aero-service     (FastAPI, only needed for real shockFLOW GPU CFD)
```

Locally the same pieces run as React on `localhost:5173`, Spring Boot on `localhost:8080` and MySQL on `localhost:3306`.

This repository:

```
├── src/main/java/com/anurag/ECE/
│   ├── EceApplication.java
│   ├── aero/         Aero-service client and the built-in analytic drag model
│   ├── config/       CORS, optimizer thread pool, starter data and demo login
│   ├── controller/   REST controllers: vehicles, sites, bodies, missions, transfers, users, stats
│   ├── dao/          Dashboard statistics with plain JDBC
│   ├── dto/          Request/response records with Bean Validation
│   ├── entity/       JPA entities: LaunchVehicle, Stage, LaunchSite, CelestialBody,
│   │                 MissionPlan, TransferPlan, AppUser
│   ├── exception/    Custom exceptions and the global error handler
│   ├── physics/      Ascent simulator, optimizer, orbital mechanics, transfer planner
│   ├── repository/   Spring Data JPA repositories
│   ├── service/      Business logic; service/orbit holds the LEO / SSO / GTO target classes
│   └── util/         CSV export, password hashing
├── src/main/resources/   application.properties (env-driven) and the h2 profile
├── src/test/             Physics unit tests
├── aero-service/         Optional Python microservice wrapping shockFLOW GPU CFD (section 7)
├── database/schema.sql   MySQL schema (tables are also created automatically)
├── postman/              Postman collection for the REST API
├── Dockerfile            Container build used by Render
└── render.yaml           Render Blueprint
```

## 3. Running locally

| Tool | Version | Notes |
|---|---|---|
| JDK | 21 or newer | Set `JAVA_HOME`. |
| Apache Maven | 3.9.x | |
| MySQL | 8.4 | Optional; use the `h2` profile to skip it. |
| Node.js | 22 LTS or newer | For the frontend repository. |
| Python | 3.11+ | Optional, only for the aero-service. |

### Database (optional)

With Docker:
```
docker run --name ohmann-mysql -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=ohmann -p 3306:3306 -v ohmann-data:/var/lib/mysql -d mysql:8.4
```
The backend connects as `root` / `root` by default; override with `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`. Tables are created automatically when the backend starts.

### Backend

```
mvn spring-boot:run                                   # MySQL on localhost:3306
mvn spring-boot:run -Dspring-boot.run.profiles=h2     # no database needed, in-memory
mvn test                                              # physics unit tests
mvn clean package                                     # builds target/ECE-0.0.1-SNAPSHOT.jar
```

### Frontend

```
git clone https://github.com/D285x/ohmann-frontend
cd ohmann-frontend
npm install
npm run dev
```

Open http://localhost:5173. The 3D views need a browser with WebGL (any current Chrome, Edge or Firefox).

## 4. Deploying on Render

**Blueprint:** Render > New > Blueprint > this repository. It creates `ohmann-backend` (Docker) and `ohmann-aero` (Python).

**Manual:** Render > New > Web Service > this repository > Language **Docker** > Instance **Free**, with these environment variables:

| Variable | Value |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `h2` for the in-memory demo database; leave unset when using a real database |
| `DB_URL` | e.g. `jdbc:mysql://host:3306/ohmann?useSSL=true` or `jdbc:postgresql://host:5432/ohmann` |
| `DB_USERNAME` / `DB_PASSWORD` | database credentials |
| `CORS_ALLOWED_ORIGINS` | frontend URL(s), comma-separated, wildcards allowed; default `https://*.onrender.com,http://localhost:5173` |
| `AERO_SERVICE_URL` | optional, URL of the aero-service. Without it, **Refine aero** uses the same analytic model built into the backend |
| `OPTIMIZER_THREADS` | `2` suits the free instance |

`PORT` is injected by Render automatically. Render does not host MySQL; for data that survives restarts, use Render PostgreSQL (in `jdbc:postgresql://...` form) or an external MySQL provider.

The frontend is a Render **Static Site** built from [ohmann-frontend](https://github.com/D285x/ohmann-frontend); see its README.

## 5. REST API

| Method | Endpoint | Purpose |
|---|---|---|
| GET, POST | `/api/vehicles` | List / create vehicles |
| GET, PUT, DELETE | `/api/vehicles/{id}` | Read / update / delete a vehicle |
| POST | `/api/vehicles/{id}/aero/refine` | Compute and store a Mach-indexed drag curve (section 7) |
| DELETE | `/api/vehicles/{id}/aero` | Discard the drag curve, back to the constant Cd |
| GET, POST | `/api/sites` | List / create sites |
| GET, PUT, DELETE | `/api/sites/{id}` | Read / update / delete a site |
| GET, POST | `/api/bodies` | List / create celestial bodies |
| PUT, DELETE | `/api/bodies/{id}` | Update / delete a body |
| POST | `/api/missions/plan` | Optimize a launch (saved when `save` is true) |
| GET | `/api/missions` | Saved launch plans |
| GET, DELETE | `/api/missions/{id}` | Plan with its trajectory / delete (own plans only, needs a session token) |
| GET | `/api/missions/{id}/export` | Trajectory as CSV |
| POST | `/api/transfers/plan` | Find transfer windows (saved when `save` is true) |
| GET | `/api/transfers` | Saved transfer plans |
| DELETE | `/api/transfers/{id}` | Delete a transfer plan (own plans only, needs a session token) |
| POST | `/api/users/register` | Create an operator and log in (201 with a session token; 400 on invalid input; 409 if the email exists) |
| POST | `/api/users/login` | Log in (200 with a session token, or 401) |
| GET | `/api/users/me` | The operator behind the session token (401 if missing or expired) |
| GET | `/api/users` | List operators |
| DELETE | `/api/users/{id}` | Delete your own account (needs a session token; their plans are kept) |
| GET | `/api/stats` | Dashboard figures |

Invalid input returns HTTP 400 with a message per field, which the forms show under the matching input:
```json
{ "status": 400, "message": "Validation failed",
  "fieldErrors": { "semiMajorAxisAu": "Orbit radius must be at least 0.05 AU" } }
```

Import `postman/OhMann.postman_collection.json` into Postman to try every endpoint. It targets the live backend by default (switch `baseUrl` to `localUrl` for a local one); run the folders in order, since the mission-1 requests need a saved plan.

### Who can delete what

Logging in returns a random session token, which the frontend sends as `Authorization: Bearer <token>`. The server uses it to enforce two rules:

- a launch or transfer plan can be deleted only by the operator who saved it (plans whose owner deleted their account can be deleted by any logged-in operator);
- an operator can delete only their own account.

Anything else is rejected with 401 (not logged in or session expired) or 403 (someone else's plan or account). Sessions are kept in memory, so restarting the server logs everyone out. Browsing, planning, and editing vehicles, sites and bodies stay open to everyone. Passwords are stored as salted PBKDF2 hashes.

## 6. How the calculations work

### 6.1 Ascent simulation (`AscentSimulator`)
- The vehicle is modeled as a point mass flying in one plane over a spherical Earth. The state is radius, downrange angle, radial velocity, transverse velocity and mass.
- The equations of motion are integrated with fourth-order Runge-Kutta using a 0.5 s step. The step is refined at staging and at engine cutoff.
- The atmosphere is exponential (density 1.225 kg/m³, scale height 8.5 km) and rotates with the Earth, so drag acts on the velocity relative to the air.
- Stages fire in sequence, and each spent stage drops its dry mass. Strap-on boosters are merged into stage 1.
- Guidance:
  - vertical rise for 10 s,
  - a pitch kick for 10 s,
  - a gravity turn (thrust along the air-relative velocity) for the rest of stage 1,
  - a linear pitch program on the upper stages, ending at a chosen final pitch angle.
- Cutoff happens in the last stage, once apoapsis reaches the target altitude and the vehicle is above 80 km. The insertion delta-v then covers the circularization burn, or a two-burn correction if apoapsis overshot the target.
- Gravity, drag and steering losses are integrated along the way.

### 6.2 Optimization (`AscentOptimizer`)
- There are two decision variables: the pitch kick angle and the final upper-stage pitch.
- The objective is to maximize the delta-v left over after insertion and any post-insertion manoeuvres. Infeasible trajectories are ranked by their apoapsis shortfall, so the search still moves toward feasible ones.
- The search starts with a coarse 14 × 13 grid, evaluated in parallel with `ExecutorService.invokeAll`, followed by four levels of local grid refinement.
- **Payload capacity** is found by bisection on payload mass. The pitch program is re-optimized at every step.

### 6.3 Orbit geometry (`OrbitalMechanics`, `LaunchGeometry`)
- **SSO inclination** comes from the J2 nodal precession condition.
- **Launch azimuth:** first the inertial azimuth, sin β = cos i / cos φ. It is then corrected for Earth rotation and checked against the site's range-safety corridor. If the northbound branch is not allowed, the southbound (descending) branch is tried.
- **Unreachable inclination:** if the target inclination is below the site latitude, the vehicle launches due east and the plane change cost 2v·sin(Δi/2) is added.
- **GTO:** the Hohmann perigee burn from the parking orbit is included in the requirement. The satellite's apogee burn to GEO, including the plane change, is reported for information only.
- **Launch window:** the local sidereal time must equal RAAN plus the site's longitude offset from the ascending node. GMST is computed from the Julian date.

### 6.4 Interplanetary transfers (`InterplanetaryPlanner`)
- Body orbits are treated as circular and coplanar, positioned by their J2000 mean longitude. The orbital period comes from Kepler's third law, T = 2π√(a³/μ☉), so any body a user adds only needs its orbit radius.
- Required phase angle: θ = 180° − n₂·TOF. The next window is found by solving the linear phase-angle equation, and later windows repeat every synodic period.
- Departure and capture burns use patched conics: Δv = √(v∞² + 2μ/r) − √(μ/r).
- As a sanity check, Earth to Mars gives a time of flight of 259 days, v∞ of 2.94 km/s, C3 of 8.7 km²/s², a synodic period of 780 days and a next window in November 2026.

### 6.5 Limitations
- The stage data for the seeded vehicles are rounded public estimates, so payload figures are indicative rather than official.
- Parallel staging (PSLV strap-ons, LVM3 S200 boosters) is approximated as sequential, which overestimates payload for those vehicles.
- The planar ascent model ignores yaw steering, dogleg manoeuvres during ascent, winds and engine throttling.
- The transfer model ignores eccentricity and inclination of the planetary orbits. Real launch windows can differ by days to weeks, and real C3 values are somewhat higher.

## 7. Aerodynamics refinement

By default every vehicle's drag is a single constant coefficient (section 6.1), applied at every speed. **Refine aero** on the Vehicles page replaces it with a Mach-indexed drag curve (Cd from Mach 0.3 to 5), which the ascent simulator interpolates at every step. This captures the transonic drag rise real vehicles show crossing Mach 1, which the constant model cannot.

There are two sources for that curve:

- **Built-in analytic model** (`aero/AnalyticDragModel.java`): a classic engineering correlation with a constant subsonic coefficient, a drag-divergence bump centred on Mach 1, and supersonic wave drag decaying like 1/√(M² − 1), scaled by nose fineness. It is used whenever the aero-service is not configured or cannot be reached, which is how the hosted demo runs. The UI labels these curves **Analytic fallback**.
- **shockFLOW GPU CFD** through the optional `aero-service/` (FastAPI). [shockFLOW](https://github.com/DKrasauskas/shockFLOW) is a GPU-based transonic/supersonic CFD solver (Entropic Lattice Boltzmann Method, stable to Mach 5). Its compiled CUDA/C++ core is proprietary and not bundled here, and it needs an NVIDIA GPU with driver ≥ 580. When it runs, curves are labelled **shockFLOW (GPU CFD)**.

The curve is fetched once per **Refine aero** click and stored on the vehicle, never computed inside the simulator's per-step loop. See `aero-service/README.md` for running the service and wiring in a real shockFLOW install.
