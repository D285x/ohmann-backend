package com.anurag.ECE.physics;

/** Immutable stage description used by the simulator (decoupled from JPA). */
public record StageSpec(String name, double propellantKg, double dryKg, double thrustN, double ispS) {

    public double massFlow() {
        return thrustN / (ispS * Constants.G0);
    }

    public double burnTime() {
        return propellantKg / massFlow();
    }

    public double exhaustVelocity() {
        return ispS * Constants.G0;
    }
}
