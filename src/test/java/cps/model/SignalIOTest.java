package cps.model;

import cps.model.signals.PeriodicSignal;
import cps.model.signals.Signal;
import cps.model.signals.SignalType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

class SignalIOTest {

    @Test
    void readSignalFromFile() {
        Signal signal = SignalIO.readSignalFromFile("signals/2025-03-31_21:29:12.990266305.ser");

        System.out.println(signal.getSignalType());
        for (Map.Entry<Double, Double> entry : signal.getTimestampSamples().entrySet()) {
            System.out.println(entry.getKey() + " " + entry.getValue());
        }
    }

    @Test
    void writeSignalToFile() throws IOException {

        PeriodicSignal signal = PeriodicSignal.builder()
                .signalType(SignalType.SINE)
                .amplitude(1.0)
                .startTime(0.0)
                .durationTime(5.0)
                .period(1.0)
                .build();

        SignalIO.writeSignalToFile(signal);
    }
}