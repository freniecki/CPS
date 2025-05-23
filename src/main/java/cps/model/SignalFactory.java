package cps.model;

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.logging.Logger;

public class SignalFactory {
    private static final Logger logger = Logger.getLogger(String.valueOf(SignalFactory.class));
    private static final Random random = new Random();
    private static double sampleStep = 0.01;
    private static int quantizationBits = 8;
    private static String quantizationType = "none";

    private static final String PARAM_NO_TYPE_ERROR = "No. of param no equal to type: ";

    private SignalFactory() {
    }

    public static void setSampleStep(double sampleStep) {
        SignalFactory.sampleStep = sampleStep;
    }

    public static Signal createSignal(SignalType type, Map<String, String> params) {
        List<Double> retrievedParams = params.values().stream().map(Double::parseDouble).toList();
        double amplitude = retrievedParams.getFirst();
        double startTime = retrievedParams.get(1);
        double durationTime = retrievedParams.get(2);

        if (retrievedParams.size() == 3) {
            return switch (type) {
                case UNIFORM_NOISE -> createUniformNoise(amplitude, startTime, durationTime);
                case GAUSS_NOISE -> createGaussNoise(amplitude, startTime, durationTime);
                default -> throw new IllegalArgumentException(PARAM_NO_TYPE_ERROR + type);
            };
        } else if (retrievedParams.size() == 4) {
            double period = retrievedParams.get(3);
            return switch (type) {
                case SINE -> createSineSignal(amplitude, startTime, durationTime, period);
                case SINE_HALF -> createSineHalfSignal(amplitude, startTime, durationTime, period);
                case SINE_FULL -> createSineFullSignal(amplitude, startTime, durationTime, period);
                case UNIT_STEP -> createUnitStepSignal(amplitude, startTime, durationTime, period);
                default -> throw new IllegalArgumentException(PARAM_NO_TYPE_ERROR + type);
            };
        } else if (retrievedParams.size() == 5) {
            double period = retrievedParams.get(3);
            // fifth param is one of [duty cycle, impulseTime, probability]
            double fifthParam = retrievedParams.get(4);
            return switch (type) {
                case RECTANGLE -> createRectangleSignal(amplitude, startTime, durationTime, period, fifthParam);
                case RECTANGLE_SYMETRIC -> createRectangleSymmetricSignal(amplitude, startTime, durationTime, period, fifthParam);
                case TRIANGLE -> createTriangleSignal(amplitude, startTime, durationTime, period, fifthParam);
                case UNIT_IMPULS -> createUnitImpulseSignal(amplitude, startTime, durationTime, period, fifthParam);
                case IMPULSE_NOISE -> createImpulseNoiseSignal(amplitude, startTime, durationTime, period, fifthParam);
                default -> throw new IllegalArgumentException(PARAM_NO_TYPE_ERROR + type);
            };
        } else {
            throw new IllegalArgumentException(PARAM_NO_TYPE_ERROR + type);
        }
    }

    public static Signal createSignal(Map<Double, Double> timeStampSamples) {
        if (timeStampSamples.isEmpty()) {
            throw new IllegalArgumentException("empty samples");
        }

        List<Double> timeStamps = new ArrayList<>(timeStampSamples.keySet());
        Collections.sort(timeStamps);

        double startTime = timeStamps.getFirst();
        double durationTime = timeStamps.getLast() - startTime;

        LinkedHashMap<Double, Double> samples = new LinkedHashMap<>();
        for (double timeStamp : timeStamps) {
            samples.putLast(timeStamp, timeStampSamples.get(timeStamp));
        }

        return Signal.builder()
                .startTime(startTime)
                .durationTime(durationTime)
                .timestampSamples(quantizeSamples(samples))
                .signalType(SignalType.CUSTOM)
                .build();
    }

    public static Signal createSignalWithQuantization(SignalType type, Map<String, String> params, int quantizationBits, String quantizationType) {
        SignalFactory.quantizationBits = quantizationBits;
        SignalFactory.quantizationType = quantizationType;

        if (params.size() < 3 || params.size() > 5) {
            throw new IllegalArgumentException("retrieved params list size incorrect: " + params.size());
        }

        return createSignal(type, params);
    }

    public static Signal createSignalWithQuantization(Signal signal, int quantizationBits, String quantizationType) {
        Map<Double, Double> timeStampSamples = signal.getTimestampSamples();

        SignalFactory.quantizationBits = quantizationBits;
        SignalFactory.quantizationType = quantizationType;

        return createSignal(timeStampSamples);
    }

