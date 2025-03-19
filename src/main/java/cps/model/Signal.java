package cps.model;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@AllArgsConstructor
@SuperBuilder
public class Signal {
    int sampleCount;
    double sampleTime;
    List<Double> samples;

    SignalType signalType;
}
