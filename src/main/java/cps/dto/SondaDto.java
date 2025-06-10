package cps.dto;

import cps.model.signals.Signal;

public record SondaDto(Signal correlationSignal, Signal baseBufforedSignal, Signal shiftedBufforedSignal, double measuredDistance) {

}
