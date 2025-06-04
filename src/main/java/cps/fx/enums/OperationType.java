package cps.fx.enums;

public enum OperationType {
    SUM("Sum"),
    DIFFERENCE("Difference"),
    MULTIPLY("Multiply"),
    DIVIDE("Divide");

    private final String displayName;

    OperationType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
