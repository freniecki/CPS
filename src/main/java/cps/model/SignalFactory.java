package cps.model;

import java.util.*;
import java.util.logging.Logger;

public class SignalFactory {
    // przy tworzeniu sygnału okresowego należy ustalić minimalną liczbę próbkowania dla okresu sygnałów okresowych
    // częstotliwość próbkowania niech będzie 50 próbek na okres dla ładnego zarysowania kształtu naszego sygnału
    // np. dla t = 5s oraz T = 2s -> t_i = T / 50 = 0.04 s

    // w późniejszym czasie będzie trzeba uwzględnić długość trwania sygnału, aby dla sytuacji t = 100s, T = 2s
    // nie doszło do liczby próbek znacznie przekraczającej wartość użytkową (n = 2500)
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
    // ---- CONTINUOUS SIGNALS ----

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
        for (double timestamp = startTime; timestamp < startTime + durationTime; timestamp += sampleStep) {
            // create uniformly quantized signal

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

    // ---- DISCRETE SIGNALS ----

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

    // ---- TOOLS ----

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
}
