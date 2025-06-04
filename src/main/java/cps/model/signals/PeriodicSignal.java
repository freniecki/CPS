package cps.model.signals;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class PeriodicSignal extends Signal {
    double period;

    @Override
    public String toString() {
        return "PeriodicSignal{" +
                "name=" + name +
                ", A=" + String.format("%.2f", amplitude) +
                ", t=" + String.format("%.2f", startTime) +
                ", d=" + String.format("%.2f", durationTime) +
                ", T=" + String.format("%.2f", period) +
                '}';
    }
}