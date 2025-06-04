package cps.model;

import cps.dto.FiltrationDto;
import cps.model.signals.Signal;
import cps.model.signals.SignalType;

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public class SignalOperations {
    
    private static final Logger logger = Logger.getLogger(String.valueOf(SignalOperations.class));

    private SignalOperations() {}

    // ============= BASE OPERATIONS =============

    public static Signal sum(List<Signal> signals) {
        return operation(signals, "sum");
    }

    public static Signal difference(List<Signal> signals) {
        return operation(signals, "difference");
    }

    public static Signal multiply(List<Signal> signals) {
        return operation(signals, "multiply");
    }

    public static Signal divide(List<Signal> signals) {
        return operation(signals, "divide");
    }

    private static Signal operation(List<Signal> signals, String operation) {
        LinkedHashMap<Double, Double> timestampSamples = new LinkedHashMap<>();

        for (Signal signal : signals) {
            for (Map.Entry<Double, Double> entry : signal.getTimestampSamples().entrySet()) {
                double value = switch (operation) {
                    case "sum" -> timestampSamples.getOrDefault(entry.getKey(), 0.0) + entry.getValue();
                    case "difference" -> timestampSamples.getOrDefault(entry.getKey(), 0.0) - entry.getValue();
                    case "multiply" -> timestampSamples.getOrDefault(entry.getKey(), 0.0) * entry.getValue();
                    case "divide" -> {
                        double temp = entry.getValue();
                        if (temp == 0.0) {
                            yield 0.0;
                        }
                        yield timestampSamples.get(entry.getKey()) / temp;
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + operation);
                };

                timestampSamples.put(entry.getKey(), value);
            }
        }

        return Signal.builder()
                .signalType(SignalType.CUSTOM)
                .timestampSamples(timestampSamples)
                .build();
    }

    // ============= CONVOLUTION & CORRELATION =============

    /**
     * Implementation of convolution operation on discrete sets of numbers. Algorithm is
     * based on 'Input-side algorithm' with O(s1.size() * s2.size()) complexity.
     * Equation: (h * x)(n) = sum {h(k) * x(n - k)}
     *
     * @param signal1 List of 1st signal's values
     * @param signal2 List of 2nd signal's values
     * @return Product of convolution
     */
    public static List<Double> convolve(List<Double> signal1, List<Double> signal2) {
        double[] s1 = signal1.stream().mapToDouble(Double::doubleValue).toArray();
        double[] s2 = signal2.stream().mapToDouble(Double::doubleValue).toArray();

        int productSize = s1.length + s2.length - 1;
        double[] product = new double[productSize];

        for (int k = 0; k < s1.length; k++) {
            double s1value = s1[k];
            for (int i = 0; i < s2.length; i++) {
                product[k + i] += s1value * s2[i];
            }
        }
        return DoubleStream.of(product).boxed().toList();
    }

    public static List<Double> crossCorrelate(List<Double> s1, List<Double> s2) {
        logger.fine("s1: " + s1);
        logger.fine("s2: " + s2);

        s2 = s2.reversed();

        int productSize = s1.size() + s2.size() - 1;
        List<Double> product = new ArrayList<>(Collections.nCopies(productSize, 0.0));

        for (int k = 0; k < s1.size(); k++) {
            for (int i = s2.size() - 1; i >= 0; i--) {
                double s1value = s1.get(k);
                double s2value = s2.get(i);
                double value = product.get(k + i) + s1value * s2value;
                product.set(k + i, value);
                logger.fine("k: %s | i: %s | product: %s".formatted(k, i, product));
            }
        }

        logger.fine("product: " + product);
        return product;
    }

    public static Signal crossCorrelateSignal(Signal signal1, Signal signal2) {
        List<Double> correlationProduct = crossCorrelate(
                signal1.getSamples(),
                signal2.getSamples()
        );

        Map<Double, Double> timeStampSamples = new LinkedHashMap<>();
        double startTime = signal1.getStartTime();
        double timeStep = signal1.getTimeStep();

        for (int i = 0; i < correlationProduct.size(); i++) {
            timeStampSamples.put(startTime + i * timeStep, correlationProduct.get(i));
        }

        return SignalFactory.createSignal(timeStampSamples);
    }

    /**
     * Low pass filtration on discrete set using FIR.
     * @param signal Signal object containing discrete set of samples.
     * @param M No. of coefficients in FIR.
     * @param cutoffFrequency Cut-off frequency of filter, required smaller than half of sampling frequency.
     * @return Signal object with filtered samples.
     */
    public static FiltrationDto lowPassFIRFiltration(Signal signal, int M, double cutoffFrequency) {
        return firFiltration(signal, M, cutoffFrequency, coefficients -> coefficients);
    }

    /**
     * High pass filtration on discrete set using FIR. Difference to low pass
     * is that coefficients are modified by multiplying them by s(n) = (-1)^n.
     * @param signal Signal object containing discrete set of samples.
     * @param M No. of coefficients in FIR.
     * @param cutoffFrequency Cut-off frequency of filter, required smaller than half of sampling frequency.
     * @return Signal object with filtered samples.
     */
    public static FiltrationDto highPassFIRFiltration(Signal signal, int M, double cutoffFrequency) {
        return firFiltration(signal, M, cutoffFrequency, coefficients -> {
            double[] coefficientsArray = coefficients.stream().mapToDouble(Double::doubleValue).toArray();
            double[] modifiedCoefficientsArray = new double[coefficientsArray.length];
            for (int n = 0; n < coefficientsArray.length; n++) {
                double coefficient = (n % 2) == 0 ? coefficientsArray[n] : coefficientsArray[n] * (-1);
                modifiedCoefficientsArray[n] = coefficient;
            }
            return DoubleStream.of(modifiedCoefficientsArray).boxed().toList();
        });
    }

    /**
     * Implementation of filter on discrete set using FIR filter (pl. SOI).
     * Filtration product is a convolution of signal and filter.
     * Filtered samples are cut to the size of original signal.
     * @param signal Signal object containing discrete set of samples.
     * @param M No. of coefficients in FIR.
     * @param cutoffFrequency Cut-off frequency of filter, required smaller than half of sampling frequency.
     * @return New signal object containing filtered samples.
     */
    private static FiltrationDto firFiltration(Signal signal, int M, double cutoffFrequency,
                                        UnaryOperator<List<Double>> coefficientsModifier) {
        List<Double> timestamps = signal.getTimestamps();
        List<Double> samples = signal.getSamples();

        double samplingFrequency = timestamps.size() / signal.getDurationTime();
        double K = samplingFrequency / cutoffFrequency;
        List<Double> coefficients = createFIRCoefficients(M, K);

        List<Double> modifiedCoefficients = coefficientsModifier.apply(coefficients);

        List<Double> product = convolve(samples, modifiedCoefficients);

        LinkedHashMap<Double, Double> filteredSamples = new LinkedHashMap<>();
        int shift = (M - 1) / 2;
        for (int i = 0; i < timestamps.size(); i++) {
            filteredSamples.put(timestamps.get(i), product.get(i + shift));
        }

        // coefficients to discrete signal
        Map<Double, Double> coefficientsSamples = new LinkedHashMap<>();
        for (int i = 0; i < modifiedCoefficients.size(); i++) {
            coefficientsSamples.put((double) i, modifiedCoefficients.get(i));
        }

        return FiltrationDto.builder()
                .filteredSignal(SignalFactory.createSignal(filteredSamples))
                .coefficients(SignalFactory.createSignal(coefficientsSamples))
                .build();
    }

    /**
     * Creates coefficients for FIR filter. Value of each is product of sinc function and given window.
     * @param M No. of coefficients in FIR.
     * @param K Relation between sampling frequency and cut-off frequency.
     * @return List of coefficients.
     */
    public static List<Double> createFIRCoefficients(int M, double K) {
        List<Double> coefficients = new ArrayList<>();
        for (int i = 0; i < M; i++) {
            double value = coefficient(i, M, K) * hammingWindow(i, M);
            coefficients.add(value);
        }
        return coefficients;
    }

    /**
     * Helper method to calculate coefficient at given index.
     * @param n No. of current sample.
     * @param M No. of coefficients in FIR.
     * @param K Relation between sampling frequency and cut-off frequency.
     * @return FIR coefficient at index n.
     */
    private static double coefficient(int n, int M, double K) {
        double state = (M - 1) / 2.0;

        if (n == state) {
            return 2 / K;
        }

        double numerator = Math.sin(2 * Math.PI * (n - state) / K);
        double denominator = Math.PI * (n - state);
        return numerator / denominator;
    }

    /**
     * Implementation of Hamming window. Calculates value at given index.
     * @param n No. of current sample
     * @param M No. of coefficients in FIR
     * @return Value of Hamming window.
     */
    public static double hammingWindow(int n, int M) {
        return 0.53836 - 0.46164 * Math.cos(2 * Math.PI * n / M);
    }
}
