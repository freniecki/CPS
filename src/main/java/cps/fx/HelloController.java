package cps.fx;

import cps.model.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.util.*;
import java.util.List;
import java.util.logging.Logger;

public class HelloController {
    private static final Logger logger = Logger.getLogger(HelloController.class.getName());

    private final Map<String, Signal> signals = new HashMap<>();

    @FXML private Pane chartPane;
    @FXML private VBox statisticsVBox;
    @FXML private VBox configurationVBox;
    private final List<VBox> configurationRows = new ArrayList<>();

    @FXML private Button generateButton;
    @FXML private Button calculateStatsButton;

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
    @FXML private ComboBox<String> histogramBinsComboBox;
    @FXML private Button showHistogramButton;

    @FXML
    private void initialize() {
        List<MenuItem> signalItemList;
        signalItemList = List.of(uniformNoiseMenuItem, gaussNoiseMenuItem, sineMenuItem, sineHalfMenuItem, sineFullMenuItem,
                rectangularMenuItem, rectangularSymetricMenuItem, triangularMenuItem, unitStepMenuItem,
                unitImpulseMenuItem, impulseNoiseMenuItem);
        signalItemList.forEach(item -> item.setOnAction(e -> addConfigurationRow(signalItemList.indexOf(item))));

        List<MenuItem> operationItemList;
        operationItemList = List.of(noneMenuItem, sumMenuItem, differenceMenuItem, multiplyMenuItem, divideMenuItem);
        operationItemList.forEach(item -> item.setOnAction(e -> operationMenu.setText(item.getText())));

        calculateStatsButton.setOnAction(e -> calculateAndDisplayStatistics());
        showHistogramButton.setOnAction(e -> showHistogram());

        generateButton.setOnAction(e -> generateChart());
    }

    // ---- Signal Config ----

    private void addConfigurationRow(int signalId) {
        VBox configurationRow = new VBox();
        configurationRow.setPadding(new Insets(10, 10, 10, 10));

        SignalType signalType = SignalType.values()[signalId];

        addInfoRow(signalType, configurationRow);
        addParamRow(signalType, configurationRow);

        configurationVBox.getChildren().add(configurationRow);
        configurationRows.add(configurationRow);
    }

    private void addInfoRow(SignalType signalType, VBox configurationRow) {
        HBox infoRow = new HBox();

        CheckBox checkBox = new CheckBox("");
        checkBox.setPadding(new Insets(10));
        infoRow.getChildren().add(checkBox);

        String name = configurationRows.size() + ": " + signalType.name();
        Label label = new Label(name);
        label.setPadding(new Insets(10));
        infoRow.getChildren().add(label);

        configurationRow.getChildren().add(infoRow);
    }

    private void addParamRow(SignalType signalType, VBox configurationRow) {
        HBox paramRow = new HBox();

        String[] paramNames = switch (signalType) {
            case UNIFORM_NOISE, GAUSS_NOISE, UNIT_STEP, UNIT_IMPULS, IMPULSE_NOISE, CUSTOM -> new String[]{"A", "t", "d"};
            case SINE, SINE_HALF, SINE_FULL -> new String[]{"A", "t", "d", "T"};
            case RECTANGLE, RECTANGLE_SYMETRIC, TRIANGLE -> new String[]{"A", "t", "d", "T", "kw"};
        };
        for (String s : Objects.requireNonNull(paramNames)) {
            Label label = new Label(s);
            label.setPadding(new Insets(10));
            paramRow.getChildren().add(label);

            TextField textField = new TextField();
            textField.setPromptText("0.00");
            textField.setPrefWidth(40);
            textField.setPadding(new Insets(10));
            textField.textProperty().addListener((obs, oldValue, newValue) -> {
                if (!newValue.matches("\\d*\\.?\\d*")) {
                    textField.setText(oldValue);
                }
            });
            paramRow.getChildren().add(textField);
        }

        Button removeButton = new Button("X");
        removeButton.setOnAction(e -> removeRow(configurationRow));
        removeButton.setPadding(new Insets(5));
        paramRow.getChildren().add(removeButton);

        configurationRow.getChildren().add(paramRow);
    }

