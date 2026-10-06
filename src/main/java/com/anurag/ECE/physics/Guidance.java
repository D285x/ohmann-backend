package com.anurag.ECE.physics;

/**
 * Open-loop ascent guidance parameters.
 *
 * @param pitchKickDeg  tilt from vertical applied after the vertical rise; stage 1 then flies a gravity turn
 * @param finalPitchDeg thrust elevation above local horizontal at the end of the upper-stage burn
 *                      (upper stages fly a linear pitch program towards this value)
 */
public record Guidance(double pitchKickDeg, double finalPitchDeg) {

    public static final double VERTICAL_RISE_S = 10.0;
    public static final double KICK_DURATION_S = 10.0;
}