    public static Signal createNewFromSamplingExistingOne(Signal signal, double sampleRate) {
        LinkedHashMap<Double, Double> samples = new LinkedHashMap<>();
        for (double timestamp = signal.getStartTime(); timestamp < signal.getStartTime() + signal.getDurationTime(); timestamp += sampleRate) {
            samples.putLast(timestamp, signal.getTimestampSamples().get(timestamp));
        }

        return Signal.builder()
                .amplitude(signal.getAmplitude())
                .startTime(signal.getStartTime())
                .durationTime(signal.getDurationTime())
                .timestampSamples(quantizeSamples(samples))
                .signalType(signal.getSignalType())
                .build();
    }

    // ======== CONTINUOUS SIGNALS ========

    private static Signal createUniformNoise(double amplitude, double startTime, double durationTime) {
        LinkedHashMap<Double, Double> samples = new LinkedHashMap<>();
        for (double timestamp = startTime; timestamp < startTime + durationTime; timestamp += sampleStep) {
            samples.putLast(timestamp, getUniformValue(amplitude));
        }

        return Signal.builder()
                .amplitude(amplitude)
                .startTime(startTime)
                .durationTime(durationTime)
                .timestampSamples(quantizeSamples(samples))
                .signalType(SignalType.UNIFORM_NOISE)
                .build();
    }

    private static Signal createGaussNoise(double amplitude, double startTime, double durationTime) {
        LinkedHashMap<Double, Double> samples = new LinkedHashMap<>();
        for (double timestamp = startTime; timestamp < startTime + durationTime; timestamp += sampleStep) {
            samples.putLast(timestamp,random.nextGaussian() * amplitude);
        }

        return Signal.builder()
                .amplitude(amplitude)
                .startTime(startTime)
                .durationTime(durationTime)
                .timestampSamples(quantizeSamples(samples))
                .signalType(SignalType.GAUSS_NOISE)
                .build();
    }

    private static Signal createSineSignal(double amplitude, double startTime, double durationTime, double period) {
        LinkedHashMap<Double, Double> samples = new LinkedHashMap<>();
        for (double timestamp = startTime; timestamp <= startTime + durationTime; timestamp += sampleStep) {
            samples.putLast(timestamp, amplitude * Math.sin(2 * Math.PI / period * timestamp));
        }

        return PeriodicSignal.builder()
                .amplitude(amplitude)
                .startTime(startTime)
                .durationTime(durationTime)
                .timestampSamples(quantizeSamples(samples))
                .signalType(SignalType.SINE)
                .period(period)
                .build();
    }

    private static Signal createSineHalfSignal(double amplitude, double startTime, double durationTime, double period) {
        LinkedHashMap<Double, Double> samples = new LinkedHashMap<>();
        for (double timestamp = startTime; timestamp < startTime + durationTime; timestamp += sampleStep) {
            double sinValue = Math.sin(2 * Math.PI / period * (timestamp - startTime));
            double halfRectified = 0.5 * amplitude * (sinValue + Math.abs(sinValue));
            samples.putLast(timestamp, halfRectified);
        }

        return PeriodicSignal.builder()
                .amplitude(amplitude)
                .startTime(startTime)
                .durationTime(durationTime)
                .timestampSamples(quantizeSamples(samples))
                .signalType(SignalType.SINE_HALF)
                .period(period)
                .build();
    }

    private static Signal createSineFullSignal(double amplitude, double startTime, double durationTime, double period) {
        LinkedHashMap<Double, Double> samples = new LinkedHashMap<>();
        for (double timestamp = startTime; timestamp < startTime + durationTime; timestamp += sampleStep) {
            // Formula: x(t) = A * |sin(2π/T(t-t₁))|
            double absValue = Math.abs(Math.sin(2 * Math.PI / period * (timestamp - startTime)));
            samples.putLast(timestamp, amplitude * absValue);
        }

        return PeriodicSignal.builder()
                .amplitude(amplitude)
                .startTime(startTime)
                .durationTime(durationTime)
                .timestampSamples(quantizeSamples(samples))
                .signalType(SignalType.SINE_FULL)
                .period(period)
                .build();
    }

    private static Signal createRectangleSignal(double amplitude, double startTime, double durationTime, double period, double dutyCycle) {
        LinkedHashMap<Double, Double> samples = new LinkedHashMap<>();
        for (double timestamp = startTime; timestamp < startTime + durationTime; timestamp += sampleStep) {
            double time = (timestamp - startTime) % period;
            double value = (time < dutyCycle * period) ? amplitude : 0;
            samples.putLast(timestamp, value);
        }

        return PolygonalSignal.builder()
                .amplitude(amplitude)
                .startTime(startTime)
                .durationTime(durationTime)
                .timestampSamples(quantizeSamples(samples))
                .signalType(SignalType.RECTANGLE)
                .period(period)
                .dutyCycle(dutyCycle)
                .build();
    }

