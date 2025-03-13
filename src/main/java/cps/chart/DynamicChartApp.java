package cps.chart;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class DynamicChartApp extends Application {

    private XYChart.Series<Number, Number> series;
    private int index = 0;  // Indeks dla osi X

    @Override
    public void start(Stage primaryStage) {
        // Tworzymy osie wykresu
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Indeks");
        yAxis.setLabel("Wartość");

        // Tworzymy wykres liniowy
        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Dynamiczny wykres");
        series = new XYChart.Series<>();
        series.setName("Dane użytkownika");
        lineChart.getData().add(series);

        // Pole do wprowadzania wartości
        TextField inputField = new TextField();
        inputField.setPromptText("Wpisz wartość liczbową");

        // Przycisk do dodawania punktów
        Button addButton = new Button("Dodaj do wykresu");
        addButton.setOnAction(event -> {
            try {
                double value = Double.parseDouble(inputField.getText());
                series.getData().add(new XYChart.Data<>(index++, value));
                inputField.clear();
            } catch (NumberFormatException e) {
                inputField.setText("Błąd! Wpisz liczbę.");
            }
        });

        // Layout
        VBox layout = new VBox(10, inputField, addButton, lineChart);
        Scene scene = new Scene(layout, 600, 400);

        // Ustawienia okna
        primaryStage.setTitle("Dynamiczny wykres w JavaFX");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

