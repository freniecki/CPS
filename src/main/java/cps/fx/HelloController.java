package cps.fx;

import cps.model.Signal;
import cps.model.SignalFactory;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;

import java.util.List;

public class HelloController {

    @FXML private Button generateButton;
    @FXML private MenuButton operationMenuButton;
    @FXML private Pane chartPane;

    @FXML private CheckBox noiseCheckBox;
    @FXML private CheckBox gaussNoiseCheckBox;
    @FXML private CheckBox sineCheckBox;
    @FXML private CheckBox sine1halfCheckBox;
    @FXML private CheckBox sine2halfCheckBox;
    @FXML private CheckBox rectangularCheckBox;


    @FXML
    private void initialize() {
        generateButton.setOnAction(e -> generateChart());
    }

    private void generateChart() {
        // todo: based on selected checkbox create *operation* outcome of these signals


        if (sineCheckBox.isSelected()) {
            NumberAxis xAxis = new NumberAxis();
            NumberAxis yAxis = new NumberAxis();
            LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);

            lineChart.setLayoutX(0);
            lineChart.setLayoutY(0);
            lineChart.setPrefSize(chartPane.getPrefWidth(), chartPane.getHeight());

            XYChart.Series<Number, Number> series = new XYChart.Series<>();

            Signal sine = SignalFactory.createSineSignal();
            List<Double> samples = sine.getSamples();
            int sampleSize = sine.getSampleSize();
            double sampleTime = sine.getSampleTime();
            double timeDiff = sampleTime / sampleSize;

            for (int i = 0; i < sampleSize; i++) {
                series.getData().add(new XYChart.Data<>(timeDiff * i, samples.get(i)));
            }

            series.setName("wykres");

            lineChart.getData().add(series);

            chartPane.getChildren().add(lineChart);
        }

    }


}
