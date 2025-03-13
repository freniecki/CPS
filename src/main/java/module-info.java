module cps.fx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.desktop;
    requires org.jfree.jfreechart;
    requires static lombok;

    opens cps.fx to javafx.fxml;
    exports cps.fx;
}