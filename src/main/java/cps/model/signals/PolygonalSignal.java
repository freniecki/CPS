package cps.model.signals;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class PolygonalSignal extends PeriodicSignal{
    double dutyCycle;

    @Override
    public String toString() {
        return "PolygonalSignal{" +
                "name=" + name +
                ", A=" + String.format("%.2f", amplitude) +
                ", t=" + String.format("%.2f", startTime) +
                ", d=" + String.format("%.2f", durationTime) +
                ", T=" + String.format("%.2f", period) +
                ", kw=" + String.format("%.2f", dutyCycle) +
                '}';
    }
}
