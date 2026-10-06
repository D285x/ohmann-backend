package com.anurag.ECE.entity;

/** Target orbit families supported by the launch planner. */
public enum OrbitType {
    /** Circular low Earth orbit at a user-chosen altitude and inclination. */
    LEO,
    /** Sun-synchronous orbit: inclination is derived from altitude (J2 precession). */
    SSO,
    /** Geostationary transfer orbit: parking orbit + perigee burn to 35 786 km apogee. */
    GTO
}
