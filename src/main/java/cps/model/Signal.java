package cps.model;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.*;

@Data
@AllArgsConstructor
@SuperBuilder
public class Signal {
    double amplitude;
    double startTime;
    double durationTime;

    List<Double> samples;
    SignalType signalType;

    public List<Double> getSamplesForCalculation() {
        return samples;
    }

    public double calculateMean() {
        List<Double> samplesForCalc = getSamplesForCalculation();
        if (samplesForCalc.isEmpty()) return 0;

        double sum = 0;
        for (Double sample : samplesForCalc) {
            sum += sample;
        }
        return sum / samplesForCalc.size();
    }

    public double calculateAbsoluteMean() {
        List<Double> samplesForCalc = getSamplesForCalculation();
        if (samplesForCalc.isEmpty()) return 0;

        double sum = 0;
        for (Double sample : samplesForCalc) {
            sum += Math.abs(sample);
        }
        return sum / samplesForCalc.size();
    }

    public double calculateRMS() {
        List<Double> samplesForCalc = getSamplesForCalculation();
        if (samplesForCalc.isEmpty()) return 0;

        double sumOfSquares = 0;
        for (Double sample : samplesForCalc) {
            sumOfSquares += sample * sample;
        }
        return Math.sqrt(sumOfSquares / samplesForCalc.size());
    }

    public double calculateVariance() {
        List<Double> samplesForCalc = getSamplesForCalculation();
        if (samplesForCalc.isEmpty()) return 0;

        double mean = calculateMean();
        double sumOfSquaredDifferences = 0;
        for (Double sample : samplesForCalc) {
            double difference = sample - mean;
            sumOfSquaredDifferences += difference * difference;
        }
        return sumOfSquaredDifferences / samplesForCalc.size();
    }

    // Obliczenie mocy średniej
    public double calculateAveragePower() {
        // Dla większości sygnałów, moc średnia = wartość skuteczna do kwadratu
        double rms = calculateRMS();
        return rms * rms;
    }

    // Metoda pomocnicza zwracająca wszystkie obliczone parametry jako mapa
    public Map<String, Double> calculateAllParameters() {
        Map<String, Double> parameters = new HashMap<>();
        parameters.put("Mean", calculateMean());
        parameters.put("Absolute Mean", calculateAbsoluteMean());
        parameters.put("RMS", calculateRMS());
        parameters.put("Variance", calculateVariance());
        parameters.put("Average Power", calculateAveragePower());
        return parameters;
    }

    public Map<String, Integer> createHistogramData(int numBins) {
        List<Double> samplesForCalc = getSamplesForCalculation();
        if (samplesForCalc.isEmpty()) return new HashMap<>();

        // Znajdź min i max wartość w próbkach
        double min = samplesForCalc.stream().min(Double::compare).orElse(0.0);
        double max = samplesForCalc.stream().max(Double::compare).orElse(0.0);

        // Dodaj małą wartość do max aby upewnić się, że największa wartość będzie wewnątrz przedziału
        max += 0.000001;

        // Oblicz szerokość przedziału
        double binWidth = (max - min) / numBins;

        // Przygotuj mapę przedziałów (jako string) do liczby wystąpień
        Map<String, Integer> histogramData = new LinkedHashMap<>(); // LinkedHashMap zachowa kolejność

        // Inicjalizuj wszystkie przedziały z zerami
        for (int i = 0; i < numBins; i++) {
            double lowerBound = min + i * binWidth;
            double upperBound = min + (i + 1) * binWidth;
            String binLabel = String.format("%.2f - %.2f", lowerBound, upperBound);
            histogramData.put(binLabel, 0);
        }

        // Zlicz wystąpienia dla każdego przedziału
        for (Double sample : samplesForCalc) {
            int binIndex = (int)Math.floor((sample - min) / binWidth);
            // Upewnij się, że wartość maksymalna trafia do ostatniego przedziału
            if (binIndex == numBins) binIndex--;

            String binLabel = String.format("%.2f - %.2f",
                    min + binIndex * binWidth,
                    min + (binIndex + 1) * binWidth);

            histogramData.put(binLabel, histogramData.getOrDefault(binLabel, 0) + 1);
        }

        return histogramData;
    }

}