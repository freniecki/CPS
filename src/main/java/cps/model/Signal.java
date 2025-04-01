package cps.model;

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
}