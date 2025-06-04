package cps.model.signals;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.*;

@Data
@AllArgsConstructor
@SuperBuilder
public class Signal implements Serializable {
    @Builder.Default
    String name = "Signal";

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

    @Override
    public String toString() {
        return "Signal{" +
                "name=" + name +
                ", A=" + String.format("%.2f", amplitude) +
                ", t=" + String.format("%.2f", startTime) +
                ", d=" + String.format("%.2f", durationTime) +
                "}";
    }
}