package cps.fx;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;

public class MainController {
    @FXML private AnchorPane topMenuContainer;
    @FXML private AnchorPane signalListContainer;
    @FXML private AnchorPane chartContainer;
    @FXML private AnchorPane statisticsContainer;

    @FXML
    public void initialize() throws IOException {
        StatisticsController statisticsController;
        ChartController chartController;
        SignalListController signalListController;
        TopMenuController topMenuController;
        FXMLLoader topMenuLoader = new FXMLLoader(getClass().getResource("top_menu.fxml"));
        AnchorPane topMenuPane = topMenuLoader.load();
        topMenuController = topMenuLoader.getController();

        FXMLLoader signalListLoader = new FXMLLoader(getClass().getResource("signal_list.fxml"));
        AnchorPane signalListPane = signalListLoader.load();
        signalListController = signalListLoader.getController();

        FXMLLoader chartLoader = new FXMLLoader(getClass().getResource("chart_view.fxml"));
        AnchorPane chartPane = chartLoader.load();
        chartController = chartLoader.getController();

        FXMLLoader statisticsLoader = new FXMLLoader(getClass().getResource("statistics_view.fxml"));
        AnchorPane statisticsPane = statisticsLoader.load();
        statisticsController = statisticsLoader.getController();

        topMenuController.setSignalListController(signalListController);
        topMenuController.setChartController(chartController);
        topMenuController.setStatisticsController(statisticsController);

        topMenuContainer.getChildren().setAll(topMenuPane);
        signalListContainer.getChildren().setAll(signalListPane);
        chartContainer.getChildren().setAll(chartPane);
        statisticsContainer.getChildren().setAll(statisticsPane);
    }
}
