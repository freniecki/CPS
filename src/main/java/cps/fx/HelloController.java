package cps.fx;

import cps.model.Signal;
import cps.model.SignalFactory;
import cps.model.SignalType;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;

import java.util.*;

public class HelloController {

    private final boolean[] signals = new boolean[11];
    private List<CheckBox> checkBoxList;
    private List<MenuItem> menuButtonsList;

    @FXML private Button generateButton;
    @FXML private Pane chartPane;

    @FXML private MenuButton operationMenuButton;
    @FXML private MenuItem sumMenuItem;
    @FXML private MenuItem differenceMenuItem;
    @FXML private MenuItem multiplyMenuItem;
    @FXML private MenuItem divideMenuItem;

    @FXML private CheckBox uniformNoiseCheckBox;
    @FXML private CheckBox gaussNoiseCheckBox;
    @FXML private CheckBox sineCheckBox;
    @FXML private CheckBox sineHalfCheckBox;
    @FXML private CheckBox sineFullCheckBox;
    @FXML private CheckBox rectangularCheckBox;
    @FXML private CheckBox rectangularSymetricCheckBox;
    @FXML private CheckBox triangularCheckBox;
    @FXML private CheckBox unitStepCheckBox;
    @FXML private CheckBox unitImpulseCheckBox;
    @FXML private CheckBox impulseNoiseCheckBox;

    @FXML
    private void initialize() {
        checkBoxList = List.of(uniformNoiseCheckBox, gaussNoiseCheckBox,
                sineCheckBox, sineHalfCheckBox, sineFullCheckBox,
                rectangularCheckBox, rectangularSymetricCheckBox, triangularCheckBox,
                unitStepCheckBox, unitImpulseCheckBox, impulseNoiseCheckBox);
        checkBoxList.forEach(c -> c.setOnAction(e -> addSignal(checkBoxList.indexOf(c))));

        menuButtonsList = List.of(sumMenuItem, differenceMenuItem, multiplyMenuItem, divideMenuItem);
        menuButtonsList.forEach(m -> m.setOnAction(e -> operationMenuButton.setText(m.getText())));

        generateButton.setOnAction(e -> generateChart());
    }

    private void addSignal(int id) {
        signals[id] = true;
    }

    private void generateChart() {
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);

        lineChart.setLayoutX(0);
        lineChart.setLayoutY(0);
        lineChart.setPrefSize(chartPane.getPrefWidth(), chartPane.getHeight());

        XYChart.Series<Number, Number> series = new XYChart.Series<>();

        // map of timestamp and aggregated value
        Map<Double, Double> aggregatedSamples = new HashMap<>();

        for (int i = 0; i < signals.length; i++) {
            if (signals[i]) {
                Signal signal = SignalFactory.createSignal(SignalType.values()[i]);
                if (signal == null) {
                    continue;
                }
                List<Double> samples = signal.getSamples();

                int sampleSize = signal.getSampleSize();
                double sampleTime = signal.getSampleTime();
                double timeDiff = sampleTime / sampleSize;

                for (int j = 0; j < sampleSize; j++) {
                    if (!aggregatedSamples.containsKey(timeDiff * j)) {
                        aggregatedSamples.put(timeDiff * j, samples.get(j));
                    } else {
                        double oldValue = aggregatedSamples.get(timeDiff * j);
                        double newValue = countNewValue(oldValue, samples.get(j));

                        aggregatedSamples.replace(timeDiff * j, newValue);
                    }
                }
            }
        }

        for (Map.Entry<Double, Double> entry : aggregatedSamples.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        series.setName("wykres");

        lineChart.getData().add(series);

        chartPane.getChildren().add(lineChart);

    }

    private double countNewValue(double oldValue, double newValue) {
        return switch (operationMenuButton.getText()) {
            case "Sum" -> oldValue + newValue;
            case "Difference" -> oldValue - newValue;
            case "Multiply" -> oldValue * newValue;
            case "Divide" -> oldValue / newValue;
            default -> 0;
        };
    }


}
