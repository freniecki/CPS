package cps.fx;

import cps.model.*;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
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
import javafx.stage.Stage;

import java.util.*;
import java.util.List;
import java.util.logging.Logger;

public class HelloController {
    private static final Logger logger = Logger.getLogger(HelloController.class.getName());

    @FXML private Pane chartPane;
    @FXML private VBox statisticsVBox;
    @FXML private VBox configurationVBox;
    private final List<HBox> rows = new ArrayList<>();

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
        List<MenuItem> operationItemList;
        List<MenuItem> signalItemList;
        signalItemList = List.of(uniformNoiseMenuItem, gaussNoiseMenuItem, sineMenuItem, sineHalfMenuItem, sineFullMenuItem,
                rectangularMenuItem, rectangularSymetricMenuItem, triangularMenuItem, unitStepMenuItem,
                unitImpulseMenuItem, impulseNoiseMenuItem);
        signalItemList.forEach(item -> item.setOnAction(e -> addRow(signalItemList.indexOf(item))));

        operationItemList = List.of(noneMenuItem, sumMenuItem, differenceMenuItem, multiplyMenuItem, divideMenuItem);
        operationItemList.forEach(item -> item.setOnAction(e -> operationMenu.setText(item.getText())));
        calculateStatsButton.setOnAction(e -> calculateAndDisplayStatistics());
        showHistogramButton.setOnAction(e -> showHistogram());
        generateButton.setOnAction(e -> generateChart());
    }

    // ---- Signal Config ----

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

    // ---- Chart ----

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

    // ---- Statistics ----

    private void calculateAndDisplayStatistics() {
        Map<String, List<String>> mapOfParams = getActiveSignals();
        if (mapOfParams.isEmpty()) {
            showAlert("No active signals", "Please select at least one signal to calculate statistics.");
            return;
        }

        // Dla każdego aktywnego sygnału oblicz statystyki
        for (Map.Entry<String, List<String>> entry : mapOfParams.entrySet()) {
            String signalName = entry.getKey();
            String type = signalName.split(": ")[1];
            SignalType signalType = SignalType.valueOf(type);
            Signal signal = SignalFactory.createSignal(signalType, entry.getValue());

            // Pobierz wszystkie parametry
            Map<String, Double> parameters = signal.calculateAllParameters();

            // Wyświetl okno z parametrami
//            showStatisticsDialog(signalName, parameters);
            addStatisticsRow(signalName, parameters);
        }
    }

    private void addStatisticsRow(String signalName, Map<String, Double> parameters) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        int row = 0;
        for (Map.Entry<String, Double> param : parameters.entrySet()) {
            grid.add(new Label(param.getKey() + ":"), 0, row);
            grid.add(new Label(String.format("%.6f", param.getValue())), 1, row);
            row++;
        }

        statisticsVBox.getChildren().add(grid);
    }

    private void showStatisticsDialog(String signalName, Map<String, Double> parameters) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Signal Statistics: " + signalName);
        dialog.setHeaderText("Parameters for " + signalName);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        int row = 0;
        for (Map.Entry<String, Double> param : parameters.entrySet()) {
            grid.add(new Label(param.getKey() + ":"), 0, row);
            grid.add(new Label(String.format("%.6f", param.getValue())), 1, row);
            row++;
        }

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);

        dialog.showAndWait();
    }

    // ---- Histogram ----

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showHistogram() {
        Map<String, List<String>> mapOfParams = getActiveSignals();
        if (mapOfParams.isEmpty()) {
            showAlert("No active signals", "Please select at least one signal to show histogram.");
            return;
        }

        // Pobierz liczbę przedziałów z ComboBox
        int numBins = Integer.parseInt(histogramBinsComboBox.getValue());

        // Dla każdego aktywnego sygnału pokaż histogram
        for (Map.Entry<String, List<String>> entry : mapOfParams.entrySet()) {
            String signalName = entry.getKey();
            String type = signalName.split(": ")[1];
            SignalType signalType = SignalType.valueOf(type);
            Signal signal = SignalFactory.createSignal(signalType, entry.getValue());

            // Pobierz dane histogramu
            Map<String, Integer> histogramData = signal.createHistogramData(numBins);

            // Pokaż histogram w nowym oknie
            showHistogramWindow(signalName, histogramData);
        }
    }

    private void showHistogramWindow(String signalName, Map<String, Integer> histogramData) {
        // Tworzenie nowego okna dla histogramu
        Stage histogramStage = new Stage();
        histogramStage.setTitle("Histogram: " + signalName);

        // Tworzenie wykresu słupkowego (BarChart)
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Amplitude Range");
        yAxis.setLabel("Frequency");

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Amplitude Distribution");

        // Tworzenie serii danych
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Frequency");

        // Dodawanie danych do serii
        for (Map.Entry<String, Integer> bin : histogramData.entrySet()) {
            series.getData().add(new XYChart.Data<>(bin.getKey(), bin.getValue()));
        }

        barChart.getData().add(series);

        // Dodanie wykresu do sceny
        Scene scene = new Scene(barChart, 800, 600);
        histogramStage.setScene(scene);

        // Pokazanie okna
        histogramStage.show();
    }
}
