package cps.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SignalFactory {
    // przy tworzeniu sygnału okresowego należy ustalić minimalną liczbę próbkowania dla okresu sygnałów okresowych
    // częstotliwość próbkowania niech będzie 50 próbek na okres dla ładnego zarysowania kształtu naszego sygnału
    // np. dla t = 5s oraz T = 2s -> t_i = T / 50 = 0.04 s

    // w późniejszym czasie będzie trzeba uwzględnić długość trwania sygnału, aby dla sytuacji t = 100s, T = 2s
    // nie doszło do liczby próbek znacznie przekraczającej wartość użytkową (n = 2500)

    private static final Random random = new Random();

    public static final double SAMPLE_STEP = 0.01;
    private static final double AMPLITUDE = 1.0;

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

        return switch (type) {
            case UNIFORM_NOISE -> createUniformNoise(retrievedParams);
            case GAUSS_NOISE -> createGaussNoise(retrievedParams);
            case SINE -> createSineSignal(retrievedParams);
            case SINE_HALF -> createSineHalfSignal(retrievedParams);
            case SINE_FULL -> createSineFullSignal(retrievedParams);
            case RECTANGLE -> createRectangleSignal(retrievedParams);
            case RECTANGLE_SYMETRIC -> createRectangleSymmetricSignal(retrievedParams);
            case TRIANGLE -> createTriangleSignal(retrievedParams);
            case UNIT_STEP -> createUnitStepSignal(retrievedParams);
            case UNIT_IMPULS -> createUnitImpulseSignal(retrievedParams);
            case IMPULSE_NOISE -> createImpulseNoiseSignal(retrievedParams);
        };
    }

    public static Signal createUniformNoise(List<Double> params) {
        double amplitude = params.getFirst();
        double startTime = params.get(1);
        double durationTime = params.get(2);

        List<Double> samples = new ArrayList<>();

        for (double i = startTime; i < durationTime; i += SAMPLE_STEP) {
            samples.add(getUniformValue(amplitude));
        }

        return Signal.builder()
                .amplitude(amplitude)
                .startTime(startTime)
                .durationTime(durationTime)
                .samples(samples)
                .signalType(SignalType.UNIFORM_NOISE)
                .build();
    }

    public static Signal createGaussNoise(List<Double> params) {
        double amplitude = params.getFirst();
        double startTime = params.get(1);
        double durationTime = params.get(2);

        List<Double> samples = new ArrayList<>();

        for (double i = startTime; i < durationTime; i += SAMPLE_STEP) {
            samples.add(random.nextGaussian() * amplitude);
        }

        return Signal.builder()
                .amplitude(amplitude)
                .startTime(startTime)
                .durationTime(durationTime)
                .samples(samples)
                .signalType(SignalType.GAUSS_NOISE)
                .build();
    }

    public static Signal createSineSignal(List<Double> params) {
        if (params.size() != 4) {
            throw new IllegalArgumentException("zła liczba parametrów");
        }

        double amplitude = params.getFirst();
        double startTime = params.get(1);
        double durationTime = params.get(2);
        double period = params.get(3);

        List<Double> samples = new ArrayList<>();
        for (double i = startTime; i < durationTime; i += SAMPLE_STEP) {
            samples.add(amplitude * Math.sin(2 * Math.PI / period * i));
        }

        return PeriodicSignal.builder()
                .amplitude(amplitude)
                .startTime(startTime)
                .durationTime(durationTime)
                .samples(samples)
                .signalType(SignalType.SINE)
                .period(period)
                .build();
    }

    public static Signal createSineHalfSignal(List<Double> params) {
        if (params.size() != 4) {
            throw new IllegalArgumentException("zła liczba parametrów");
        }

        double amplitude = params.getFirst();
        double startTime = params.get(1);
        double durationTime = params.get(2);
        double period = params.get(3);

        List<Double> samples = new ArrayList<>();
        for (double i = startTime; i < durationTime; i += SAMPLE_STEP) {
            // Formula: x(t) = 0.5 * A * (sin(2π/T(t-t₁)) + |sin(2π/T(t-t₁))|)
            double sinValue = Math.sin(2 * Math.PI / period * (i - startTime));
            double halfRectified = 0.5 * amplitude * (sinValue + Math.abs(sinValue));
            samples.add(halfRectified);
        }

        return PeriodicSignal.builder()
                .amplitude(amplitude)
                .startTime(startTime)
                .durationTime(durationTime)
                .samples(samples)
                .signalType(SignalType.SINE_HALF)
                .period(period)
                .build();
    }

    public static Signal createSineFullSignal(List<Double> params) {
        if (params.size() != 4) {
            throw new IllegalArgumentException("zła liczba parametrów");
        }

        double amplitude = params.getFirst();
        double startTime = params.get(1);
        double durationTime = params.get(2);
        double period = params.get(3);

        List<Double> samples = new ArrayList<>();
        for (double i = startTime; i < durationTime; i += SAMPLE_STEP) {
            // Formula: x(t) = A * |sin(2π/T(t-t₁))|
            double absValue = Math.abs(Math.sin(2 * Math.PI / period * (i - startTime)));
            samples.add(amplitude * absValue);
        }

        return PeriodicSignal.builder()
                .amplitude(amplitude)
                .startTime(startTime)
                .durationTime(durationTime)
                .samples(samples)
                .signalType(SignalType.SINE_FULL)
                .period(period)
                .build();
    }

    public static Signal createRectangleSignal(List<Double> params) {
        if (params.size() != 5) {
            throw new IllegalArgumentException("zła liczba parametrów");
        }

        double amplitude = params.getFirst();
        double startTime = params.get(1);
        double durationTime = params.get(2);
        double period = params.get(3);
        double dutyCycle = params.get(4); // kₚ - współczynnik wypełnienia (0-1)

        List<Double> samples = new ArrayList<>();
        for (double i = startTime; i < durationTime; i += SAMPLE_STEP) {
            double time = (i - startTime) % period;
            double value = (time < dutyCycle * period) ? amplitude : 0;
            samples.add(value);
        }

        return PolygonalSignal.builder()
                .amplitude(amplitude)
                .startTime(startTime)
                .durationTime(durationTime)
                .samples(samples)
                .signalType(SignalType.RECTANGLE)
                .period(period)
                .dutyCycle(dutyCycle)
                .build();
    }

    public static Signal createRectangleSymmetricSignal(List<Double> params) {
        if (params.size() != 5) {
            throw new IllegalArgumentException("zła liczba parametrów");
        }

        double amplitude = params.getFirst();
        double startTime = params.get(1);
        double durationTime = params.get(2);
        double period = params.get(3);
        double dutyCycle = params.get(4); // kₚ - współczynnik wypełnienia (0-1)

        List<Double> samples = new ArrayList<>();
        for (double i = startTime; i < durationTime; i += SAMPLE_STEP) {
            double time = (i - startTime) % period;
            double value = (time < dutyCycle * period) ? amplitude : -amplitude;
            samples.add(value);
        }

        return PolygonalSignal.builder()
                .amplitude(amplitude)
                .startTime(startTime)
                .durationTime(durationTime)
                .samples(samples)
                .signalType(SignalType.RECTANGLE_SYMETRIC)
                .period(period)
                .dutyCycle(dutyCycle)
                .build();
    }

    public static Signal createTriangleSignal(List<Double> params) {
        if (params.size() != 5) {
            throw new IllegalArgumentException("zła liczba parametrów");
        }

        double amplitude = params.getFirst();
        double startTime = params.get(1);
        double durationTime = params.get(2);
        double period = params.get(3);
        double dutyCycle = params.get(4); // kₚ - współczynnik wypełnienia (0-1)

        List<Double> samples = new ArrayList<>();
        for (double i = startTime; i < durationTime; i += SAMPLE_STEP) {
            double time = (i - startTime) % period;
            double value;

            if (time < dutyCycle * period) {
                // Rising edge
                value = (time / (dutyCycle * period)) * amplitude;
            } else {
                // Falling edge
                value = amplitude - ((time - dutyCycle * period) / ((1 - dutyCycle) * period)) * amplitude;
            }

            samples.add(value);
        }

        return PolygonalSignal.builder()
                .amplitude(amplitude)
                .startTime(startTime)
                .durationTime(durationTime)
                .samples(samples)
                .signalType(SignalType.TRIANGLE)
                .period(period)
                .dutyCycle(dutyCycle)
                .build();
    }

    public static Signal createUnitStepSignal(List<Double> params) {
        double amplitude = params.getFirst();
        double startTime = params.get(1);
        double durationTime = params.get(2);
        double stepTime = params.size() > 3 ? params.get(3) : startTime; // Time when step occurs

        List<Double> samples = new ArrayList<>();
        for (double i = startTime; i < durationTime; i += SAMPLE_STEP) {
            double value = (i >= stepTime) ? amplitude : 0;
            samples.add(value);
        }

        return Signal.builder()
                .amplitude(amplitude)
                .startTime(startTime)
                .durationTime(durationTime)
                .samples(samples)
                .signalType(SignalType.UNIT_STEP)
                .build();
    }

    public static Signal createUnitImpulseSignal(List<Double> params) {
        double amplitude = params.getFirst();
        double startTime = params.get(1);
        double durationTime = params.get(2);
        double impulseTime = params.size() > 3 ? params.get(3) : startTime; // Time when impulse occurs

        List<Double> samples = new ArrayList<>();
        for (double i = startTime; i < durationTime; i += SAMPLE_STEP) {
            // Using a small window for the impulse to make it visible
            double value = (Math.abs(i - impulseTime) < SAMPLE_STEP / 2) ? amplitude : 0;
            samples.add(value);
        }

        return Signal.builder()
                .amplitude(amplitude)
                .startTime(startTime)
                .durationTime(durationTime)
                .samples(samples)
                .signalType(SignalType.UNIT_IMPULS)
                .build();
    }
    public static Signal createImpulseNoiseSignal(List<Double> params) {
        double amplitude = params.getFirst();
        double startTime = params.get(1);
        double durationTime = params.get(2);
        double probability = params.size() > 3 ? params.get(3) : 0.1; // Probability of impulse occurrence

        List<Double> samples = new ArrayList<>();
        for (double i = startTime; i < durationTime; i += SAMPLE_STEP) {
            // Generate random impulses with given probability
            double value = (random.nextDouble() < probability) ? amplitude : 0;
            samples.add(value);
        }

        return Signal.builder()
                .amplitude(amplitude)
                .startTime(startTime)
                .durationTime(durationTime)
                .samples(samples)
                .signalType(SignalType.IMPULSE_NOISE)
                .build();
    }

    public static double getUniformValue(double range) {
        return (Math.random() * 2 - 1) * range;
    }
}
