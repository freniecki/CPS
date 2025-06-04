package cps.fx.utils;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.util.function.UnaryOperator;

public class TextFieldFactory {

    private TextFieldFactory() {}

    public static TextField createPositiveIntegerField() {
        TextField textField = new TextField();
        textField.setPromptText("0");
        textField.setMinWidth(60);

        UnaryOperator<TextFormatter.Change> integerFilter = change -> {
            String newText = change.getControlNewText();
            // Regex: zero lub więcej cyfr (pusta wartość jest dozwolona, aby móc edytować)
            if (newText.matches("\\d*")) {
                return change;
            }
            return null;
        };

        textField.setTextFormatter(new TextFormatter<>(integerFilter));
        return textField;
    }

    public static TextField createPositiveDoubleField() {
        TextField textField = new TextField();
        textField.setPromptText("0.00");
        textField.setMinWidth(60);

        UnaryOperator<TextFormatter.Change> doubleFilter = change -> {
            String newText = change.getControlNewText();
            // Regex: liczba zmiennoprzecinkowa, pusta wartość jest OK, kropka może wystąpić maks raz
            if (newText.matches("\\d*(\\.\\d*)?")) {
                return change;
            }
            return null;
        };

        textField.setTextFormatter(new TextFormatter<>(doubleFilter));
        return textField;
    }
}
