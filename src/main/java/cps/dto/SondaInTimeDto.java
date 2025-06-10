package cps.dto;

import cps.model.signals.Signal;

public record SondaInTimeDto(Signal theoreticalDistances, Signal measuredDistances) {
}
