package cps.dto;

import cps.model.signals.Signal;
import lombok.Builder;

@Builder
public record FiltrationDto(Signal filteredSignal, Signal coefficients) {
}
