package cps.model;

import java.util.*;
import java.util.logging.Logger;

public class StatisticTool {
    private static final Logger logger = Logger.getLogger(StatisticTool.class.getName());
    private static final String MISSING_BASE_SAMPLE = "missing base sample";

    private StatisticTool() {}

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

    /* ------------ GENERAL ------------ */

    public static double getSquaredSum(List<Double> samples) {
        return samples.stream().mapToDouble(sample -> sample * sample).sum();
    }

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

    /* ------------ SIGNAL SPECIFIC ------------ */

    public static double getMSE(Map<Double, Double> baseSamples, Map<Double, Double> samples) {
        double sum = 0;
        for (Map.Entry<Double, Double> sample : samples.entrySet()) {
            if (!baseSamples.containsKey(sample.getKey())) {
                logger.warning(MISSING_BASE_SAMPLE);
                continue;
            }
            sum += Math.pow(sample.getValue() - baseSamples.get(sample.getKey()), 2);
        }
        return Math.sqrt(sum / samples.size());
    }

    public static double getSNR(Map<Double, Double> baseSamples, Map<Double, Double> samples) {
        double upperSum = 0;
        double lowerSum = 0;
        for (Map.Entry<Double, Double> sample : samples.entrySet()) {
            if (!baseSamples.containsKey(sample.getKey())) {
                logger.warning(MISSING_BASE_SAMPLE);
                continue;
            }
            upperSum += Math.pow(sample.getValue(), 2);
            lowerSum += Math.pow(sample.getValue() - baseSamples.get(sample.getKey()), 2);
        }

        if (lowerSum == 0) {
            logger.warning("lower sum is zero");
            return Double.POSITIVE_INFINITY;
        }

        return 10 * Math.log10(upperSum / lowerSum);
    }

    public static double getPSNR(Map<Double, Double> baseSamples, Map<Double, Double> samples) {
        return 10 * Math.log10(Collections.max(samples.values()) / StatisticTool.getMSE(baseSamples, samples));
    }

    public static double getENOB(Map<Double, Double> baseSamples, Map<Double, Double> samples) {
        return (StatisticTool.getSNR(baseSamples, samples) - 1.76) / 6.02;
    }

    public static double getMD(Map<Double, Double> baseSamples, Map<Double, Double> samples) {
        List<Double> differences = new ArrayList<>();
        for (Map.Entry<Double, Double> sample : samples.entrySet()) {
            if (!baseSamples.containsKey(sample.getKey())) {
                logger.warning(MISSING_BASE_SAMPLE);
                continue;
            }
            differences.add(Math.abs(sample.getValue() - baseSamples.get(sample.getKey())));
        }
        return Collections.max(differences);
    }
}
