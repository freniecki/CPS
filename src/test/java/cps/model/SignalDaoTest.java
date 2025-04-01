package cps.model;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

class SignalDaoTest {

    @Test
    void readSignalFromFile() {
        Signal signal = SignalDao.readSignalFromFile("signals/2025-03-31_21:29:12.990266305.ser");

        System.out.println(signal.getSignalType());
        for (Map.Entry<Double, Double> entry : signal.getTimestampSamples().entrySet()) {
            System.out.println(entry.getKey() + " " + entry.getValue());
        }
    }

    @Test
    void writeSignalToFile() {

        PeriodicSignal signal = PeriodicSignal.builder()
                .signalType(SignalType.SINE)
                .amplitude(1.0)
                .startTime(0.0)
                .durationTime(5.0)
                .period(1.0)
                .build();

        SignalDao.writeSignalToFile(signal);
    }
}