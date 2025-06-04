package cps.fx;

import cps.dto.FiltrationDto;
import cps.model.*;
import cps.model.signals.Signal;
import cps.model.signals.SignalType;
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
    private static final String LINEAR = "linear";
    private static final String NO_ACTIVE_SIGNALS = "No active signals";

    private final Map<String, Signal> signalsObjects = new LinkedHashMap<>();
    private final Map<String, String> reconstructions = new HashMap<>();

    @FXML public TextField cutoffFrequencyTextField;
    @FXML public TextField mParameterTextField;
    @FXML public ChoiceBox<String> filtrateChoiceBox;
    @FXML public Button filtrateButton;

    @FXML public TextField distanceTextField;
    @FXML public TextField signalVelocityTextField;
    @FXML public TextField bufferSizeTextField;
    @FXML public Button startSondaButton;

    @FXML private VBox signalsUIList;
    @FXML private Pane chartPane;
    @FXML private VBox statisticsVBox;

    @FXML private VBox paramPane;
    @FXML private Button createSignalButton;

    @FXML private Button generateButton;
    @FXML private Button calculateStatsButton;
    @FXML private Button calculateMeasuresButton;

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

    @FXML private Button readFileButton;
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

        createSignalButton.setOnAction(e -> createSignal());

        List<MenuItem> operationItemList;
        operationItemList = List.of(noneMenuItem, sumMenuItem, differenceMenuItem, multiplyMenuItem, divideMenuItem);
        operationItemList.forEach(item -> item.setOnAction(e -> operationMenu.setText(item.getText())));

        calculateStatsButton.setOnAction(e -> calculateAndDisplayStatistics());
        calculateMeasuresButton.setOnAction(e -> calculateAndDisplayMeasures());
        showHistogramButton.setOnAction(e -> calculateAndDisplayHistogram());

        generateButton.setOnAction(e -> generateChart());

        readFileButton.setOnAction(e -> readFile());
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

        filtrateButton.setOnAction(e -> filtrate());

        startSondaButton.setOnAction(e -> startSonda());
    }

    // ------------ Sonda ------------

    private void startSonda() {
        Map<String, Signal> activeSignals = getActiveSignals();
        if (!isOneSignalActive(activeSignals)) {
            return;
        }
        Signal baseSignal = activeSignals.values().iterator().next();

        if (distanceTextField.getText().isEmpty()
                || signalVelocityTextField.getText().isEmpty()
                || bufferSizeTextField.getText().isEmpty()) {
            showAlert("Missing parameters", "Please fill all the parameters.");
            return;
        }

        double distance = Double.parseDouble(distanceTextField.getText());
        double signalVelocity = Double.parseDouble(signalVelocityTextField.getText());
        int bufferSize = Integer.parseInt(bufferSizeTextField.getText());

        // find time shift
        List<Double> timestamps = baseSignal.getTimestamps();
        double timeStep = timestamps.get(1) - timestamps.getFirst();
        int arrivalIndex = (int) Math.round(2 * distance / signalVelocity / timeStep);

        // create buffered subsignals
        Map<Double, Double> transmitterSamples = new HashMap<>();
        for (int i = 0; i < bufferSize; i++) {
            transmitterSamples.put(timestamps.get(i), baseSignal.getSamples().get(i));
        }
        Signal transmitterSignal = SignalFactory.createSignal(transmitterSamples);
        addNewSignal(transmitterSignal);

        Map<Double, Double> receiverSamples = new HashMap<>();
        for (int i = 0; i < bufferSize; i++) {
            receiverSamples.put(timestamps.get(i), baseSignal.getSamples().get(i + arrivalIndex));
        }
        Signal receiverSignal = SignalFactory.createSignal(receiverSamples);
        addNewSignal(receiverSignal);

        // ---- create correlation ----
        Signal correlationSignal = SignalOperations.crossCorrelateSignal(transmitterSignal, receiverSignal);
        addNewSignal(correlationSignal);

        // ---- calculate correlation-based distance
        List<Double> correlationProduct = correlationSignal.getSamples();
        int correlationSize = correlationProduct.size();
        int correlationMiddle = correlationSize / 2;
        int maxIndex = correlationMiddle;
        for (int i = correlationMiddle; i < correlationSize; i++) {
            maxIndex = correlationProduct.get(i) > correlationProduct.get(maxIndex) ? i : maxIndex;
        }

        double detectedDelay = (maxIndex) * timeStep;
        double measuredDistance = (detectedDelay * signalVelocity) / 2.0;
        logger.info("detectedDelay: " + detectedDelay);
        logger.info("measuredDistance: " + measuredDistance);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("Signal: " + activeSignals.keySet().iterator().next()), 0, 0);
        grid.add(new Label("Distance: " + distance), 0, 1);
        grid.add(new Label("Signal velocity: " + signalVelocity), 0, 2);
        grid.add(new Label("Buffer size: " + bufferSize), 0, 3);
        grid.add(new Label("Estimated distance: " + measuredDistance), 0, 4);

        statisticsVBox.getChildren().add(grid);
    }

    private boolean isOneSignalActive(Map<String, Signal> activeSignals) {
        if (activeSignals.isEmpty()) {
            showAlert(NO_ACTIVE_SIGNALS, "Please select at least one signal to filtrate.");
            return false;
        }

        if (activeSignals.size() > 1) {
            showAlert("Too many active signals", "Please select only one signal to filtrate.");
            return false;
        }
        return true;
    }

    // ------------ Filtration ------------

    private void filtrate() {
        Map<String, Signal> activeSignals = getActiveSignals();
        if (!isOneSignalActive(activeSignals)) {
            return;
        }

        FiltrationDto filtrationDto = switch (filtrateChoiceBox.getValue()) {
            case "Low Pass" -> SignalOperations.lowPassFIRFiltration(
                    activeSignals.values().iterator().next(),
                    Integer.parseInt(mParameterTextField.getText()),
                    Double.parseDouble(cutoffFrequencyTextField.getText())
            );
            case "High Pass" -> SignalOperations.highPassFIRFiltration(
                    activeSignals.values().iterator().next(),
                    Integer.parseInt(mParameterTextField.getText()),
                    Double.parseDouble(cutoffFrequencyTextField.getText())
            );
            default -> throw new IllegalStateException("Unexpected value: " + filtrateChoiceBox.getValue());
        };

        addNewSignal(filtrationDto.filteredSignal());
    }

    // ------------ Signal read from file ------------

    private void readFile() {
        File file = fileChooser();
        if (file != null) {
            Signal signal = SignalDao.readSignalFromFile(file.getPath());
            if (signalsObjects.containsValue(signal)) {
                logger.warning("signal already exists");
            } else {
                String signalId = String.valueOf(signalsObjects.size());
                signalsObjects.put(signalId, signal);
                createSignalUIRow(signalId, signal.getSignalType().toString());
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

    // ------------ Signal Parameters Input ------------

    private void addConfigurationRow(int signalId) {
        SignalType signalType = SignalType.values()[signalId];

        HBox labelHBox = new HBox();
        Label signalLabel = new Label(signalType.toString());
        signalLabel.setPadding(new Insets(10, 10, 10, 10));
        labelHBox.getChildren().add(signalLabel);

        HBox configurationRow = createParamRow(signalType);
        configurationRow.setPadding(new Insets(10, 10, 10, 10));

        paramPane.getChildren().clear();
        paramPane.getChildren().addAll(labelHBox, configurationRow);
    }

    private HBox createParamRow(SignalType signalType) {
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
                comboBox.getItems().addAll("T", "f");
                comboBox.setValue("T");
                paramRow.getChildren().add(comboBox);
            } else {
                Label label = new Label(s);
                label.setPadding(new Insets(5));
                paramRow.getChildren().add(label);
            }

            paramRow.getChildren().add(positiveTextField());
        }

        return paramRow;
    }

    private TextField positiveTextField() {
        TextField textField = new TextField();
        textField.setPromptText("0.00");
        textField.setPrefWidth(60);
        textField.setPadding(new Insets(10));
        textField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("\\d*\\.?\\d*")) {
                textField.setText(oldValue);
            }
        });
        return textField;
    }

    // ------------ Signal creation ------------

    private void createSignal() {
        // parse configuration row
        HBox labelHBox = (HBox) paramPane.getChildren().getFirst();
        Label signalLabel = (Label) labelHBox.getChildren().getFirst();
        SignalType signalType = SignalType.valueOf(signalLabel.getText());

        Map<String, String> params = getParams(paramPane.getChildren().get(1));

        // create signal
        if (params.containsKey("fs")) {
            double samplingFrequency = Double.parseDouble(params.remove("fs"));
            if (samplingFrequency == 0) {
                throw new IllegalArgumentException("sampling frequency must be positive");
            }
            double sampleStep = 1 / samplingFrequency;
            logger.info("sample step: " + sampleStep);
            SignalFactory.setSampleStep(sampleStep);
        }
        Signal signal = SignalFactory.createSignal(signalType, params);

        // add to signalsObjects
        String signalId = String.valueOf(signalsObjects.size());
        signalsObjects.put(signalId, signal);

        // add to signalsUIList
        signalsUIList.getChildren().add(createSignalUIRow(signalId, signalType.name()));
        signalsUIList.getChildren().add(createSignalUIInfoRow(params));

        // remove configurationRow
        paramPane.getChildren().clear();
    }

    private Map<String, String> getParams(Node paramHBox) {
        LinkedHashMap<String, String> paramMap = new LinkedHashMap<>();
        if (paramHBox instanceof HBox hBoxParam) {
            var paramRow = hBoxParam.getChildren();

            for (int i = 0; i < paramRow.size(); i += 2) {
                String label = getParamLabel(paramRow, i);
                String value = getParamValue(paramRow, i + 1, label);

                if (label.equals("f")) label = "T";
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
        if (paramRow.get(i) instanceof TextField textField) {
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

    // ------------ Signal UI Row ------------
    private HBox createSignalUIRow(String signalId, String signalType) {
        HBox signalUIRow = new HBox(5);

        CheckBox checkBox = new CheckBox("");
        checkBox.setPadding(new Insets(10));
        signalUIRow.getChildren().add(checkBox);

        Label idLabel = new Label(signalId);
        idLabel.setPadding(new Insets(10));
        signalUIRow.getChildren().add(idLabel);

        Label typeLabel = new Label(signalType);
        typeLabel.setPadding(new Insets(10));
        signalUIRow.getChildren().add(typeLabel);

        ChoiceBox<String> quantizationBits = new ChoiceBox<>();
        quantizationBits.getItems().addAll("1", "2", "3", "4", "5", "6", "7", "8");
        quantizationBits.setValue("8");
        signalUIRow.getChildren().add(quantizationBits);

        ChoiceBox<String> quantizationType = new ChoiceBox<>();
        quantizationType.getItems().addAll("none", "with cut", "with rounding");
        quantizationType.setValue("none");
        signalUIRow.getChildren().add(quantizationType);

        ChoiceBox<String> reconstructionType = new ChoiceBox<>();
        reconstructionType.getItems().addAll("zero-order hold", LINEAR, "sinc-based");
        reconstructionType.setValue(LINEAR);
        signalUIRow.getChildren().add(reconstructionType);

        Button removeButton = new Button("X");
        removeButton.setOnAction(e -> removeRow(signalUIRow));
        removeButton.setPadding(new Insets(5));
        signalUIRow.getChildren().add(removeButton);

        return signalUIRow;
    }

    private Node createSignalUIInfoRow(Map<String, String> params) {
        HBox signalInfoRow = new HBox(5);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key.equals("T")) {
                key = "f";
                value = String.valueOf(1 / Double.parseDouble(value));
            }

            Label label = new Label(key + ": " + value);
            label.setPadding(new Insets(5));
            signalInfoRow.getChildren().add(label);
        }
        return signalInfoRow;
    }

    // ------------ ... ------------

    private void removeRow(Pane row) {
        if (row instanceof HBox signalInfoRow
                && signalInfoRow.getChildren().get(1) instanceof Label signalId) {

            int rowIndex = signalsUIList.getChildren().indexOf(row);
            signalsUIList.getChildren().remove(row);
            signalsUIList.getChildren().remove(rowIndex);

            Signal signalRemoved = signalsObjects.remove(signalId.getText());

            logger.info("signal removed:" + signalRemoved.toString());
        }
    }

    // ------------ Chart ------------

    private void generateChart() {
        chartPane.getChildren().clear();

        LineChart<Number, Number> lineChart = switch (operationMenu.getText()) {
            case NONE, OPERATION -> multiChart();
            default -> aggregatedChart();
        };

        lineChart.setPrefSize(widthSlider.getValue(), chartPane.getPrefHeight());
        lineChart.setLayoutX(0);
        lineChart.setLayoutY(0);

        chartPane.getChildren().add(lineChart);
        centerScrollPane.setContent(new Group(chartPane));
    }

    private LineChart<Number, Number> multiChart() {
        LineChart<Number, Number> lineChart = new LineChart<>(new NumberAxis(), new NumberAxis());

        Map<String, Signal> activeSignals = getActiveSignals();
        for (Map.Entry<String, Signal> entry : activeSignals.entrySet()) {
            XYChart.Series<Number, Number> series = switch (reconstructions.get(entry.getKey())) {
                case "zero-order hold" -> createZeroHoldSeries(entry.getValue().getTimestampSamples());
                case LINEAR -> createLinearInterpolationSeries(entry.getValue().getTimestampSamples());
                case "sinc-based" -> createSincBasedSeries(entry.getValue().getTimestampSamples());
                default -> {
                    XYChart.Series<Number, Number> s = new XYChart.Series<>();
                    for (Map.Entry<Double, Double> sample : entry.getValue().getTimestampSamples().entrySet()) {
                        s.getData().add(new XYChart.Data<>(sample.getKey(), sample.getValue()));
                    }
                    yield s;
                }
            };

            SignalType signalType = entry.getValue().getSignalType();

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

            series.setName(entry.getKey() + " (" + signalType + ")");
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

        Map<String, Signal> activeSignals = getActiveSignals();

        for (Map.Entry<String, Signal> entry : activeSignals.entrySet()) {
            Signal signal = entry.getValue();

            Map<Double, Double> samples = signal.getTimestampSamples();
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

    // -------- Series manipulation --------

    private XYChart.Series<Number, Number> createZeroHoldSeries(Map<Double, Double> samples) {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();

        for (int i = 0; i < samples.size(); i++) {
            double x0 = (double) samples.keySet().toArray()[i];
            double x1 = (double) samples.keySet().toArray()[(i+1)%samples.size()];
            double y = samples.get(x0);

            if (i == samples.size() - 1) {
                series.getData().add(new XYChart.Data<>(x0, y));
                continue;
            }
            series.getData().add(new XYChart.Data<>(x0, y));
            series.getData().add(new XYChart.Data<>(x1, y));
        }

        return series;
    }

    private XYChart.Series<Number, Number> createLinearInterpolationSeries(Map<Double, Double> samples) {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        for (Map.Entry<Double, Double> sample : samples.entrySet()) {
            series.getData().add(new XYChart.Data<>(sample.getKey(), sample.getValue()));
        }
        return series;
    }

    public XYChart.Series<Number, Number> createSincBasedSeries(Map<Double, Double> samples) {
        List<Map.Entry<Double, Double>> sortedSamples = samples.entrySet().stream().sorted(Comparator.comparingDouble(Map.Entry::getKey)).toList();
        logger.info("in samples: " + sortedSamples.size());

        double tMin = sortedSamples.getFirst().getKey();
        double tMax = sortedSamples.getLast().getKey();
        int size = sortedSamples.size() - 1;
        double period = (tMax - tMin) / size;

        double interpolationTimeStep = (tMax - tMin) / (5 * size);

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        for (double t = tMin; t < tMax; t += interpolationTimeStep) {
            double value = 0;
            for (int n = 0; n < size; n++) {
                value += sortedSamples.get(n).getValue() * sinc(t / period - n);
            }

            series.getData().add(new XYChart.Data<>(t, value));
        }
        logger.info("out samples: " + series.getData().size());

        return series;
    }

    // ------------ Signal retriever ------------

    private Map<String, Signal> getActiveSignals() {
        Map<String, Signal> activeSignals = new HashMap<>();

        for (Node node : signalsUIList.getChildren()) {
            if (node instanceof HBox signalUIRow
                && signalUIRow.getChildren().getFirst() instanceof CheckBox signalCheckBox
                && signalCheckBox.isSelected()
                && signalUIRow.getChildren().get(1) instanceof Label idLabel
                && signalUIRow.getChildren().get(3) instanceof ChoiceBox<?> quantizationBitsChoiceBox
                && signalUIRow.getChildren().get(4) instanceof ChoiceBox<?> quantizationTypeChoiceBox
                && signalUIRow.getChildren().get(5) instanceof ChoiceBox<?> reconstructionTypeChoiceBox) {

                String signalId = idLabel.getText();
                int quantizationBits = Integer.parseInt((String) quantizationBitsChoiceBox.getValue());
                String quantizationType = (String) quantizationTypeChoiceBox.getValue();
                Signal newSignal = SignalFactory.createSignalWithQuantization(signalsObjects.get(signalId), quantizationBits, quantizationType);
                activeSignals.put(signalId, newSignal);

                String reconstructionType = (String) reconstructionTypeChoiceBox.getValue();
                reconstructions.put(signalId, reconstructionType);

                logger.fine("id: " + signalId + " | " + quantizationBits + " " + quantizationType + " " + reconstructionType + " | " + newSignal.toString());
            }
        }

        return activeSignals;
    }

    private void createNewSignal(Map<Double, Double> aggregatedSamples) {
        addNewSignal(SignalFactory.createSignal(aggregatedSamples));
    }

    private void addNewSignal(Signal signal) {
        String signalId = String.valueOf(signalsObjects.size());
        signalsObjects.put(signalId, signal);

        //SignalDao.writeSignalToFile(signal);

        signalsUIList.getChildren().add(createSignalUIRow(signalId, signal.getSignalType().toString()));
        signalsUIList.getChildren().add(createSignalUIInfoRow(Map.of("", "")));
    }

    // ------------ Statistics ------------

    private void calculateAndDisplayStatistics() {
        Map<String, Signal> activeSignals = getActiveSignals();
        if (activeSignals.isEmpty()) {
            showAlert(NO_ACTIVE_SIGNALS, "Please select at least one signal to calculate statistics.");
            return;
        }

        if (!operationMenu.getText().equals(NONE) && !operationMenu.getText().equals(OPERATION)) {
            addStatisticsRow(operationMenu.getText(), getAggregatedSamples());
        }

        for (Map.Entry<String, Signal> entry : activeSignals.entrySet()) {
            addStatisticsRow(entry.getKey(), entry.getValue().getTimestampSamples());
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

    // ------------ Measures ------------

    private void calculateAndDisplayMeasures() {
        Map<String, Signal> activeSignals = getActiveSignals();
        if (activeSignals.isEmpty()) {
            showAlert(NO_ACTIVE_SIGNALS, "Please select at least one signal to calculate statistics.");
            return;
        }

        for (Map.Entry<String, Signal> entry : activeSignals.entrySet()) {
            String signalId = entry.getKey();
            Signal baseSignal = signalsObjects.get(signalId);
            Signal reconstructedSignal = activeSignals.get(signalId);
            addSignalMeasuresRow(entry.getKey(), baseSignal.getTimestampSamples(), reconstructedSignal.getTimestampSamples());
        }
    }

    private void addSignalMeasuresRow(String signalName, Map<Double, Double> baseSignalSamples, Map<Double, Double> reconstructedSignalSamples) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        double MSE = StatisticTool.getMSE(baseSignalSamples, reconstructedSignalSamples);
        double SNR = StatisticTool.getSNR(baseSignalSamples, reconstructedSignalSamples);
        double PSNR = StatisticTool.getPSNR(baseSignalSamples, reconstructedSignalSamples);
        double ENOB = StatisticTool.getENOB(baseSignalSamples, reconstructedSignalSamples);
        double MD = StatisticTool.getMD(baseSignalSamples, reconstructedSignalSamples);

        grid.add(new Label("signalId: " + signalName), 0, 0);
        grid.add(new Label("MSE:"), 0, 1);
        grid.add(new Label(String.format("%.6f", MSE)), 1, 1);
        grid.add(new Label("SNR:"), 0, 2);
        grid.add(new Label(String.format("%.2f", SNR)), 1, 2);
        grid.add(new Label("PSNR:"), 0, 3);
        grid.add(new Label(String.format("%.2f", PSNR)), 1, 3);
        grid.add(new Label("ENOB:"), 0, 4);
        grid.add(new Label(String.format("%.2f", ENOB)), 1, 4);
        grid.add(new Label("MD:"), 0, 5);
        grid.add(new Label(String.format("%.6f", MD)), 1, 5);

        statisticsVBox.getChildren().add(grid);
    }

    // ------------ Histogram ------------

    private void calculateAndDisplayHistogram() {
        Map<String, Signal> activeSignals = getActiveSignals();
        if (activeSignals.isEmpty()) {
            showAlert(NO_ACTIVE_SIGNALS, "Please select at least one signal to show histogram.");
            return;
        }

        int numBins = Integer.parseInt(histogramBinsComboBox.getValue());

        if (!operationMenu.getText().equals(NONE) && !operationMenu.getText().equals(OPERATION)) {
            String operationMenuText = operationMenu.getText();
            List<Double> cleanSamples = getAggregatedSamples().values().stream().toList();
            Map<String, Integer> histogramData = StatisticTool.createHistogramData(numBins, cleanSamples);

            addHistogramRow(operationMenuText, histogramData);
        }

        for (Map.Entry<String, Signal> entry : activeSignals.entrySet()) {
            String signalName = entry.getKey();
            List<Double> cleanSamples = entry.getValue().getTimestampSamples().values().stream().toList();
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

    // ------------ Tools ------------

    private double sinc(double x) {
        if (x == 0.0) return 1.0;
        return Math.sin(Math.PI * x) / (Math.PI * x);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
