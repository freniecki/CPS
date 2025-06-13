package cps.fx;

import cps.dto.SondaDto;
import cps.dto.SondaInTimeDto;
import cps.fx.enums.TransformationType;
import cps.fx.utils.SignalRepository;
import cps.fx.enums.FiltrationType;
import cps.fx.enums.OperationType;
import cps.dto.FiltrationDto;
import cps.model.SignalDao;
import cps.model.SignalFactory;
import cps.model.SignalOperations;
import cps.model.signals.Complex;
import cps.model.signals.Signal;
import cps.simulator.SondaCore;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import lombok.Setter;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TopMenuController {
    private static final Logger logger = Logger.getLogger(TopMenuController.class.getName());

    @Setter
    @FXML private SignalListController signalListController;
    @Setter
    @FXML private ChartController chartController;
    @Setter
    @FXML private StatisticsController statisticsController;

    // Operacje
    @FXML private ComboBox<OperationType> operationTypeComboBox;
    @FXML private Button operationButton;

    // Wykres
    @FXML private ComboBox<String> generateChartComboBox;
    @FXML private Button generateChartButton;
    @FXML private Button clearChartsButton;

    // Filtracja
    @FXML private ComboBox<FiltrationType> filtrationTypeComboBox;
    @FXML private TextField cutoffFrequencyTextField;
    @FXML private Button filtrateButton;

    // Parametry
    @FXML private TextField mParameterTextField;

    // Plik i szerokość
    @FXML private Button readFileButton;
    @FXML private Slider widthSlider;

    // Statystyki / Histogram
    @FXML private Button calculateStatsButton;
    @FXML private ComboBox<String> histogramBinsComboBox;
    @FXML private Button showHistogramButton;
    @FXML private Button calculateMeasuresButton;

    // Sonda
    @FXML private TextField distanceTextField;
    @FXML private TextField signalVelocityTextField;
    @FXML private TextField bufferSizeTextField;
    @FXML private Button startSondaButton;
    @FXML private Button startInTimeSondaButton;

    @FXML private ComboBox<TransformationType> transformationComboBox;
    @FXML private ComboBox<Integer> log2NComboBox;
    @FXML private Button transformButton;

    @FXML private Button clearStatisticsButton;

    @FXML
    public void initialize() {
        operationTypeComboBox.setItems(FXCollections.observableArrayList(OperationType.values()));
        operationTypeComboBox.getSelectionModel().selectFirst();
        operationButton.setOnAction(e -> performOperation());

        generateChartComboBox.setItems(FXCollections.observableArrayList("Multichart", "Separate"));
        generateChartButton.setOnAction(e -> generateChart());

        clearChartsButton.setOnAction(e -> chartController.clear());

        filtrationTypeComboBox.setItems(FXCollections.observableArrayList(FiltrationType.values()));
        filtrationTypeComboBox.getSelectionModel().selectFirst();
        filtrateButton.setOnAction(e -> filtrate());

        readFileButton.setOnAction(e -> readFile());

        calculateStatsButton.setOnAction(e -> calculateStats());

        histogramBinsComboBox.setItems(FXCollections.observableArrayList("5", "10", "15", "20"));
        histogramBinsComboBox.getSelectionModel().selectFirst();
        showHistogramButton.setOnAction(e -> showHistogram());

        calculateMeasuresButton.setOnAction(e -> calculateMeasures());

        startSondaButton.setOnAction(e -> startSonda());
        startInTimeSondaButton.setOnAction(e -> startInTimeSonda());

        transformationComboBox.setItems(FXCollections.observableArrayList(TransformationType.values()));
        transformationComboBox.getSelectionModel().selectFirst();
        log2NComboBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        transformButton.setOnAction(e -> transform());

        clearStatisticsButton.setOnAction(e -> statisticsController.clear());
    }

    private void performOperation() {
        List<Signal> selectedSignals = signalListController.getSelectedSignals();
        OperationType operationType = operationTypeComboBox.getSelectionModel().getSelectedItem();

        List<String> signalNames = selectedSignals.stream().map(Signal::getName).toList();
        StringBuilder sb = new StringBuilder(signalNames.getFirst());
        for (int i = 1; i < signalNames.size(); i++) {
            sb.append(switch (operationType) {
                case SUM -> "+";
                case DIFFERENCE -> "-";
                case MULTIPLY -> "*";
                case DIVIDE -> "/";
            });
            sb.append(signalNames.get(i));
        }

        Signal resultSignal = switch (operationType) {
            case SUM -> SignalOperations.sum(selectedSignals);
            case DIFFERENCE -> SignalOperations.difference(selectedSignals);
            case MULTIPLY -> SignalOperations.multiply(selectedSignals);
            case DIVIDE -> SignalOperations.divide(selectedSignals);
        };
        resultSignal.setName(sb.toString());

        SignalRepository.getInstance().addSignal(resultSignal);
    }

    private void generateChart() {
        List<Signal> selectedSignals = signalListController.getSelectedSignals();

        String chartType = generateChartComboBox.getSelectionModel().getSelectedItem();

        chartController.generateChart(selectedSignals, chartType);
    }

    private void filtrate() {
        List<Signal> selectedSignals = signalListController.getSelectedSignals();
        if (selectedSignals.size() != 1) {
            logger.warning("Choose one signal.");
            return;
        }

        Signal signal = selectedSignals.getFirst();
        int m = Integer.parseInt(mParameterTextField.getText());
        double cutoffFrequency = Double.parseDouble(cutoffFrequencyTextField.getText());

        FiltrationDto filtrationDto = switch (filtrationTypeComboBox.getValue()) {
            case LOW_PASS -> SignalOperations.lowPassFIRFiltration(signal, m, cutoffFrequency);
            case HIGH_PASS -> SignalOperations.highPassFIRFiltration(signal, m, cutoffFrequency);
        };

        SignalRepository.getInstance().addSignal(filtrationDto.filteredSignal());
        SignalRepository.getInstance().addSignal(filtrationDto.coefficients());
    }

    private void readFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Signal Files", "*.ser"));

        File file =  fileChooser.showOpenDialog(null);

        // todo: refactor according to java.dev guide on file handling
        if (file == null) {
            logger.warning("Noo file selected");
            return;
        }

        Signal signal = SignalDao.readSignalFromFile(file.getPath());
        if (SignalRepository.getInstance().contains(signal)) {
            logger.warning("Signal already exists");
        } else {
            SignalRepository.getInstance().addSignal(signal);
        }
    }

    private void calculateStats() {
        List<Signal> selectedSignals = signalListController.getSelectedSignals();
        if (selectedSignals.isEmpty()) {
            logger.warning("Choose at least one signal.");
            return;
        }

        statisticsController.calculateStats(selectedSignals);
    }

    private void showHistogram() {
        int bins = Integer.parseInt(histogramBinsComboBox.getValue());
        List<Signal> selectedSignals = signalListController.getSelectedSignals();
        if (selectedSignals.isEmpty()) {
            logger.warning("Choose at least one signal.");
            return;
        }

        statisticsController.showHistogram(selectedSignals, bins);
    }

    private void calculateMeasures() {
        List<Signal> selectedSignals = signalListController.getSelectedSignals();
        if (selectedSignals.size() != 2) {
            logger.warning("Choose exactly two signals.");
            return;
        }

        statisticsController.calculateMeasures(selectedSignals);
    }

    private void startSonda() {
        double distance = Double.parseDouble(distanceTextField.getText());
        double signalVelocity = Double.parseDouble(signalVelocityTextField.getText());
        int bufferSize = Integer.parseInt(bufferSizeTextField.getText());

        List<Signal> selectedSignals = signalListController.getSelectedSignals();
        if (selectedSignals.size() != 1) {
            logger.warning("Choose one signal.");
            return;
        }

        SondaDto sondaDto = SondaCore.run(selectedSignals.getFirst(), distance, signalVelocity, bufferSize);
        if (sondaDto == null) {
            logger.warning("Sonda failed");
            return;
        }

        Signal baseBufforedSignal = sondaDto.baseBufforedSignal();
        baseBufforedSignal.setName("baseBuffered");
        SignalRepository.getInstance().addSignal(baseBufforedSignal);

        Signal shiftedBufforedSignal = sondaDto.shiftedBufforedSignal();
        shiftedBufforedSignal.setName("shiftedBuffered");
        SignalRepository.getInstance().addSignal(shiftedBufforedSignal);

        Signal correlationSignal = sondaDto.correlationSignal();
        correlationSignal.setName("correlation");
        SignalRepository.getInstance().addSignal(correlationSignal);

        Map<String, Double> sondaInfo = new HashMap<>();
        sondaInfo.put("distance", distance);
        sondaInfo.put("signalVelocity", signalVelocity);
        sondaInfo.put("bufferSize", (double) bufferSize);
        sondaInfo.put("estimatedDistance", sondaDto.measuredDistance());
        statisticsController.showSondaInfo(sondaInfo);
    }

    private void startInTimeSonda() {
        double distance = Double.parseDouble(distanceTextField.getText());
        double signalVelocity = Double.parseDouble(signalVelocityTextField.getText());
        int bufferSize = Integer.parseInt(bufferSizeTextField.getText());

        List<Signal> selectedSignals = signalListController.getSelectedSignals();
        if (selectedSignals.size() != 1) {
            logger.warning("Choose one signal.");
            return;
        }
        SondaInTimeDto resultDto = SondaCore.runInTime(selectedSignals.getFirst(), distance, 10, signalVelocity, 0.1, bufferSize);

        SignalRepository.getInstance().addSignal(resultDto.theoreticalDistances());
        SignalRepository.getInstance().addSignal(resultDto.measuredDistances());

        logger.info("teo: " + resultDto.theoreticalDistances().getTimestampSamples());
        logger.info("meas: " + resultDto.measuredDistances().getTimestampSamples());
    }

    private void transform() {
        List<Signal> selectedSignals = signalListController.getSelectedSignals();
        if (selectedSignals.size() != 1) {
            logger.warning("Choose one signal.");
            return;
        }

        int log2N = log2NComboBox.getValue();
        int size = 1 << log2N;
        List<Double> samplesList = selectedSignals.getFirst().getSamples().subList(0, size);
        double[] samples = samplesList.stream().mapToDouble(Double::doubleValue).toArray();

        switch (transformationComboBox.getValue()) {
            case DFT -> runFourier(SignalOperations.dft(samples, log2N));
            case FFT -> runFourier(SignalOperations.fftDIF(samples, log2N));
            case DCT, FCT -> runCosine(samples, log2N);
        }

    }

    private void runFourier(Complex[] result) {
        List<Double> reals = Arrays.stream(result).map(Complex::real).toList();
        Map<Double, Double> realSamples = new LinkedHashMap<>();
        for (int i = 0; i < reals.size(); i++) {
            realSamples.put((double) i, reals.get(i));
        }
        Signal realSignal = SignalFactory.createSignal(realSamples);
        realSignal.setName("real");
        SignalRepository.getInstance().addSignal(realSignal);

        List<Double> imaginaries = Arrays.stream(result).map(Complex::imaginary).toList();
        Map<Double, Double> imaginariesSamples = new LinkedHashMap<>();
        for (int i = 0; i < reals.size(); i++) {
            imaginariesSamples.put((double) i, imaginaries.get(i));
        }
        Signal imaginarySignal = SignalFactory.createSignal(imaginariesSamples);
        imaginarySignal.setName("imaginary");
        SignalRepository.getInstance().addSignal(imaginarySignal);
    }

    private void runCosine(double[] samples, int log2N) {

    }
}
