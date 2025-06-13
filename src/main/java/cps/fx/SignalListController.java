package cps.fx;

import cps.fx.utils.SignalRepository;
import cps.fx.utils.TextFieldFactory;
import cps.model.SignalFactory;
import cps.model.signals.Signal;
import cps.model.signals.SignalType;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public class SignalListController {
    private static final Logger logger = Logger.getLogger(SignalListController.class.getName());

    @FXML private ComboBox<SignalType> signalTypeComboBox;
    @FXML private Button createSignalButton;
    @FXML private Button clearSignalsListButton;

    @FXML private VBox parametersVBox;
    @FXML private ListView<Signal> signalsListView;

    @FXML
    public void initialize() {
        signalsListView.setPadding(new Insets(5));
        signalTypeComboBox.setItems(FXCollections.observableArrayList(SignalType.values()));
        signalTypeComboBox.getSelectionModel().selectFirst();
        signalTypeComboBox.setOnAction(e -> addCreateSignalPanel());

        createSignalButton.setPadding(new Insets(5));
        createSignalButton.setOnAction(e -> createSignal());

        clearSignalsListButton.setOnAction(e -> {
            for (Signal signal : signalsListView.getSelectionModel().getSelectedItems()) {
                SignalRepository.getInstance().removeSignal(signal);
            }
        });

        signalsListView.setItems(SignalRepository.getInstance().getSignals());
        signalsListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    // ---- SIGNAL LISTVIEW ACCESS ----

    public List<Signal> getSelectedSignals() {
        return signalsListView.getSelectionModel().getSelectedItems();
    }

    // ---- PARAMETERS PANEL CREATION ----

    private void addCreateSignalPanel() {
        int signalId = signalTypeComboBox.getSelectionModel().getSelectedIndex();
        addConfigurationRow(signalId);
    }

    private void addConfigurationRow(int signalId) {
        SignalType signalType = SignalType.values()[signalId];

        HBox labelHBox = createLabelRow(signalType);

        HBox configurationRow = createParamRow(signalType);
        configurationRow.setPadding(new Insets(5));

        parametersVBox.getChildren().clear();
        parametersVBox.getChildren().addAll(labelHBox, configurationRow);
    }

    private HBox createLabelRow(SignalType signalType) {
        HBox labelHBox = new HBox();
        Label signalLabel = new Label(signalType.toString());
        signalLabel.setPadding(new Insets(5));

        TextField customNameField = new TextField();
        customNameField.setPromptText("Custom name");
        customNameField.setPrefWidth(100);
        customNameField.setPadding(new Insets(5));

        labelHBox.getChildren().addAll(signalLabel, customNameField);
        return labelHBox;
    }

    private HBox createParamRow(SignalType signalType) {
        HBox paramRow = new HBox();
        paramRow.setSpacing(5);

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
                label.setPadding(new Insets(5, 2, 5, 2));
                paramRow.getChildren().add(label);
            }
            TextField textField = TextFieldFactory.createPositiveDoubleField();
            textField.setPrefWidth(40);
            paramRow.getChildren().add(textField);
        }
        return paramRow;
    }

    // ---- SIGNAL CREATION ----

    private void createSignal() {
        if (parametersVBox.getChildren().isEmpty()) return;

        HBox labelHBox = (HBox) parametersVBox.getChildren().getFirst();
        Label signalLabel = (Label) labelHBox.getChildren().getFirst();
        SignalType signalType = SignalType.valueOf(signalLabel.getText());

        TextField customNameLabel = (TextField) labelHBox.getChildren().get(1);
        String customName = customNameLabel.getText();

        Map<String, String> params = getParams(parametersVBox.getChildren().get(1));

        if (params.containsKey("fs")) {
            double samplingFrequency = Double.parseDouble(params.remove("fs"));
            if (samplingFrequency <= 0) {
                logger.warning("Sampling frequency must be positive.");
                return;
            }
            SignalFactory.setSampleStep(1 / samplingFrequency);
        }

        try {
            Signal signal = SignalFactory.createSignal(signalType, params);
            if (!customName.isEmpty()) {
                signal.setName(customName);
            }

            SignalRepository.getInstance().addSignal(signal);
            parametersVBox.getChildren().clear();
        } catch (Exception e) {
            logger.warning("Failed to create signal: " + e.getMessage());
        }
    }

    private LinkedHashMap<String, String> getParams(Node node) {
        LinkedHashMap<String, String> paramMap = new LinkedHashMap<>();
        if (node instanceof HBox hBoxParam) {
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

}
