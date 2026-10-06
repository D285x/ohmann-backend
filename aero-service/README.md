# OhMann aero-service

A small FastAPI service, separate from the Spring Boot backend, that wraps
[shockFLOW](https://github.com/DKrasauskas/shockFLOW) - a GPU-based transonic
1D/2D CFD solver (Entropic Lattice Boltzmann Method), stable up to Mach 5 -
to refine a launch vehicle's drag coefficient into a Mach-indexed curve.

## Why a separate service instead of a Java library

shockFLOW's interface is Python, and its CUDA/C++ core is compiled and
proprietary (not something to bundle into this repository). Wrapping it in a
tiny HTTP service, called from Spring Boot's `AeroClient`
(`src/main/java/com/anurag/ECE/aero/AeroClient.java`), keeps that
dependency isolated and optional: nothing else in OhMann needs Python or a
GPU to run.

It is also called out-of-band from the physics engine on purpose. A CFD sweep
can take anywhere from seconds to minutes; the ascent simulator integrates
thousands of RK4 steps per second and the optimizer runs many simulations per
second on top of that, so it reads a vehicle's *already-fetched* drag curve
(cached as JSON on the `launch_vehicles` row) rather than calling out to CFD
mid-simulation. "Refine aerodynamics" is a one-off action you trigger from
the Vehicles page; the result is stored and reused by every later plan.

## Do you need it?

No. The backend contains a Java port of the same analytic model
(`src/main/java/com/anurag/ECE/aero/AnalyticDragModel.java`, numerically
identical to `fallback.py`) and uses it whenever this service is not
configured or cannot be reached. The hosted demo runs that way. Run this
service only if you want to plug in real shockFLOW GPU results.

## Running it

```
cd aero-service
pip install -r requirements.txt
uvicorn main:app --port 8500
```

That's enough to try the feature end-to-end: without shockFLOW installed,
every `/drag-curve` request is answered by the analytic fallback in
`fallback.py` (a standard transonic drag-divergence correlation, not a CFD
result - see its docstring), and the vehicle's `aeroSource` is recorded as
`ANALYTIC_FALLBACK`.

To get real GPU-accelerated results:

1. Have an NVIDIA GPU with driver >= 580.
2. Install shockFLOW per its own README: https://github.com/DKrasauskas/shockFLOW
3. Fill in `run_sweep()` in `shockflow_backend.py` with that version's actual
   solver calls (mesh setup, boundary conditions, how to read the drag
   coefficient back out) - the shockFLOW repository's README is the source of
   truth for that API, not this project. Until you do, the service still
   works; it just keeps using the analytic fallback and reports why in the
   response's `note` field.
4. Restart `uvicorn`. `GET /health` reports `shockflowAvailable: true` once
   the import and device check both succeed.

## API

`GET /health` - `{ "status": "ok", "shockflowAvailable": bool, "detail": str }`

`POST /drag-curve` - body:
```json
{ "diameterM": 3.7, "noseFinenessRatio": 3.0, "machMin": 0.3, "machMax": 5.0, "points": 24 }
```
response:
```json
{ "mach": [0.3, ...], "cd": [0.24, ...], "source": "shockflow" | "analytic_fallback", "note": "..." }
```

## Backend wiring

`application.properties` (or the `AERO_SERVICE_URL` environment variable, e.g. on Render):
```
ohmann.aero.service-url=${AERO_SERVICE_URL:http://localhost:8500}
ohmann.aero.timeout-seconds=180
```

To host it on Render: New > Web Service > this repository, Language Python 3,
Root Directory `aero-service`, Build Command `pip install -r requirements.txt`,
Start Command `uvicorn main:app --host 0.0.0.0 --port $PORT`. Then set
`AERO_SERVICE_URL` on the backend service to its URL.

`POST /api/vehicles/{id}/aero/refine` (optional body: `noseFinenessRatio`,
`machMin`, `machMax`, `points`) fetches a curve and stores it on the vehicle;
`DELETE /api/vehicles/{id}/aero` discards it, reverting to the vehicle's
constant drag coefficient. If this service is not running, `refine` falls back
to the backend's built-in copy of the analytic model, so the button always
works; every other feature of OhMann is unaffected.
