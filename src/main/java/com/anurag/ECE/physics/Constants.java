package com.anurag.ECE.physics;

/** Physical constants (SI units). */
public final class Constants {

    private Constants() {
    }

    public static final double MU_EARTH = 3.986004418e14;        // m^3/s^2
    public static final double R_EARTH = 6_378_137.0;            // m, equatorial
    public static final double J2_EARTH = 1.08262668e-3;
    public static final double OMEGA_EARTH = 7.2921159e-5;       // rad/s
    public static final double G0 = 9.80665;                     // m/s^2
    public static final double MU_SUN = 1.32712440018e20;        // m^3/s^2
    public static final double AU = 1.495978707e11;              // m
    public static final double GEO_ALTITUDE = 35_786_000.0;      // m
    /** Mean motion of the Earth around the Sun, used for sun-synchronous precession. */
    public static final double SSO_PRECESSION_RATE = 2 * Math.PI / (365.2422 * 86400.0);
    public static final double SEA_LEVEL_DENSITY = 1.225;        // kg/m^3
    public static final double SCALE_HEIGHT = 8_500.0;           // m
    public static final double J2000_JD = 2_451_545.0;
    public static final double SIDEREAL_DEG_PER_DAY = 360.98564736629;
}
