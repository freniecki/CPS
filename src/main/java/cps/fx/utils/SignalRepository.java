package cps.fx.utils;

import cps.model.signals.Signal;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class SignalRepository {
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
