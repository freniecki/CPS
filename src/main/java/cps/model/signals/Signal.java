package cps.model.signals;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.*;

@Data
@AllArgsConstructor
@SuperBuilder
public class Signal implements Serializable {
    double amplitude;
    double startTime;
    double durationTime;

    LinkedHashMap<Double, Double> timestampSamples;
    SignalType signalType;

    public double getTimeStep() {
        List<Double> timestamps = getTimestamps();
        return timestamps.get(1) - timestamps.get(0);
    }

    public List<Double> getTimestamps() {
        return new ArrayList<>(timestampSamples.keySet());
    }

    public List<Double> getSamples() {
        return new ArrayList<>(timestampSamples.values());
    }
}