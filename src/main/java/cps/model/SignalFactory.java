package cps.model;

import java.util.*;
import java.util.logging.Logger;

public class SignalFactory {
    // przy tworzeniu sygnału okresowego należy ustalić minimalną liczbę próbkowania dla okresu sygnałów okresowych
    // częstotliwość próbkowania niech będzie 50 próbek na okres dla ładnego zarysowania kształtu naszego sygnału
    // np. dla t = 5s oraz T = 2s -> t_i = T / 50 = 0.04 s

    // w późniejszym czasie będzie trzeba uwzględnić długość trwania sygnału, aby dla sytuacji t = 100s, T = 2s
    // nie doszło do liczby próbek znacznie przekraczającej wartość użytkową (n = 2500)
    private static final Logger logger = Logger.getLogger(SignalFactory.class.getName());
    private static final Random random = new Random();

    public static final double SAMPLE_STEP = 0.01;
    public static final double MIN_PERIOD_SAMPLING_RATE = 20;

    private static final String PARAM_NO_TYPE_ERROR = "No. of param no equals to type: ";

    private SignalFactory() {
    }

    public static Signal createSignal(SignalType type, List<String> params) {
        if (params.size() < 3 || params.size() > 5) {
            throw new IllegalArgumentException("params list size incorrect: " + params.size());
        }

        List<Double> retrievedParams = new ArrayList<>();
        for (String p : params) {
            retrievedParams.add(Double.parseDouble(p));
        }

        double amplitude = retrievedParams.getFirst();
        double startTime = retrievedParams.get(1);
        double durationTime = retrievedParams.get(2);

        if (params.size() == 3) {
            return switch (type) {
                case UNIFORM_NOISE -> createUniformNoise(amplitude, startTime, durationTime);
                case GAUSS_NOISE -> createGaussNoise(amplitude, startTime, durationTime);
                case UNIT_IMPULS -> createUnitImpulseSignal(amplitude, startTime, durationTime);
                case UNIT_STEP -> createUnitStepSignal(amplitude, startTime, durationTime);
                case IMPULSE_NOISE -> createImpulseNoiseSignal(amplitude, startTime, durationTime);
                default -> throw new IllegalArgumentException(PARAM_NO_TYPE_ERROR + type);
            };
        } else if (params.size() == 4) {
            double period = retrievedParams.get(3);
            return switch (type) {
                case SINE -> createSineSignal(amplitude, startTime, durationTime, period);
                case SINE_HALF -> createSineHalfSignal(amplitude, startTime, durationTime, period);
                case SINE_FULL -> createSineFullSignal(amplitude, startTime, durationTime, period);
                default -> throw new IllegalArgumentException(PARAM_NO_TYPE_ERROR + type);
            };
        } else if (params.size() == 5) {
            double period = retrievedParams.get(3);
            double dutyCycle = retrievedParams.get(4);
            return switch (type) {
                case RECTANGLE -> createRectangleSignal(amplitude, startTime, durationTime, period, dutyCycle);
                case RECTANGLE_SYMETRIC -> createRectangleSymmetricSignal(amplitude, startTime, durationTime, period, dutyCycle);
                case TRIANGLE -> createTriangleSignal(amplitude, startTime, durationTime, period, dutyCycle);
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
                .timestampSamples(samples)
                .signalType(SignalType.CUSTOM)
                .build();
    }

    private static Signal createUniformNoise(double amplitude, double startTime, double durationTime) {
        LinkedHashMap<Double, Double> samples = new LinkedHashMap<>();
        for (double timestamp = startTime; timestamp < startTime + durationTime; timestamp += SAMPLE_STEP) {
            samples.putLast(timestamp, getUniformValue(amplitude));
        }

        return Signal.builder()
                .amplitude(amplitude)
                .startTime(startTime)
                .durationTime(durationTime)
                .timestampSamples(samples)
                .signalType(SignalType.UNIFORM_NOISE)
                .build();
    }

    private static Signal createGaussNoise(double amplitude, double startTime, double durationTime) {
        LinkedHashMap<Double, Double> samples = new LinkedHashMap<>();
        for (double timestamp = startTime; timestamp < startTime + durationTime; timestamp += SAMPLE_STEP) {
            samples.putLast(timestamp,random.nextGaussian() * amplitude);
        }

        return Signal.builder()
                .amplitude(amplitude)
                .startTime(startTime)
                .durationTime(durationTime)
                .timestampSamples(samples)
                .signalType(SignalType.GAUSS_NOISE)
                .build();
    }

    private static Signal createSineSignal(double amplitude, double startTime, double durationTime, double period) {
        LinkedHashMap<Double, Double> samples = new LinkedHashMap<>();
        for (double timestamp = startTime; timestamp < startTime + durationTime; timestamp += SAMPLE_STEP) {
            samples.putLast(timestamp, amplitude * Math.sin(2 * Math.PI / period * timestamp));
        }

        return PeriodicSignal.builder()
                .amplitude(amplitude)
                .startTime(startTime)
                .durationTime(durationTime)
                .timestampSamples(samples)
                .signalType(SignalType.SINE)
                .period(period)
                .build();
    }

    private static Signal createSineHalfSignal(double amplitude, double startTime, double durationTime, double period) {
        LinkedHashMap<Double, Double> samples = new LinkedHashMap<>();
        for (double timestamp = startTime; timestamp < startTime + durationTime; timestamp += SAMPLE_STEP) {
            double sinValue = Math.sin(2 * Math.PI / period * (timestamp - startTime));
            double halfRectified = 0.5 * amplitude * (sinValue + Math.abs(sinValue));
            samples.putLast(timestamp, halfRectified);
        }

        return PeriodicSignal.builder()
                .amplitude(amplitude)
                .startTime(startTime)
                .durationTime(durationTime)
                .timestampSamples(samples)
                .signalType(SignalType.SINE_HALF)
                .period(period)
                .build();
    }

    private static Signal createSineFullSignal(double amplitude, double startTime, double durationTime, double period) {
        LinkedHashMap<Double, Double> samples = new LinkedHashMap<>();
        for (double timestamp = startTime; timestamp < startTime + durationTime; timestamp += SAMPLE_STEP) {
            // Formula: x(t) = A * |sin(2π/T(t-t₁))|
            double absValue = Math.abs(Math.sin(2 * Math.PI / period * (timestamp - startTime)));
            samples.putLast(timestamp, amplitude * absValue);
        }

        return PeriodicSignal.builder()
                .amplitude(amplitude)
                .startTime(startTime)
                .durationTime(durationTime)
                .timestampSamples(samples)
                .signalType(SignalType.SINE_FULL)
                .period(period)
                .build();
    }

    private static Signal createRectangleSignal(double amplitude, double startTime, double durationTime, double period, double dutyCycle) {
        LinkedHashMap<Double, Double> samples = new LinkedHashMap<>();
        for (double timestamp = startTime; timestamp < startTime + durationTime; timestamp += SAMPLE_STEP) {
            double time = (timestamp - startTime) % period;
            double value = (time < dutyCycle * period) ? amplitude : 0;
            samples.putLast(timestamp, value);
        }

        return PolygonalSignal.builder()
                .amplitude(amplitude)
                .startTime(startTime)
                .durationTime(durationTime)
                .timestampSamples(samples)
                .signalType(SignalType.RECTANGLE)
                .period(period)
                .dutyCycle(dutyCycle)
                .build();
    }

    private static Signal createRectangleSymmetricSignal(double amplitude, double startTime, double durationTime, double period, double dutyCycle) {
        LinkedHashMap<Double, Double> samples = new LinkedHashMap<>();
        for (double timestamp = startTime; timestamp < startTime + durationTime; timestamp += SAMPLE_STEP) {
            double time = (timestamp - startTime) % period;
            double value = (time < dutyCycle * period) ? amplitude : -amplitude;
            samples.putLast(timestamp, value);
        }

        return PolygonalSignal.builder()
                .amplitude(amplitude)
                .startTime(startTime)
                .durationTime(durationTime)
                .timestampSamples(samples)
                .signalType(SignalType.RECTANGLE_SYMETRIC)
                .period(period)
                .dutyCycle(dutyCycle)
                .build();
    }

    private static Signal createTriangleSignal(double amplitude, double startTime, double durationTime, double period, double dutyCycle) {
        LinkedHashMap<Double, Double> samples = new LinkedHashMap<>();
        for (double timestamp = startTime; timestamp < startTime + durationTime; timestamp += SAMPLE_STEP) {
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
                .timestampSamples(samples)
                .signalType(SignalType.TRIANGLE)
                .period(period)
                .dutyCycle(dutyCycle)
                .build();
    }

    // todo: repair steptime
    private static Signal createUnitStepSignal(double amplitude, double startTime, double durationTime) {
        double stepTime = 0.1;

        LinkedHashMap<Double, Double> samples = new LinkedHashMap<>();
        for (double timestamp = startTime; timestamp < startTime + durationTime; timestamp += SAMPLE_STEP) {
            double value = (timestamp >= stepTime) ? amplitude : 0;
            samples.putLast(timestamp, value);
        }

        return Signal.builder()
                .amplitude(amplitude)
                .startTime(startTime)
                .durationTime(durationTime)
                .timestampSamples(samples)
                .signalType(SignalType.UNIT_STEP)
                .build();
    }

    //todo: repair impulseTime
    private static Signal createUnitImpulseSignal(double amplitude, double startTime, double durationTime) {
        double impulseTime = 0.1;

        LinkedHashMap<Double, Double> samples = new LinkedHashMap<>();
        for (double timestamp = startTime; timestamp < startTime + durationTime; timestamp += SAMPLE_STEP) {
            // Using a small window for the impulse to make it visible
            double value = (Math.abs(timestamp - impulseTime) < SAMPLE_STEP / 2) ? amplitude : 0;
            samples.putLast(timestamp, value);
        }

        return Signal.builder()
                .amplitude(amplitude)
                .startTime(startTime)
                .durationTime(durationTime)
                .timestampSamples(samples)
                .signalType(SignalType.UNIT_IMPULS)
                .build();
    }

    // todo: repair probability
    private static Signal createImpulseNoiseSignal(double amplitude, double startTime, double durationTime) {
        double probability = 0.1;

        LinkedHashMap<Double, Double> samples = new LinkedHashMap<>();
        for (double timestamp = startTime; timestamp < startTime + durationTime; timestamp += SAMPLE_STEP) {
            // Generate random impulses with given probability
            double value = (random.nextDouble() < probability) ? amplitude : 0;
            samples.putLast(timestamp, value);
        }

        return Signal.builder()
                .amplitude(amplitude)
                .startTime(startTime)
                .durationTime(durationTime)
                .timestampSamples(samples)
                .signalType(SignalType.IMPULSE_NOISE)
                .build();
    }

    private static double getUniformValue(double range) {
        return (Math.random() * 2 - 1) * range;
    }
}
