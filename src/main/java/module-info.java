module cps.fx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.desktop;
    requires static lombok;
    requires java.logging;

    opens cps.fx to javafx.fxml;
    opens cps.fx.utils to javafx.fxml;
    opens cps.fx.enums to javafx.fxml;

    exports cps.fx;
    exports cps.fx.enums;
    exports cps.fx.utils;
}