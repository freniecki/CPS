package cps.model.signals;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.LinkedHashMap;

@AllArgsConstructor
@Builder
@Data
public class SignalComplex {
    private LinkedHashMap<Double, Complex> timestampSamples;

    @Override
    public String toString() {
        return "ComplexSignal";
    }
}
