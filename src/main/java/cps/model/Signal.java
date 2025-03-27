package cps.model;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.*;

@Data
@AllArgsConstructor
@SuperBuilder
public class Signal {
    double amplitude;
    double startTime;
    double durationTime;

    LinkedHashMap<Double, Double> timestampSamples;
    SignalType signalType;
}