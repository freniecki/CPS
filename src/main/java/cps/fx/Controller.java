package cps.fx;

import cps.model.*;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.Group;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

public class Controller {
    private static final Logger logger = Logger.getLogger(Controller.class.getName());
    private static final String OPERATION = "Operation";
    private static final String NONE = "None";

    private final Map<String, Signal> signals = new HashMap<>();

    @FXML private Pane chartPane;
    @FXML private VBox statisticsVBox;
    @FXML private VBox configurationVBox;
    private final List<VBox> configurationList = new ArrayList<>();

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

    @FXML private Button readfileButton;
    @FXML private Button clearStatisticsButton;

    @FXML private Slider widthSlider;
    @FXML private ScrollPane centerScrollPane;

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
        showHistogramButton.setOnAction(e -> calculateAndDisplayHistogram());

        generateButton.setOnAction(e -> generateChart());

        readfileButton.setOnAction(e -> readFile());
        clearStatisticsButton.setOnAction(e -> statisticsVBox.getChildren().clear());

        widthSlider.setShowTickMarks(true);
        widthSlider.setShowTickLabels(true);
        widthSlider.setMajorTickUnit(400);
        widthSlider.setMinorTickCount(4);
        widthSlider.setBlockIncrement(100);
        widthSlider.valueProperty().addListener((obs, oldVal, newVal)
                -> logger.info("slider value changed: " + newVal));

        centerScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        centerScrollPane.setFitToWidth(false);
        centerScrollPane.setPannable(true);
    }

    // ---- Signal read from file ----

    private void readFile() {
        File file = fileChooser();
        if (file != null) {
            Signal signal = SignalDao.readSignalFromFile(file.getPath());
            if (signals.containsValue(signal)) {
                logger.warning("signal already exists");
            } else {
                String signalId = String.valueOf(configurationList.size());
                signals.put(signalId, signal);
                addCustomSignal();
            }
        } else {
            logger.warning("no file selected");
        }
    }

    private File fileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Signal Files", "*.ser"));

        return fileChooser.showOpenDialog(null);
    }

    // ---- Signal Config ----

    private void addConfigurationRow(int signalId) {
        VBox configurationRow = new VBox();
        configurationRow.setPadding(new Insets(10, 10, 10, 10));

        SignalType signalType = SignalType.values()[signalId];

        addInfoRow(signalType, configurationRow);
        addParamRow(signalType, configurationRow);

        configurationVBox.getChildren().add(configurationRow);
        configurationList.add(configurationRow);
    }

    private void addInfoRow(SignalType signalType, VBox configurationRow) {
        HBox infoRow = new HBox();

        CheckBox checkBox = new CheckBox("");
        checkBox.setPadding(new Insets(10));
        infoRow.getChildren().add(checkBox);

        Label idLabel = new Label(String.valueOf(configurationList.size()));
        idLabel.setPadding(new Insets(10));
        infoRow.getChildren().add(idLabel);

        Label typeLabel = new Label(signalType.name());
        typeLabel.setPadding(new Insets(10));
        infoRow.getChildren().add(typeLabel);

        Button removeButton = new Button("X");
        removeButton.setOnAction(e -> removeRow(configurationRow));
        removeButton.setPadding(new Insets(5));
        infoRow.getChildren().add(removeButton);

        configurationRow.getChildren().add(infoRow);
    }

    private void addParamRow(SignalType signalType, VBox configurationRow) {
        HBox paramRow = new HBox();

        String[] paramNames = switch (signalType) {
            case UNIFORM_NOISE, GAUSS_NOISE -> new String[]{"A", "t", "d", "fs"};
            case SINE, SINE_HALF, SINE_FULL -> new String[]{"A", "t", "d", "T", "fs"};
            case RECTANGLE, RECTANGLE_SYMETRIC, TRIANGLE -> new String[]{"A", "t", "d", "T", "kw", "fs"};
            case UNIT_STEP -> new String[]{"A", "t", "d", "s"};
            case UNIT_IMPULS -> new String[]{"A", "t", "d", "T", "i"};
            case IMPULSE_NOISE -> new String[]{"A", "t", "d", "T", "p"};
            case CUSTOM -> throw new IllegalArgumentException("no param for custom signal");
        };
        for (String s : Objects.requireNonNull(paramNames)) {
            if (s.equals("T")) {
                ComboBox<String> comboBox = new ComboBox<>();
                comboBox.getItems().add("T");
                comboBox.getItems().add("f");
                comboBox.setValue("T");
                paramRow.getChildren().add(comboBox);
            } else {
                Label label = new Label(s);
                label.setPadding(new Insets(10));
                paramRow.getChildren().add(label);
            }

            TextField textField = new TextField();
            textField.setPromptText("0.00");
            textField.setPrefWidth(60);
            textField.setPadding(new Insets(10));
            textField.textProperty().addListener((obs, oldValue, newValue) -> {
                if (!newValue.matches("\\d*\\.?\\d*")) {
                    textField.setText(oldValue);
                }
            });
            paramRow.getChildren().add(textField);
        }

        configurationRow.getChildren().add(paramRow);
    }

    private void removeRow(VBox row) {
        if (row.getChildren().getFirst() instanceof HBox infoRow
                && infoRow.getChildren().get(1) instanceof Label label
                && infoRow.getChildren().get(2) instanceof Label typeLabel
                && typeLabel.getText().equals(SignalType.CUSTOM.toString())) {
            Signal signalRemoved = signals.remove(label.getText());
            logger.info(signalRemoved.toString());
        }

        configurationVBox.getChildren().remove(row);
        configurationList.remove(row);

        // update ids
        for (int i = 0; i < configurationList.size(); i++) {
            VBox configurationRow = configurationList.get(i);
            if (configurationRow.getChildren().getFirst() instanceof HBox infoRow
                    && infoRow.getChildren().get(1) instanceof Label idLabel) {
                idLabel.setText(String.valueOf(i));
            }
        }
    }

    private void addCustomSignal() {
        VBox configurationRow = new VBox();
        configurationRow.setPadding(new Insets(10, 10, 10, 10));

        addInfoRow(SignalType.CUSTOM, configurationRow);

        configurationVBox.getChildren().add(configurationRow);
        configurationList.add(configurationRow);
    }

    // ---- Chart ----

    private void generateChart() {
        chartPane.getChildren().clear();
        LineChart<Number, Number> lineChart;

        if (operationMenu.getText().equals(NONE) || operationMenu.getText().equals(OPERATION)) {
            lineChart = multiChart();
        } else {
            lineChart = aggregatedChart();
        }

        logger.info("widthSlider: " + widthSlider.getValue());
        lineChart.setPrefSize(widthSlider.getValue(), chartPane.getPrefHeight());

        lineChart.setLayoutX(0);
        lineChart.setLayoutY(0);

        chartPane.getChildren().add(lineChart);
        centerScrollPane.setContent(new Group(chartPane));
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
            }

            if (signalType != SignalType.IMPULSE_NOISE && signalType != SignalType.UNIT_IMPULS) {
                for (XYChart.Data<Number, Number> data : series.getData()) {
                    Platform.runLater(() -> {
                        StackPane stackPane = (StackPane) data.getNode();
                        stackPane.setVisible(false);
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

        for (XYChart.Data<Number, Number> data : series.getData()) {
            Platform.runLater(() -> {
                StackPane stackPane = (StackPane) data.getNode();
                stackPane.setVisible(false);
            });
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

    // ---- Signal retriever ----

    private Map<String, Map<Double, Double>> getActiveSignals() {
        Map<String, Map<Double, Double>> activeSamples = new HashMap<>();

        for (VBox vBox : configurationList) {
            String signalId;
            String signalType;
            var infoHBox = vBox.getChildren().getFirst();

            if (infoHBox instanceof HBox hbox
                    && hbox.getChildren().getFirst() instanceof CheckBox checkBox
                    && checkBox.isSelected()
                    && hbox.getChildren().get(1) instanceof Label idLabel
                    && hbox.getChildren().get(2) instanceof Label typeLabel) {

                signalId = idLabel.getText();
                signalType = typeLabel.getText();
                String signalLabel = signalId + ": " + signalType;

                if (SignalType.valueOf(signalType) == SignalType.CUSTOM) {
                    Map<Double, Double> samples = signals.get(signalId).getTimestampSamples();
                    activeSamples.put(signalLabel, samples);
                    continue;
                }

                var paramHBox = vBox.getChildren().get(1);
                Map<String, String> params = getParams(paramHBox);
                logger.info(params.toString());

                if (params.containsKey("fs")) {
                    double samplingFrequency = Double.parseDouble(params.get("fs"));
                    if (samplingFrequency == 0) {
                        throw new IllegalArgumentException("sampling frequency must be positive");
                    }
                    double sampleStep = 1 / samplingFrequency;
                    SignalFactory.setSampleStep(sampleStep);

                    params.remove("fs");
                }

                Signal signal = SignalFactory.createSignal(SignalType.valueOf(signalType), params.values().stream().toList());
                Map<Double, Double> timestampSamples = Objects.requireNonNull(signal).getTimestampSamples();

                activeSamples.put(signalLabel, timestampSamples);
            }
        }
        return activeSamples;
    }

    private Map<String, String> getParams(Node paramHBox) {
        LinkedHashMap<String, String> paramMap = new LinkedHashMap<>();
        if (paramHBox instanceof HBox hBoxParam) {
            var paramRow = hBoxParam.getChildren();
            for (int i = 0; i < paramRow.size(); i += 2) {
                String label = getParamLabel(paramRow, i);
                String value = getParamValue(paramRow, i, label);

                paramMap.putLast(label, value);
            }
        }
        return paramMap;
    }

    private String getParamLabel(ObservableList<Node> paramRow, int i) {
        String label;
        if (paramRow.get(i) instanceof Label labelBox) {
            label = labelBox.getText();
        } else if (paramRow.get(i) instanceof ComboBox<?> comboBox) {
            label = comboBox.getValue().toString();
        } else {
            throw new IllegalArgumentException("mamy problem, nie ma label");
        }
        return label;
    }

    private String getParamValue(ObservableList<Node> paramRow, int i, String label) {
        String value;
        if (paramRow.get(i + 1) instanceof TextField textField) {
            if (label.equals("f")) {
                value = String.valueOf(1 / Double.parseDouble(textField.getText()));
            } else {
                value = textField.getText();
            }
        } else {
            throw new IllegalArgumentException("mamy problem, nie ma value");
        }
        return value;
    }

    private void createNewSignal(Map<Double, Double> aggregatedSamples) {
        Signal signal = SignalFactory.createSignal(aggregatedSamples);
        String signalId = String.valueOf(configurationList.size());
        // add signal to available signals
        signals.put(signalId, signal);
        // write new signal to file
        SignalDao.writeSignalToFile(signal);
        // add signal to configuration row
        addCustomSignal();
    }

    // ---- Statistics ----

    private void calculateAndDisplayStatistics() {
        Map<String, Map<Double, Double>> mapOfSamples = getActiveSignals();
        if (mapOfSamples.isEmpty()) {
            showAlert("No active signals", "Please select at least one signal to calculate statistics.");
            return;
        }

        if (!operationMenu.getText().equals(NONE) && !operationMenu.getText().equals(OPERATION)) {
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

    private void calculateAndDisplayHistogram() {
        Map<String, Map<Double, Double>> mapOfSamples = getActiveSignals();
        if (mapOfSamples.isEmpty()) {
            showAlert("No active signals", "Please select at least one signal to show histogram.");
            return;
        }

        int numBins = Integer.parseInt(histogramBinsComboBox.getValue());

        if (!operationMenu.getText().equals(NONE) && !operationMenu.getText().equals(OPERATION)) {
            String operationMenuText = operationMenu.getText();
            List<Double> cleanSamples = getAggregatedSamples().values().stream().toList();
            Map<String, Integer> histogramData = StatisticTool.createHistogramData(numBins, cleanSamples);

            addHistogramRow(operationMenuText, histogramData);
        }

        for (Map.Entry<String, Map<Double, Double>> entry : mapOfSamples.entrySet()) {
            String signalName = entry.getKey();
            List<Double> cleanSamples = entry.getValue().values().stream().toList();
            Map<String, Integer> histogramData = StatisticTool.createHistogramData(numBins, cleanSamples);

            addHistogramRow(signalName, histogramData);
        }
    }

    private void addHistogramRow(String signalName, Map<String, Integer> histogramData) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Amplitude Range");
        yAxis.setLabel("Density");

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Amplitude Distribution");
        barChart.setPrefWidth(400);
        barChart.setMinHeight(300);
        barChart.setBarGap(0.0);
        barChart.setCategoryGap(0.0);

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
