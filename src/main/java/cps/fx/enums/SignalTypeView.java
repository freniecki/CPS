package cps.fx.enums;

public enum SignalTypeView {
    UNIFORM_NOISE("Uniform Noise"),
    GAUSS_NOISE("Gauss Noise"),
    SINE("Sine"),
    HALF_SINE("Half Sine"),
    FULL_SINE("Full Sine"),
    RECTANGULAR("Rectangular"),
    RECTANGULAR_SYMMETRIC("Rectangular Symmetric"),
    TRIANGULAR("Triangular"),
    UNIT_STEP("Unit Step"),
    UNIT_IMPULSE("Unit Impulse"),
    IMPULSE_NOISE("Impulse Noise");

    private final String displayName;

    SignalTypeView(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}