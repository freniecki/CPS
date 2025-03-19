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
            case SINE_HALF -> null;
            case SINE_FULL -> null;
            case RECTANGLE -> null;
            case RECTANGLE_SYMETRIC -> null;
            case TRIANGLE -> null;
            case UNIT_STEP -> null;
            case UNIT_IMPULS -> null;
            case IMPULSE_NOISE -> null;
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

    public static double getUniformValue(double range) {
        return (Math.random() * 2 - 1) * range;
    }
}
