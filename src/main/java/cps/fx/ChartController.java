package cps.fx;

import cps.model.signals.Signal;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;

public class ChartController {
    @FXML private ScrollPane chartScrollPane;
    @FXML private VBox chartsContainerVBox;

    public void generateChart(List<Signal> signals, String chartType) {
        switch (chartType) {
            case "Multichart" -> showMultichart(signals);
            case "Separate" -> showSeparateCharts(signals);
            default -> throw new IllegalStateException("Unexpected chart type: " + chartType);
        }
    }

    private void showMultichart(List<Signal> signals) {
        LineChart<Number, Number> lineChart = new LineChart<>(new NumberAxis(), new NumberAxis());

        for (Signal signal : signals) {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            for (Map.Entry<Double, Double> sample : signal.getTimestampSamples().entrySet()) {
                series.getData().add(new XYChart.Data<>(sample.getKey(), sample.getValue()));
            }
            lineChart.getData().add(series);
        }

        chartsContainerVBox.getChildren().add(lineChart);
    }

    private void showSeparateCharts(List<Signal> signals) {
        for (Signal signal : signals) {
            LineChart<Number, Number> lineChart = new LineChart<>(new NumberAxis(), new NumberAxis());
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            for (Map.Entry<Double, Double> sample : signal.getTimestampSamples().entrySet()) {
                series.getData().add(new XYChart.Data<>(sample.getKey(), sample.getValue()));
            }
            lineChart.getData().add(series);
            chartsContainerVBox.getChildren().add(lineChart);
        }
    }


    public void clear() {
        chartsContainerVBox.getChildren().clear();
    }
}