    private void removeRow(VBox row) {
        configurationVBox.getChildren().remove(row);
        configurationRows.remove(row);
    }

    private void addCustomSignal() {
        VBox configurationRow = new VBox();
        configurationRow.setPadding(new Insets(10, 10, 10, 10));

        addInfoRow(SignalType.CUSTOM, configurationRow);

        configurationVBox.getChildren().add(configurationRow);
        configurationRows.add(configurationRow);
    }

    // ---- Chart ----
    private void generateChart() {
        chartPane.getChildren().clear();
        LineChart<Number, Number> lineChart;

        if (operationMenu.getText().equals("None") || operationMenu.getText().equals("Operation")) {
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

        Map<String, Map<Double, Double>> mapOfParams = getActiveSignals();
        for (Map.Entry<String, Map<Double, Double>> entry : mapOfParams.entrySet()) {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();

            for (Map.Entry<Double, Double> sample : entry.getValue().entrySet()) {
                series.getData().add(new XYChart.Data<>(sample.getKey(), sample.getValue()));
            }

            SignalType signalType = SignalType.valueOf(entry.getKey().split(": ")[1]);
            if (signalType == SignalType.IMPULSE_NOISE || signalType == SignalType.UNIT_IMPULS) {
                Platform.runLater(() -> series.getNode().setStyle("-fx-stroke: transparent"));

                for (XYChart.Data<Number, Number> data : series.getData()) {
                    data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                        if (newNode != null) {
                            newNode.setStyle("-fx-background-color: white, green; -fx-background-radius: 10px;");
                        }
                    });
                }
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

        createNewSignal(aggregatedSamples);

        for (Map.Entry<Double, Double> entry : aggregatedSamples.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        series.setName("wykres");
        lineChart.getData().add(series);
        return lineChart;
    }

    private Map<Double, Double> getAggregatedSamples() {
        Map<Double, Double> aggregatedSamples = new HashMap<>();

        Map<String, Map<Double, Double>> mapOfParams = getActiveSignals();

        for (Map.Entry<String, Map<Double, Double>> entry : mapOfParams.entrySet()) {
            logger.info("name" + entry.getKey());
            logger.info("" + entry.getValue());

            Map<Double, Double> samples = entry.getValue();
            for (Map.Entry<Double, Double> sample : samples.entrySet()) {
                if (aggregatedSamples.containsKey(sample.getKey())) {
                    double newValue = countNewValue(aggregatedSamples.get(sample.getKey()), sample.getValue());
                    aggregatedSamples.put(sample.getKey(), newValue);
                } else {
                    aggregatedSamples.put(sample.getKey(), sample.getValue());
                }
            }
        }

        return aggregatedSamples;
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

    private Map<String, Map<Double, Double>> getActiveSignals() {
        Map<String, Map<Double, Double>> activeSamples = new HashMap<>();

        for (VBox vBox : configurationRows) {
            String signalId;
            String signalType;
            var infoHBox = vBox.getChildren().getFirst();

            if (infoHBox instanceof HBox hbox
                    && hbox.getChildren().getFirst() instanceof CheckBox checkBox
                    && checkBox.isSelected()
                    && hbox.getChildren().get(1) instanceof Label label) {

                    String signalLabel = label.getText();
                    signalId = signalLabel.split(": ")[0];
                    signalType = signalLabel.split(": ")[1];

                    if (SignalType.valueOf(signalType) == SignalType.CUSTOM) {
                        Map<Double, Double> samples = getTimestampSamples(signals.get(signalId));
                        activeSamples.put(signalLabel, samples);
                    } else {
                        var paramHBox = vBox.getChildren().get(1);
                        List<String> params = getParams(paramHBox);
                        Signal signal = SignalFactory.createSignal(SignalType.valueOf(signalType), params);
                        Map<Double, Double> timestampSamples = getTimestampSamples(Objects.requireNonNull(signal));

                        activeSamples.put(signalLabel, timestampSamples);
                    }
                }
        }
        return activeSamples;
    }

    private List<String> getParams(Node paramHBox) {
        List<String> params = new ArrayList<>();
        if (paramHBox instanceof HBox hBoxParam) {
            var paramRow = hBoxParam.getChildren();
            for (Node node : paramRow) {
                if (node instanceof TextField textField) {
                    params.add(textField.getText());
                }
            }
        }
        return params;
    }

    private Map<Double, Double> getTimestampSamples(Signal signal) {
        List<Double> samples = signal.getSamples();
        double sampleStep = SignalFactory.SAMPLE_STEP;
        double startTime = signal.getStartTime();

        Map<Double, Double> timestampSamples = new HashMap<>();
        for (int j = 0; j < samples.size(); j++) {
            double timestamp = startTime + j * sampleStep;
            timestampSamples.put(timestamp, samples.get(j));
        }
        return timestampSamples;
    }

    private void createNewSignal(Map<Double, Double> aggregatedSamples) {
        Signal signal = SignalFactory.createCustomSignal(aggregatedSamples);
        String signalId = String.valueOf(configurationRows.size());
        signals.put(signalId, signal);
        addCustomSignal();
    }

    // ---- Statistics ----

    private void calculateAndDisplayStatistics() {
        Map<String, Map<Double, Double>> mapOfSamples = getActiveSignals();
        if (mapOfSamples.isEmpty()) {
            showAlert("No active signals", "Please select at least one signal to calculate statistics.");
            return;
        }

        if (!Objects.equals(operationMenu.getText(), "None")) {
            addStatisticsRow(operationMenu.getText(), getAggregatedSamples());
        }

        for (Map.Entry<String, Map<Double, Double>> entry : mapOfSamples.entrySet()) {
            addStatisticsRow(entry.getKey(), entry.getValue());
        }
    }

    private void addStatisticsRow(String signalName, Map<Double, Double> samples) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        Map<String, Double> stats = StatisticTool.getStatistics(samples);

        int row = 0;
        for (Map.Entry<String, Double> stat : stats.entrySet()) {
            if (row == 0) {
                grid.add(new Label(signalName), 0, row);
                row++;
            }
            grid.add(new Label(stat.getKey() + ":"), 0, row);
            grid.add(new Label(String.format("%.6f", stat.getValue())), 1, row);
            row++;
        }

        statisticsVBox.getChildren().add(grid);
    }

    // ---- Histogram ----

    private void showHistogram() {
        Map<String, Map<Double, Double>> mapOfSamples = getActiveSignals();
        if (mapOfSamples.isEmpty()) {
            showAlert("No active signals", "Please select at least one signal to show histogram.");
            return;
        }

        int numBins = Integer.parseInt(histogramBinsComboBox.getValue());

        if (!Objects.equals(operationMenu.getText(), "None")) {
            String operationMenuText = operationMenu.getText();
            List<Double> cleanSamples = getAggregatedSamples().values().stream().toList();
            Map<String, Integer> histogramData = StatisticTool.createHistogramData(numBins, cleanSamples);

            showHistogramWindow(operationMenuText, histogramData);
        }

        for (Map.Entry<String, Map<Double, Double>> entry : mapOfSamples.entrySet()) {
            String signalName = entry.getKey();
            List<Double> cleanSamples = entry.getValue().values().stream().toList();
            Map<String, Integer> histogramData = StatisticTool.createHistogramData(numBins, cleanSamples);

            showHistogramWindow(signalName, histogramData);
        }
    }

    private void showHistogramWindow(String signalName, Map<String, Integer> histogramData) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Amplitude Range");
        yAxis.setLabel("Density");

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Amplitude Distribution");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(signalName);

        for (Map.Entry<String, Integer> bin : histogramData.entrySet()) {
            series.getData().add(new XYChart.Data<>(bin.getKey(), bin.getValue()));
        }

        barChart.getData().add(series);

        statisticsVBox.getChildren().add(barChart);
    }

    // ---- Tools ----

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