    private static Signal createRectangleSymmetricSignal(double amplitude, double startTime, double durationTime, double period, double dutyCycle) {
        LinkedHashMap<Double, Double> samples = new LinkedHashMap<>();
        for (double timestamp = startTime; timestamp < startTime + durationTime; timestamp += sampleStep) {
            double time = (timestamp - startTime) % period;
            double value = (time < dutyCycle * period) ? amplitude : -amplitude;
            samples.putLast(timestamp, value);
        }

        return PolygonalSignal.builder()
                .amplitude(amplitude)
                .startTime(startTime)
                .durationTime(durationTime)
                .timestampSamples(quantizeSamples(samples))
                .signalType(SignalType.RECTANGLE_SYMETRIC)
                .period(period)
                .dutyCycle(dutyCycle)
                .build();
    }

    private static Signal createTriangleSignal(double amplitude, double startTime, double durationTime, double period, double dutyCycle) {
        LinkedHashMap<Double, Double> samples = new LinkedHashMap<>();
        for (double timestamp = startTime; timestamp < startTime + durationTime; timestamp += sampleStep) {
            double time = (timestamp - startTime) % period;
            double value;

            if (time < dutyCycle * period) {
                // Rising edge
                value = (time / (dutyCycle * period)) * amplitude;
            } else {
                // Falling edge
                value = amplitude - ((time - dutyCycle * period) / ((1 - dutyCycle) * period)) * amplitude;
            }

            samples.putLast(timestamp, value);
        }

        return PolygonalSignal.builder()
                .amplitude(amplitude)
                .startTime(startTime)
                .durationTime(durationTime)
                .timestampSamples(quantizeSamples(samples))
                .signalType(SignalType.TRIANGLE)
                .period(period)
                .dutyCycle(dutyCycle)
                .build();
    }

    private static Signal createUnitStepSignal(double amplitude, double startTime, double durationTime, double stepTime) {
        LinkedHashMap<Double, Double> samples = new LinkedHashMap<>();
        for (double timestamp = startTime; timestamp < startTime + durationTime; timestamp += sampleStep) {
            if (timestamp < stepTime) {
                samples.putLast(timestamp, 0.0);
            } else if (timestamp == stepTime) {
                samples.putLast(timestamp, amplitude / 2);
            } else {
                samples.putLast(timestamp, amplitude);
            }
        }

        return Signal.builder()
                .amplitude(amplitude)
                .startTime(startTime)
                .durationTime(durationTime)
                .timestampSamples(quantizeSamples(samples))
                .signalType(SignalType.UNIT_STEP)
                .build();
    }

    // ======== DISCRETE SIGNALS ========

    private static Signal createUnitImpulseSignal(double amplitude, double startTime, double durationTime, double period, double impulseTime) {
        LinkedHashMap<Double, Double> samples = new LinkedHashMap<>();
        for (double timestamp = startTime; timestamp < startTime + durationTime; timestamp += period) {
            double value = (timestamp == impulseTime) ? amplitude : 0;
            samples.putLast(timestamp, value);
        }

        return Signal.builder()
                .amplitude(amplitude)
                .startTime(startTime)
                .durationTime(durationTime)
                .timestampSamples(quantizeSamples(samples))
                .signalType(SignalType.UNIT_IMPULS)
                .build();
    }

    private static Signal createImpulseNoiseSignal(double amplitude, double startTime, double durationTime, double period, double probability) {
        LinkedHashMap<Double, Double> samples = new LinkedHashMap<>();
        for (double timestamp = startTime; timestamp < startTime + durationTime; timestamp += period) {
            double value = (random.nextDouble() < probability) ? amplitude : 0;
            samples.putLast(timestamp, value);
        }

        return Signal.builder()
                .amplitude(amplitude)
                .startTime(startTime)
                .durationTime(durationTime)
                .timestampSamples(quantizeSamples(samples))
                .signalType(SignalType.IMPULSE_NOISE)
                .build();
    }

    // ======== TOOLS ========

    private static double quantize(double value, double min, double max) {
        int levels = (int) Math.pow(2, quantizationBits);
        double step = (max - min) / (levels - 1);

        double quantized;
        switch (quantizationType) {
            case "none" -> quantized = value;
            case "with cut" -> {
                value = Math.clamp(value, min, max);
                quantized = min + step * (int)((value - min) / step);
            }
            case "with rounding" -> quantized = min + Math.round((value - min) / step) * step;
            default -> throw new IllegalArgumentException("Unsupported quantization type: " + quantizationType);
        }
        return quantized;
    }

    private static LinkedHashMap<Double, Double> quantizeSamples(LinkedHashMap<Double, Double> samples) {
        double min = Collections.min(samples.values());
        double max = Collections.max(samples.values());

        LinkedHashMap<Double, Double> quantized = new LinkedHashMap<>();
        for (var entry : samples.entrySet()) {
            quantized.put(entry.getKey(), quantize(entry.getValue(), min, max));
        }
        return quantized;
    }

