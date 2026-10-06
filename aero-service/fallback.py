"""
Analytic fallback drag model.

Used whenever shockFLOW (the GPU CFD solver) is not installed, or no
compatible GPU is present. It is a classic engineering correlation, not a CFD
result: a constant subsonic coefficient, a transonic "drag divergence" bump
centred on Mach 1 (the well-known rise every real vehicle shows crossing the
sound barrier), and a supersonic wave-drag term that falls off roughly like
the Prandtl-Glauert factor 1/sqrt(M^2 - 1) and scales with how slender the
nose is (a pointier nose - higher fineness ratio - makes a smaller bump).

This exists so the aero-service, and the "refine aerodynamics" feature on top
of it, work out of the box on any machine, with or without a GPU. It is
deliberately simple and should not be read as a substitute for shockFLOW's
actual CFD solve; see README.md for how the two are wired together.
"""
from __future__ import annotations

import math


def base_cd(fineness_ratio: float) -> float:
    """Rough subsonic Cd for a slender body of the given fineness ratio."""
    return max(0.15, 0.30 - 0.02 * fineness_ratio)


def drag_curve(fineness_ratio: float, mach_min: float, mach_max: float, points: int) -> tuple[list[float], list[float]]:
    cd0 = base_cd(fineness_ratio)
    bump = 0.55 / max(1.0, fineness_ratio / 2.0)

    machs = [mach_min + (mach_max - mach_min) * i / (points - 1) for i in range(points)]
    cds = [_cd_at(m, cd0, bump) for m in machs]
    return machs, cds


def _cd_at(m: float, cd0: float, bump: float) -> float:
    if m < 0.8:
        return cd0
    if m <= 1.2:
        # Smooth rise-and-partial-fall bump peaking at Mach 1 (drag divergence).
        return cd0 + bump * math.sin(math.pi * (m - 0.8) / 0.8)
    # Supersonic: wave drag decays roughly like the Prandtl-Glauert factor.
    decay = 1.0 / math.sqrt(max(m * m - 1.0, 0.05))
    peak = cd0 + bump  # value the curve reached at m = 1.2's neighbourhood
    return cd0 + (peak - cd0) * decay / math.sqrt(1.2 * 1.2 - 1.0 + 0.05)
