package com.edulink.gui.controllers.journal;

import com.edulink.gui.services.journal.NoteService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.PieChart;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

public class AdminJournalStatsController implements Initializable {

    @FXML
    private PieChart categoryChart;
    private NoteService noteService = new NoteService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadStats();
    }

    private void loadStats() {
        Map<String, Integer> stats = noteService.getNoteCountByCategory();
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

        stats.forEach((category, count) -> {
            if (count > 0) {
                pieChartData.add(new PieChart.Data(category + " (" + count + ")", count));
            }
        });

        categoryChart.setData(pieChartData);
    }
}
