package cps.fx;

import cps.model.*;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.util.*;
import java.util.List;
import java.util.logging.Logger;

public class HelloController {
    private static final Logger logger = Logger.getLogger(HelloController.class.getName());

    private final List<HBox> rows = new ArrayList<>();

    @FXML private VBox configurationVBox;
    @FXML private Pane chartPane;
    @FXML private Button generateButton;

    @FXML private MenuButton operationMenu;
    @FXML private MenuItem noneMenuItem;
    @FXML private MenuItem sumMenuItem;
    @FXML private MenuItem differenceMenuItem;
    @FXML private MenuItem multiplyMenuItem;
    @FXML private MenuItem divideMenuItem;
    
    @FXML private MenuButton signalMenu;
    @FXML private MenuItem uniformNoiseMenuItem;
    @FXML private MenuItem gaussNoiseMenuItem;
    @FXML private MenuItem sineMenuItem;
    @FXML private MenuItem sineHalfMenuItem;
    @FXML private MenuItem sineFullMenuItem;
    @FXML private MenuItem rectangularMenuItem;
    @FXML private MenuItem rectangularSymetricMenuItem;
    @FXML private MenuItem triangularMenuItem;
    @FXML private MenuItem unitStepMenuItem;
    @FXML private MenuItem unitImpulseMenuItem;
    @FXML private MenuItem impulseNoiseMenuItem;

    @FXML
    private void initialize() {
        List<MenuItem> operationItemList;
        List<MenuItem> signalItemList;
        signalItemList = List.of(uniformNoiseMenuItem, gaussNoiseMenuItem, sineMenuItem, sineHalfMenuItem, sineFullMenuItem,
                rectangularMenuItem, rectangularSymetricMenuItem, triangularMenuItem, unitStepMenuItem,
                unitImpulseMenuItem, impulseNoiseMenuItem);
        signalItemList.forEach(item -> item.setOnAction(e -> addRow(signalItemList.indexOf(item))));

        operationItemList = List.of(noneMenuItem, sumMenuItem, differenceMenuItem, multiplyMenuItem, divideMenuItem);
        operationItemList.forEach(item -> item.setOnAction(e -> operationMenu.setText(item.getText())));

        generateButton.setOnAction(e -> generateChart());
    }

    private void addRow(int signalId) {
        SignalType signalType = SignalType.values()[signalId];

        HBox row = new HBox(10);
        CheckBox checkBox = new CheckBox("");
        row.getChildren().add(checkBox);
        row.getChildren().add(new Label(signalType.toString()));
        logger.info("addRow: " + signalType);

        String[] paramName = switch (signalType) {
            case UNIFORM_NOISE, GAUSS_NOISE -> new String[]{"A", "t", "d"};
            case SINE, SINE_HALF, SINE_FULL -> new String[]{"A", "t", "d", "T"};
            case RECTANGLE, RECTANGLE_SYMETRIC, TRIANGLE -> new String[]{"A", "t", "d", "T", "kw"};
            case UNIT_STEP, UNIT_IMPULS, IMPULSE_NOISE -> null;
        };

        for (String s : Objects.requireNonNull(paramName)) {
            TextField textField = new TextField();
            textField.setPromptText("0.00");
            textField.setPrefWidth(40);

            textField.textProperty().addListener((obs, oldValue, newValue) -> {
                if (!newValue.matches("\\d*\\.?\\d*")) {
                    textField.setText(oldValue);
                }
            });

            row.getChildren().add(new Label(s));
            row.getChildren().add(textField);
        }

        Button removeButton = new Button("X");
        removeButton.setOnAction(e -> removeRow(row));
        row.getChildren().add(removeButton);

        rows.add(row);
        configurationVBox.getChildren().add(row);
    }

    private void removeRow(HBox row) {
        configurationVBox.getChildren().remove(row);
        rows.remove(row);
    }

