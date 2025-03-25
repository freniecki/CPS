package cps.model;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StatisticTool {
    private StatisticTool() {}

    public static double getMean(List<Double> samples) {
        if (samples.isEmpty()) {
            throw new IllegalArgumentException("empty sample list");
        }

        return samples.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    public static double getAbsoluteMean(List<Double> samples) {
        if (samples.isEmpty()) {
            throw new IllegalArgumentException("empty sample list");
        }
        return samples.stream().mapToDouble(Math::abs).average().orElse(0.0);
    }

    public static double getAveragePower(List<Double> samples) {
        return samples.stream().mapToDouble(sample -> sample * sample).average().orElse(0.0);
    }

    public static double getVariance(List<Double> samples) {
        double mean = getMean(samples);

        return samples.stream().mapToDouble(sample -> sample - mean).map(sample -> sample * sample).average().orElse(0.0);
    }

    public static double getRMS(List<Double> samples) {
        return Math.sqrt(getAveragePower(samples));
    }

    public static Map<String, Double> getStatistics(Map<Double, Double> samples) {
        Map<String, Double> stats = new HashMap<>();
        stats.put("mean", StatisticTool.getMean(samples.values().stream().toList()));
        stats.put("absMean", StatisticTool.getAbsoluteMean(samples.values().stream().toList()));
        stats.put("avgPower", StatisticTool.getAveragePower(samples.values().stream().toList()));
        stats.put("variance", StatisticTool.getVariance(samples.values().stream().toList()));
        stats.put("rms", StatisticTool.getRMS(samples.values().stream().toList()));
        return stats;
    }

    public static Map<String, Integer> createHistogramData(int numBins, List<Double> samples) {
        if (samples.isEmpty()) return new HashMap<>();

        double min = samples.stream().min(Double::compare).orElse(0.0);
        double max = samples.stream().max(Double::compare).orElse(0.0);

        max += 0.000001;

        double binWidth = (max - min) / numBins;

        Map<String, Integer> histogramData = new LinkedHashMap<>();

        for (int i = 0; i < numBins; i++) {
            double lowerBound = min + i * binWidth;
            double upperBound = min + (i + 1) * binWidth;
            String binLabel = String.format("%.2f - %.2f", lowerBound, upperBound);
            histogramData.put(binLabel, 0);
        }

        for (Double sample : samples) {
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
