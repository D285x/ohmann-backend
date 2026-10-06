# OhMann

**Launch trajectory optimization and interplanetary transfer planning**

Full Stack Java-1 (ESE3104) course project
Shaun, Roll No. 24EG104B27, B.Tech III Year I Semester, ECE, Anurag University

OhMann is a web application for planning space missions. Operators enter their own launch vehicles, launch sites and celestial bodies, then use two planners to see whether a mission is possible and when it can fly. Everything is entered and viewed in the browser; the results are stored in MySQL.

**Stack:** React (Vite, Axios, React Router, Recharts, Three.js) · Spring Boot 4.1.1 (Web MVC, Data JPA, Validation, DevTools) · Java 21 · Maven · MySQL 8.4 · optional Python aero-service (FastAPI, wrapping [shockFLOW](https://github.com/DKrasauskas/shockFLOW) GPU CFD)

---

## 1. What the application does

| Screen | What you can do |
|---|---|
| **Register / Log in** | Create an operator account and log in. Plans you save are recorded under your name. |
| **Vehicles** | Add, edit and delete launch vehicles and their stages (propellant, dry mass, thrust, Isp). The table shows liftoff mass and ideal delta-v. "Refine aero" optionally fetches a Mach-indexed drag curve from the aero-service (shockFLOW GPU CFD, or its built-in analytic fallback) in place of the constant drag coefficient - see section 7. |
| **Sites** | Add, edit and delete launch sites (latitude, longitude, allowed launch azimuth corridor). |
| **Bodies** | Add, edit and delete Sun-orbiting bodies (planets, dwarf planets, asteroids). Orbital period, surface gravity and current position are calculated for you. |
| **Launch Planner** | Choose a vehicle, site and target orbit (LEO, SSO or GTO), payload and optional orbital plane. The server simulates and optimizes the ascent and shows feasibility, maximum payload, delta-v budget, launch azimuth, next launch window and trajectory charts. |
| **Transfer Planner** | Choose an origin and destination body. The server lists upcoming Hohmann transfer windows with C3, departure and capture delta-v and flight time, and can estimate how much payload a chosen vehicle can send. |
| **History** | Browse saved launch and transfer plans, filter by orbit, open a plan to view its charts again, export its trajectory as CSV, or delete it. |
| **Overview** | Landing page with an interactive 3D globe of your launch sites, statistics and per-vehicle performance. |
| **Operators** | List registered operators and remove accounts. |

A typical session:
1. Register and log in.
2. Add a vehicle on **Vehicles** (or use one of the four pre-loaded ones) and a site on **Sites**.
3. Open **Launch Planner**, pick the vehicle, site and orbit, and click **Optimise trajectory**.
4. Add a destination such as Ceres on **Bodies**, then find windows to it on **Transfer Planner**.
5. Review everything later on **History** and the **Dashboard**.

On first start the database is filled with four vehicles (Falcon 9, PSLV-XL, LVM3, Electron), six launch sites and the eight planets, so the planners work immediately.

### Interface

The interface follows Apple's design language: San Francisco (SF Pro) type, generous spacing, rounded surfaces, pill buttons, segmented controls and a light theme with a dark mode (moon icon in the navigation bar, remembered per browser). SF Pro is used automatically on macOS and iOS, or on any computer where it is installed; other systems fall back to Inter, the closest web font.

### 3D visualization

The physics results are rendered in the browser with Three.js (WebGL):

- **Home globe:** a dotted-continent Earth that shows every launch site in the database, with animated arcs between them and a satellite in orbit. Drag to rotate.
- **3D flight replay** (Launch Planner results and saved plans): the simulated trajectory is wrapped onto the Earth along the launch azimuth, coloured by stage, with the target orbit drawn as a ring. Play, pause, scrub and change speed; a live panel shows time, altitude, velocity, downrange distance and flight phase. Altitude can be exaggerated so the thin ascent layer is visible.
- **Orbital simulation** (Transfer Planner results): every body moves on its orbit in calendar time while the spacecraft follows the Hohmann ellipse, positioned by solving Kepler's equation at each frame. Click any listed window to simulate it. A live panel shows date, mission day, distance from the Sun and heliocentric speed.

Continent outlines come from the public-domain Natural Earth dataset, pre-sampled into `frontend/src/assets/landDots.json`.

## 2. Architecture

```
React (localhost:5173)  --Axios / JSON-->  Spring Boot REST API (localhost:8080)  --JPA + JDBC-->  MySQL (localhost:3306)
```

```
ohmann/
├── backend/                         Spring Boot application (Maven: com.anurag / ECE)
│   ├── pom.xml
│   └── src/main/java/com/anurag/ECE/
│       ├── EceApplication.java
│       ├── config/       CORS, optimizer thread pool, starter data
│       ├── controller/   REST controllers: vehicles, sites, bodies, missions, transfers, users, stats
│       ├── dao/          Dashboard statistics with plain JDBC
│       ├── dto/          Request/response records with Bean Validation
│       ├── entity/       JPA entities: LaunchVehicle, Stage, LaunchSite, CelestialBody,
│       │                 MissionPlan, TransferPlan, AppUser
│       ├── exception/    Custom exceptions and the global error handler
│       ├── physics/      Ascent simulator, optimizer, orbital mechanics, transfer planner
│       ├── repository/   Spring Data JPA repositories
│       ├── service/      Business logic; service/orbit holds the LEO / SSO / GTO target classes
│       └── util/         CSV export, password hashing
├── frontend/                        React application
│   └── src/
│       ├── api/client.js            Axios calls
│       ├── auth/AuthContext.jsx     Logged-in operator
│       ├── components/              Navbar, footer, form field, charts, result panel, loaders
│       ├── hooks/                   Scroll-reveal animation
│       ├── viz/                     Three.js scenes: home globe, flight replay, orbital simulation
│       ├── pages/                   One file per screen
│       └── styles.css               Design system (SF Pro typography, light and dark themes)
├── database/schema.sql              MySQL schema (tables are also created automatically)
├── aero-service/                    Optional Python microservice wrapping shockFLOW GPU CFD (see section 7)
├── postman/                         Postman collection for the REST API
├── start-all.bat / stop-all.bat     Start or stop MySQL, backend and frontend on Windows
└── README.md
```

## 3. Software to install

| Tool | Version | Notes |
|---|---|---|
| JDK | 21 or newer | Set `JAVA_HOME`. |
| Apache Maven | 3.9.x | Add its `bin` folder to `Path`. |
| MySQL | 8.4 | Installed natively, or with Docker (see 4.1). |
| Node.js (includes npm) | 22 LTS or newer | |
| Docker Desktop | latest | Only if MySQL runs in Docker. |
| IntelliJ IDEA / VS Code | latest | Recommended editors. |
| Postman | latest | Optional, for testing the API directly. |
| Python | 3.11+ | Optional, only for the aero-service (section 7). |
| NVIDIA GPU + driver 580+ | | Optional, only to run shockFLOW itself; the aero-service works without one (analytic fallback). |

Spring Boot, the MySQL driver, React and the other libraries are downloaded automatically by Maven and npm.

## 4. Running the project

### 4.1 Database
With Docker (one-time setup):
```
docker run --name ohmann-mysql -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=ohmann -p 3306:3306 -v ohmann-data:/var/lib/mysql -d mysql:8.4
```
Afterwards, start it with `docker start ohmann-mysql`.

With a native MySQL install, put your root password in `backend/src/main/resources/application.properties` (`spring.datasource.password`). The default is `root`.

Tables are created automatically when the backend starts.

### 4.2 Everything at once (Windows)
Start Docker Desktop, then double-click `start-all.bat`. It starts MySQL, opens the backend and frontend in their own windows, and opens http://localhost:5173 when the API is ready. `stop-all.bat` stops them.

### 4.3 Manually
Backend:
```
cd backend
mvn spring-boot:run
```
Frontend (second terminal):
```
cd frontend
npm install
npm run dev
```
Open http://localhost:5173. The 3D views need a browser with WebGL (any current Chrome, Edge or Firefox).

Other backend commands:
```
mvn test                                         # physics unit tests
mvn clean package                                # builds target/ECE-0.0.1-SNAPSHOT.jar
mvn spring-boot:run -Dspring-boot.run.profiles=h2   # run without MySQL (in-memory, data not kept)
```

## 5. REST API

| Method | Endpoint | Purpose |
|---|---|---|
| GET, POST | `/api/vehicles` | List / create vehicles |
| GET, PUT, DELETE | `/api/vehicles/{id}` | Read / update / delete a vehicle |
| POST | `/api/vehicles/{id}/aero/refine` | Fetch a drag curve from the aero-service (§7); 503 if it isn't running |
| DELETE | `/api/vehicles/{id}/aero` | Discard a fetched drag curve, back to the constant Cd |
| GET, POST | `/api/sites` | List / create sites |
| GET, PUT, DELETE | `/api/sites/{id}` | Read / update / delete a site |
| GET, POST | `/api/bodies` | List / create celestial bodies |
| PUT, DELETE | `/api/bodies/{id}` | Update / delete a body |
| POST | `/api/missions/plan` | Optimize a launch (saved when `save` is true) |
| GET | `/api/missions` | Saved launch plans |
| GET, DELETE | `/api/missions/{id}` | Plan with its trajectory / delete |
| GET | `/api/missions/{id}/export` | Trajectory as CSV |
| POST | `/api/transfers/plan` | Find transfer windows (saved when `save` is true) |
| GET | `/api/transfers` | Saved transfer plans |
| DELETE | `/api/transfers/{id}` | Delete a transfer plan |
| POST | `/api/users/register` | Create an operator (201; 400 on invalid input; 409 if the email exists) |
| POST | `/api/users/login` | Check credentials (200 or 401) |
| GET | `/api/users` | List operators |
| DELETE | `/api/users/{id}` | Remove an operator (their plans are kept) |
| GET | `/api/stats` | Dashboard figures |

Invalid input returns HTTP 400 with a message per field, which the forms show under the matching input:
```json
{ "status": 400, "message": "Validation failed",
  "fieldErrors": { "semiMajorAxisAu": "Orbit radius must be at least 0.05 AU" } }
```

Import `postman/OhMann.postman_collection.json` into Postman to try every endpoint. IDs in the sample requests assume the pre-loaded data.

Login is a simple credential check for attributing plans; the API itself is not protected by tokens or sessions.

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

## 7. Aerodynamics refinement (optional, GPU)

By default every vehicle's drag is a single constant coefficient (§6.1), applied
at every speed. **Refine aero** on the Vehicles page instead fetches a
Mach-indexed drag curve - Cd at Mach 0.3, 0.5, ... up to 5 - from a separate
`aero-service`, and the ascent simulator interpolates it at every step instead
of using the constant. This captures the transonic drag rise real vehicles
show crossing Mach 1, which the constant model cannot.

`aero-service` is a small FastAPI process (`aero-service/`, not part of the
Maven build) that wraps [shockFLOW](https://github.com/DKrasauskas/shockFLOW),
a GPU-based transonic/supersonic CFD solver (Entropic Lattice Boltzmann
Method, stable to Mach 5, CUDA-accelerated). shockFLOW's Python interface is
MIT-licensed; its compiled CUDA/C++ core is proprietary and not bundled here,
and it needs an NVIDIA GPU with driver ≥ 580. Neither is required to try the
feature: without shockFLOW installed, `aero-service` answers every request
with a built-in analytic transonic drag correlation instead (labelled
`ANALYTIC_FALLBACK` in the UI, vs. `SHOCKFLOW` when the real CFD ran), so the
button, the storage, and the curve interpolation all work out of the box.

It runs as its own process, on its own port, called over plain HTTP by the
Spring Boot backend (`backend/.../aero/AeroClient.java`) - never from inside
the simulator's per-step physics loop, since a CFD sweep can take far longer
than one integration step. See `aero-service/README.md` for how to run it,
its two-endpoint API, and how to wire in a real shockFLOW install.