    private void generateChart() {
        chartPane.getChildren().clear();
        LineChart<Number, Number> lineChart;

        if (operationMenu.getText().equals("None")) {
            lineChart = multiChart();
        } else {
            lineChart = aggregatedChart();
        }

        lineChart.setLayoutX(0);
        lineChart.setLayoutY(0);
        lineChart.setPrefSize(chartPane.getPrefWidth(), chartPane.getHeight());
        lineChart.setCreateSymbols(false);

        chartPane.getChildren().add(lineChart);
    }

    private LineChart<Number, Number> multiChart() {
        LineChart<Number, Number> lineChart = new LineChart<>(new NumberAxis(), new NumberAxis());

        Map<String, List<String>> mapOfParams = getActiveSignals();
        for (Map.Entry<String, List<String>> entry : mapOfParams.entrySet()) {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();

            String type = entry.getKey().split(": ")[1];
            SignalType signalType = SignalType.valueOf(type);
            Signal signal = SignalFactory.createSignal(signalType, entry.getValue());

            List<Double> samples = signal.getSamples();
            double sampleStep = SignalFactory.SAMPLE_STEP;
            double startTime = signal.getStartTime();

            for (int i = 0; i < samples.size(); i++) {
                double timestamp = startTime + i * sampleStep;
                series.getData().add(new XYChart.Data<>(timestamp, samples.get(i)));
            }

            series.setName(entry.getKey());
            lineChart.getData().add(series);
        }
        return lineChart;
    }

    private LineChart<Number, Number> aggregatedChart() {
        LineChart<Number, Number> lineChart = new LineChart<>(new NumberAxis(), new NumberAxis());
        XYChart.Series<Number, Number> series = new XYChart.Series<>();

        Map<Double, Double> aggregatedSamples = getAggregatedSamples();

        for (Map.Entry<Double, Double> entry : aggregatedSamples.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        series.setName("wykres");
        lineChart.getData().add(series);
        return lineChart;
    }

    private Map<Double, Double> getAggregatedSamples() {
        Map<Double, Double> aggregatedSamples = new HashMap<>();

        Map<String, List<String>> mapOfParams = getActiveSignals();

        for (Map.Entry<String, List<String>> entry : mapOfParams.entrySet()) {
            String type = entry.getKey().split(": ")[1];

            SignalType signalType = SignalType.valueOf(type);
            Signal signal = SignalFactory.createSignal(signalType, entry.getValue());

            List<Double> samples = signal.getSamples();
            double sampleStep = SignalFactory.SAMPLE_STEP;
            double startTime = signal.getStartTime();

            for (int i = 0; i < samples.size(); i++) {
                double timeStamp = startTime + i * sampleStep;
                if (!aggregatedSamples.containsKey(timeStamp)) {
                    aggregatedSamples.put(timeStamp, samples.get(i));
                } else {
                    double newValue = countNewValue(aggregatedSamples.get(timeStamp), samples.get(i));
                    aggregatedSamples.replace(timeStamp, newValue);
                }
            }
        }
        return aggregatedSamples;
    }

    private Map<String, List<String>> getActiveSignals() {
        Map<String, List<String>> mapOfParams = new HashMap<>();
        for (int i = 0; i < rows.size(); i++) {
            var children = rows.get(i).getChildren();
            List<String> params = new ArrayList<>();

            String signalType = null;
            if (children.getFirst() instanceof CheckBox checkBox && checkBox.isSelected()) {
                if (children.get(1) instanceof Label label) {
                    signalType = label.getText();
                }
                for (int j = 1; j < children.size(); j++) {
                    if (children.get(j) instanceof TextField textField) {
                        params.add(textField.getText());
                    }
                }
                String signalName = i + ": " + signalType;
                mapOfParams.put(signalName, params);
            }
        }
        return mapOfParams;
    }

    private double countNewValue(double oldValue, double newValue) {
        return switch (operationMenu.getText()) {
            case "Sum" -> oldValue + newValue;
            case "Difference" -> oldValue - newValue;
            case "Multiply" -> oldValue * newValue;
            case "Divide" -> newValue == 0 ? 0 : oldValue / newValue;
            default -> 0;
        };
    }
}
