"""
Best-effort wrapper around shockFLOW (https://github.com/DKrasauskas/shockFLOW),
a GPU-based transonic 1D/2D CFD solver (Entropic Lattice Boltzmann Method).

shockFLOW's compiled CUDA/C++ core is proprietary and requires an NVIDIA GPU
(driver >= 580); its Python interface is MIT-licensed. Neither is a hard
dependency of this service: if the import fails, or no compatible GPU is
found, `available()` returns False and main.py uses the analytic fallback in
fallback.py instead.

`run_sweep` is intentionally the one function you may need to adapt: the
exact simulation call (mesh setup, boundary conditions, how a drag
coefficient is read back out) depends on the shockFLOW version you have
installed, and its README is the source of truth for that API, not this
comment. As shipped, this wraps the two calls the shockFLOW project's own
README documents (`import shockFLOW.solver as solver`,
`solver.getDeviceProperties()`) to detect the GPU, and raises
NotImplementedError for the actual sweep until you fill in the solver calls
for the shockFLOW version you install - main.py catches that and reports the
analytic fallback instead, so the service keeps working either way.
"""
from __future__ import annotations


def available() -> tuple[bool, str]:
    """Returns (True, device_name) if shockFLOW is importable and a GPU is found."""
    try:
        import shockFLOW.solver as solver  # type: ignore
    except Exception as exc:  # ImportError, or anything the package raises on import
        return False, f"shockFLOW not importable: {exc}"

    try:
        props = solver.getDeviceProperties()
        return True, str(props)
    except Exception as exc:
        return False, f"shockFLOW imported but no usable GPU: {exc}"


def run_sweep(diameter_m: float, nose_fineness_ratio: float,
              mach_min: float, mach_max: float, points: int) -> tuple[list[float], list[float]]:
    """
    Runs one transonic/supersonic sweep over `points` Mach numbers between
    mach_min and mach_max for an axisymmetric nose-cone of the given diameter
    and fineness ratio, and returns (mach_values, drag_coefficients).

    Left unimplemented on purpose (see module docstring): fill in the mesh
    setup and per-Mach solve for your installed shockFLOW version here. Until
    then this raises NotImplementedError, which main.py treats exactly like
    "shockFLOW unavailable" and answers with the analytic fallback instead.
    """
    raise NotImplementedError(
        "run_sweep() is a template - wire it up to your shockFLOW version's actual "
        "solver API (see shockflow_backend.py's module docstring), then this branch "
        "will be used instead of the analytic fallback."
    )
