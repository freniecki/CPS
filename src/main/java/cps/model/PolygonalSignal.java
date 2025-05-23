package cps.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class PolygonalSignal extends PeriodicSignal{
    double dutyCycle;
}
