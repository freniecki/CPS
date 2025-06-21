package cps.model;

import cps.dto.FiltrationDto;
import cps.model.signals.Complex;
import cps.model.signals.Signal;
import cps.model.signals.SignalType;

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.logging.Logger;
import java.util.stream.DoubleStream;

public class SignalOperations {

    private static final Logger logger = Logger.getLogger(String.valueOf(SignalOperations.class));

    private SignalOperations() {
    }

    // =====================================================
    // ================== BASE OPERATIONS ==================
    // =====================================================

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

        double startTime = signals.stream().mapToDouble(Signal::getStartTime).min().orElse(0.0);
        double durationTime = signals.stream().mapToDouble(Signal::getDurationTime).max().orElse(0.0);

        return Signal.builder()
                .signalType(SignalType.CUSTOM)
                .startTime(startTime)
                .durationTime(durationTime)
                .timestampSamples(timestampSamples)
                .build();
    }

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

    // =====================================================
    // ================== TRANSFORMATIONS ==================
    // =====================================================

    public static Complex[] dft(double[] samples, int log2N) {
        Complex[] complexSamples = createComplexSamples(samples);
        return dft(complexSamples, log2N);
    }

    /**
     * Calculation of Discrete Fourier Transform on real numbers. In the core we calculate value based on Euler's formula.
     *
     * @param samples List of samples, being real number.
     * @param log2N   Number of bits.
     * @return List of calculated product of DFT being imaginary numbers.
     */
    public static Complex[] dft(Complex[] samples, int log2N) {
        int N = 1 << log2N;

        Complex[] transformedSamples = new Complex[N];

        for (int m = 0; m < N; m++) {
            double real = 0.0;
            double imag = 0.0;

            for (int n = 0; n < N; n++) {
                double omega = 2 * Math.PI * m * n / N;
                real += samples[n].real() * Math.cos(omega);
                imag -= samples[n].real() * Math.sin(omega);
            }
            transformedSamples[m] = new Complex(real / N, imag / N);
        }

        return transformedSamples;
    }

    public static Complex[] fftDIF(double[] samples, int log2N) {
        Complex[] complexSamples = createComplexSamples(samples);
        return fftDIF(complexSamples, log2N);
    }

    public static Complex[] fftDIF(Complex[] complexSamples, int log2N) {
        int N = 1 << log2N;

        flipBits(complexSamples, log2N);

        for (int iteration = 0; iteration < log2N; iteration++) { // dla każdej transformaty zaczynając od 2 elementowej
            int step = 1 << iteration; // odległość między kolejnymi elementami w transformacie do operacji motylkowej
            int size = step << 1; // rozmiar transformaty

            for (int i = 0; i < N; i += size) { // od pierwszego elementu z każdej transformaty
                for (int j = 0; j < step; j++) { // po każdym elemencie w transformacie
                    int m = i + j; // indeks elementu w wynikowej tablicy

                    double omega = 2 * Math.PI * j / size;
                    Complex W = new Complex(Math.cos(omega), -Math.sin(omega));
                    Complex first = complexSamples[m].copy();
                    Complex second = complexSamples[m + step].copy();

                    complexSamples[m] = first.plus(second).times(W);
                    complexSamples[m + step] = first.minus(second).times(W);
                }
            }
        }

        return complexSamples;
    }

    private static Complex[] createComplexSamples(double[] samples) {
        Complex[] complexSamples = new Complex[samples.length];
        for (int i = 0; i < samples.length; i++) {
            complexSamples[i] = new Complex(samples[i], 0);
        }
        return complexSamples;
    }

    /**
     * Flips samples in array by symmetric flip of index's bits.
     * @param samples Array of samples to be sorted.
     * @param log2N No. of bits
     * @return Flipped array.
     */
    public static Complex[] flipBits(Complex[] samples, int log2N) {
        for (int i = 0; i < samples.length; i++) {
            int flippedBit = reverseBits(i, log2N);
            if (i < flippedBit) {
                Complex temp = samples[i];
                samples[i] = samples[flippedBit];
                samples[flippedBit] = temp;
            }
        }
        return samples;
    }

    /**
     * Flips binary representation by middle symmetry.
     * @param i number to flip
     * @param log2N no. of bits
     * @return flipped number
     */
    private static int reverseBits(int i, int log2N) {
        int reversed = 0;
        for (int bit = 0; bit < log2N; bit++) {
            reversed <<= 1;
            reversed |= (i & 1);
            i >>= 1;
        }
        return reversed;
    }

    private static Complex[] createW(int N2) {
        Complex[] W = new Complex[N2];
        double omega;
        for (int n = 0; n < N2; n++) {
            omega = 2 * Math.PI * n / N2;
            W[n] = new Complex(Math.cos(omega), Math.sin(omega));
        }
        return W;
    }

    // ==== COSINE TRANSFORMATION ====

    /**
     * Applies cosinus transformation on samples. T: R -> R, so no use of complex numbers.
     * @param samples Array of samples to be transformed.
     * @return New array containing transformed samples.
     */
    public static double[] dctII(double[] samples) {
        int N = samples.length;
        double c0 = Math.pow(N, -0.5);
        double cm = c0 * Math.pow(2, 0.5);

        double[] result = new double[N];
        for (int m = 0; m < N; m++) {
            double sum = 0.0;
            double omega;

            for (int n = 0; n < N; n++) {
                omega = Math.PI * (2 * n + 1) * m / (2 * N);
                sum += samples[n] * Math.cos(omega);
            }

            result[m] = m == 0 ? sum * c0 : sum * cm;
        }
        return result;
    }

    public static double[] fctII(double[] samples) {
        int N = samples.length;
        if ((N & (N - 1)) != 0) {
            logger.warning("Samples must be a power of 2");
            throw new IllegalArgumentException("Samples must be a power of 2");
        }

        double c0 = Math.sqrt(1.0 / N);
        double cm = Math.sqrt(2.0 / N);

        // 1. samples flip
        samples = fctFlip(samples);

        // 2. calculate fft of samples
        int log2N = (int)(Math.log(N) / Math.log(2));
        Complex[] fftResult = fftDIF(samples, log2N);

        // 3. for every find real value of operation
        double[] result = new double[N];
        for (int m = 0; m < N; m++) {
            double sigma = Math.PI * m / (2 * N);
            double value = fftResult[m].real() * Math.cos(sigma)
                    + fftResult[m].imaginary() * Math.sin(sigma);
            result[m] = m == 0 ? value * c0 : value * cm;
        }

        return result;
    }

    private static double[] fctFlip(double[] samples) {
        int N = samples.length;
        double[] result = new double[N];
        for (int i = 0; i < N / 2; i++) {
            result[2 * i] = samples[i];
            result[2 * i + 1] = samples[N - 1 - i];
        }
        return result;
    }

    // =====================================================
    // ==================== CORRELATION ====================
    // =====================================================

    public static List<Double> crossCorrelate(List<Double> samples1, List<Double> samples2) {
        double[] s1 = samples1.stream().mapToDouble(Double::doubleValue).toArray();
        double[] s2 = samples2.reversed().stream().mapToDouble(Double::doubleValue).toArray();
        logger.fine("s1: " + Arrays.toString(s1));
        logger.fine("s2: " + Arrays.toString(s2));

        int productSize = s1.length + s2.length - 1;
        double[] product = new double[productSize];

        for (int k = 0; k < s1.length; k++) {
            double s1value = s1[k];
            for (int i = s2.length - 1; i >= 0; i--) {
                double s2value = s2[i];
                double value = product[k + i] + s1value * s2value;
                product[k + i] = value;
            }
        }

        logger.fine("product: " + Arrays.toString(product));
        return DoubleStream.of(product).boxed().toList();
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

    // =====================================================
    // ==================== CONVOLUTION ====================
    // =====================================================

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
                double s2value = s2[i];
                double value = product[k + i] + s1value * s2value;
                product[k + i] = value;
            }
        }
        return DoubleStream.of(product).boxed().toList();
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
        cutoffFrequency = getHighPassCutoffFrequency(signal, cutoffFrequency);

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

    private static double getHighPassCutoffFrequency(Signal signal, double cutoffFrequency) {
        double samplingFrequency = signal.getTimestampSamples().size() / signal.getDurationTime();
        if (cutoffFrequency >= samplingFrequency / 2) {
            throw new IllegalArgumentException("Cut-off frequency must be smaller than half of sampling frequency.");
        }

        cutoffFrequency = samplingFrequency / 2 - cutoffFrequency;
        return cutoffFrequency;
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
        logger.info("K: %s | M: %s | fs: %s | fc: %s".formatted(K, M, samplingFrequency, cutoffFrequency));

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

        Signal filteredSignal = SignalFactory.createSignal(filteredSamples);
        filteredSignal.setName("filtered");

        Signal coefficientsSignal = SignalFactory.createSignal(coefficientsSamples);
        coefficientsSignal.setName("coefficients");

        return FiltrationDto.builder()
                .filteredSignal(filteredSignal)
                .coefficients(coefficientsSignal)
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
        double omega = 2 * Math.PI / K;

        if (n == state) {
            return 2 / K;
        }

        double numerator = Math.sin(omega * (n - state));
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
