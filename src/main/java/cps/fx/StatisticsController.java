package cps.fx;

import cps.model.StatisticTool;
import cps.model.signals.Signal;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;

public class StatisticsController {

    @FXML private VBox statisticsVBox;

    public void calculateStats(List<Signal> signalList) {
        for (Signal signal : signalList) {
            Map<String, Double> stats = StatisticTool.getStatistics(signal.getTimestampSamples());
            addGrid(stats);
        }
    }

    private void addGrid(Map<String, Double> stats) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        int row = 0;
        for (Map.Entry<String, Double> stat : stats.entrySet()) {
            grid.add(new Label(stat.getKey() + ":"), 0, row);
            grid.add(new Label(String.format("%.6f", stat.getValue())), 1, row);
            row++;
        }
        statisticsVBox.getChildren().add(grid);
    }

    public void showHistogram(List<Signal> selectedSignals, int numberOfBins) {
        for (Signal signal : selectedSignals) {
            Map<String, Integer> histogramData = StatisticTool.createHistogramData(numberOfBins, signal.getSamples());

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
            for (Map.Entry<String, Integer> bin : histogramData.entrySet()) {
                series.getData().add(new XYChart.Data<>(bin.getKey(), bin.getValue()));
            }
            barChart.getData().add(series);

            statisticsVBox.getChildren().add(barChart);
        }
    }

    public void calculateMeasures(List<Signal> selectedSignals) {
        Map<String, Double> measures = StatisticTool.getMeasures(
                selectedSignals.getFirst().getTimestampSamples(),
                selectedSignals.get(1).getTimestampSamples()
        );
        addGrid(measures);
    }

    public void showSondaInfo(Map<String, Double> sondaInfo) {
        addGrid(sondaInfo);
    }

    public void clear() {
        statisticsVBox.getChildren().clear();
    }
}
