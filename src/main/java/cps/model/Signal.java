package cps.model;

import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Signal {
    SignalType signalType;
    List<Double> samples;
    Double sampleTime;
    Double amplitude;

    public int getSampleSize() {
        return samples.size();
    }
}
