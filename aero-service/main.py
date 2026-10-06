"""
OhMann aero-service: a small HTTP wrapper around shockFLOW
(https://github.com/DKrasauskas/shockFLOW), a GPU-based transonic CFD solver,
that the Spring Boot backend calls to refine a launch vehicle's drag
coefficient into a Mach-indexed curve.

Run it with:
    pip install -r requirements.txt
    uvicorn main:app --port 8500

It is entirely optional. The Spring Boot backend's "refine aerodynamics"
button simply gets a 503 (with a clear message) if this is not running, and
every vehicle keeps working off its constant drag coefficient - nothing else
in the app depends on this service.
"""
from __future__ import annotations

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

import fallback
import shockflow_backend

app = FastAPI(title="OhMann aero-service", description="shockFLOW wrapper for Mach-indexed drag curves")


class DragCurveRequest(BaseModel):
    diameterM: float = Field(gt=0, le=20)
    noseFinenessRatio: float = Field(default=3.0, ge=1.0, le=20.0)
    machMin: float = Field(default=0.3, ge=0.05, le=8.0)
    machMax: float = Field(default=5.0, ge=0.05, le=8.0)
    points: int = Field(default=24, ge=4, le=60)


class DragCurveResponse(BaseModel):
    mach: list[float]
    cd: list[float]
    source: str
    note: str


@app.get("/health")
def health():
    ok, detail = shockflow_backend.available()
    return {"status": "ok", "shockflowAvailable": ok, "detail": detail}


@app.post("/drag-curve", response_model=DragCurveResponse)
def drag_curve(req: DragCurveRequest):
    if req.machMax <= req.machMin:
        raise HTTPException(status_code=400, detail="machMax must be greater than machMin")

    ok, detail = shockflow_backend.available()
    if ok:
        try:
            mach, cd = shockflow_backend.run_sweep(
                req.diameterM, req.noseFinenessRatio, req.machMin, req.machMax, req.points)
            return DragCurveResponse(mach=mach, cd=cd, source="shockflow",
                                      note=f"shockFLOW GPU sweep ({detail})")
        except NotImplementedError as exc:
            # shockFLOW is installed and a GPU was found, but run_sweep() hasn't been
            # wired up to that version's solver API yet - fall through to the
            # analytic model rather than fail the request outright.
            detail = str(exc)
        except Exception as exc:
            detail = f"shockFLOW sweep failed: {exc}"

    mach, cd = fallback.drag_curve(req.noseFinenessRatio, req.machMin, req.machMax, req.points)
    return DragCurveResponse(mach=mach, cd=cd, source="analytic_fallback",
                              note=f"shockFLOW unavailable ({detail}); used the built-in empirical model instead")
