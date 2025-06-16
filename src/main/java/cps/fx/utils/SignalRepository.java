package cps.fx.utils;

import cps.model.SignalIO;
import cps.model.signals.Signal;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.util.logging.Logger;

public class SignalRepository {
    private static final Logger logger = Logger.getLogger(SignalRepository.class.getName());

    private final ObservableList<Signal> signalList = FXCollections.observableArrayList();

    private static final SignalRepository INSTANCE = new SignalRepository();

    private SignalRepository() {}

    public static SignalRepository getInstance() {
        return INSTANCE;
    }

    public ObservableList<Signal> getSignals() {
        return signalList;
    }

    public void addSignal(Signal signal) {
        signalList.add(signal);
        try {
            SignalIO.writeSignalToFile(signal);
        } catch (IOException e) {
            logger.warning("Failed to save signal to file.");
        }
    }

    public void removeSignal(Signal signal) {
        signalList.remove(signal);
    }

    public boolean contains(Signal signal) {
        return signalList.contains(signal);
    }

    public void clear() {
        signalList.clear();
    }
}
