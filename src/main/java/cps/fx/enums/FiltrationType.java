package cps.fx.enums;

public enum FiltrationType {
    LOW_PASS("Low Pass"),
    HIGH_PASS("High Pass");

    private final String displayName;

    FiltrationType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