    private static double getUniformValue(double range) {
        return (Math.random() * 2 - 1) * range;
    }

    // =====================================================================
    // =====================================================================

    // ======== CONVOLUTION ========

    /**
     * Implementation of convolution operation on discrete sets of numbers. Algorithm is
     * based on 'Input-side algorithm' with O(s1.size() * s2.size()) complexity.
     * Equation: (h * x)(n) = sum {h(k) * x(n - k)}
     *
     * @param s1 List of 1st signal's values
     * @param s2 List of 2nd signal's values
     * @return Product of convolution
     */
    public static List<Double> convolve(List<Double> s1, List<Double> s2) {
        logger.info("s1: " + s1);
        logger.info("s2: " + s2);

        int productSize = s1.size() + s2.size() - 1;
        List<Double> product = new ArrayList<>(Collections.nCopies(productSize, 0.0));

        for (int k = 0; k < s1.size(); k++) {
            for (int i = 0; i < s2.size(); i++) {
                double s1value = s1.get(k);
                double s2value = s2.get(i);
                double value = product.get(k + i) + s1value * s2value;
                product.set(k + i, value);
                logger.info("k: %s | i: %s | product: %s".formatted(k, i, product));
            }
        }

        logger.info("product: " + product);
        return product;
    }

    // ======== CROSS-CORRELATION ========

    public static List<Double> crossCorrelate(List<Double> s1, List<Double> s2) {
        logger.info("s1: " + s1);
        logger.info("s2: " + s2);

        int productSize = s1.size() + s2.size() - 1;
        List<Double> product = new ArrayList<>(Collections.nCopies(productSize, 0.0));

        for (int k = 0; k < s1.size(); k++) {
            for (int i = s2.size() - 1; i >= 0; i--) {
                double s1value = s1.get(k);
                double s2value = s2.get(i);
                double value = product.get(k + i) + s1value * s2value;
                product.set(k + i, value);
                logger.info("k: %s | i: %s | product: %s".formatted(k, i, product));
            }
        }

        logger.info("product: " + product);
        return product;
    }

    // ======== FILTRATION ========

    /**
     * Implementation of filter on discrete set using FIR filter (pl. SOI).
     * Filtration product is a convolution of signal and filter.
     * Filtered samples are cut to the size of original signal.
     * @param signal Signal object containing discrete set of samples.
     * @param M No. of coefficients in FIR.
     * @param cutoffFrequency Cut-off frequency of filter, required smaller than half of sampling frequency.
     * @return New signal object containing filtered samples.
     */
    private static Signal firFiltration(Signal signal, int M, double cutoffFrequency,
                                UnaryOperator<List<Double>> coefficientsModifier) {
        List<Double> timestamps = signal.getTimestampSamples().keySet().stream().toList();
        List<Double> samples = signal.getTimestampSamples().values().stream().toList();

        double samplingFrequency = signal.getTimestampSamples().size() / signal.getDurationTime();
        double K = samplingFrequency / cutoffFrequency;
        List<Double> coefficients = createFIRCoefficients(M, K);

        List<Double> modifiedCoefficients = coefficientsModifier.apply(coefficients);

        List<Double> product = convolve(samples, modifiedCoefficients);

        LinkedHashMap<Double, Double> filteredSamples = new LinkedHashMap<>();
        for (int i = 0; i < timestamps.size(); i++) {
            // get only the number of samples that are related to timestamps
            filteredSamples.put(timestamps.get(i), product.get(i));
        }

        return SignalFactory.createSignal(filteredSamples);
    }

    /**
     * Low pass filtration on discrete set using FIR. 
     * @param signal Signal object containing discrete set of samples.
     * @param M No. of coefficients in FIR.
     * @param cutoffFrequency Cut-off frequency of filter, required smaller than half of sampling frequency.
     * @return Signal object with filtered samples.
     */
    public static Signal lowPassFIRFiltration(Signal signal, int M, double cutoffFrequency) {
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
    public static Signal highPassFIRFiltration(Signal signal, int M, double cutoffFrequency) {
        return firFiltration(signal, M, cutoffFrequency, coefficients -> {
            List<Double> modifiedCoefficients = new ArrayList<>();
            for (int n = 0; n < coefficients.size(); n++) {
                modifiedCoefficients.add(coefficients.get(n) * Math.pow(-1, n));
            }
            return modifiedCoefficients;
        });
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

        double licznik = Math.sin(2 * Math.PI * (n - state) / K);
        double mianownik = Math.PI * (n - state);
        return licznik / mianownik;
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
