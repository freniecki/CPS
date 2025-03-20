package cps.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class PeriodicSignal extends Signal {
    double period;

    @Override
    public List<Double> getSamplesForCalculation() {
        // Oblicz liczbę pełnych okresów
        double timeSpan = durationTime - startTime;
        int fullPeriods = (int)(timeSpan / period);

        // Jeśli nie ma pełnych okresów, zwróć pustą listę
        if (fullPeriods <= 0) return new ArrayList<>();

        // Oblicz liczbę próbek w jednym okresie
        double samplesPerPeriod = period / SignalFactory.SAMPLE_STEP;
        int fullSamplePeriods = (int)samplesPerPeriod;

        // Utwórz listę zawierającą tylko pełne okresy
        List<Double> fullPeriodSamples = new ArrayList<>();
        for (int i = 0; i < fullPeriods * fullSamplePeriods && i < samples.size(); i++) {
            fullPeriodSamples.add(samples.get(i));
        }

        return fullPeriodSamples;
    }
}