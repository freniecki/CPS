package cps.model;

import java.util.ArrayList;
import java.util.List;

public class SignalFactory {
    // przy tworzeniu sygnału okresowego należy ustalić minimalną liczbę próbkowania dla okresu sygnałów okresowych
    // częstotliwość próbkowania niech będzie 50 próbek na okres dla ładnego zarysowania kształtu naszego sygnału
    // np. dla t = 5s oraz T = 2s -> t_i = T / 50 = 0.04 s
    // w późniejszym czasie będzie trzeba uwzględnić długość trwania sygnału, aby dla sytuacji t = 100s, T = 2s
    // nie doszło do liczby próbek znacznie przekraczającej wartość użytkową (n = 2500)
    private static final int MIN_PERIODIC_SAMPLE_SIZE = 50;

    private SignalFactory() {
    }

    public static Signal createSignal(SignalType type) {
        return switch (type) {
            case UNIFORM_NOISE -> createUniformNoise();
            case GAUSS_NOISE -> createGaussNoise();
            case SINE -> createSineSignal();
            case SINE_HALF -> null;
            default -> null;
        };
    }

    public static Signal createUniformNoise() {
        List<Double> samples = new ArrayList<>();
        double sampleTime = 5.0;
        double amplitude = 1.0;

        for (int i = 0; i < sampleTime; i++) {
            samples.add(amplitude * generateUniform(1.0));
        }

        return new Signal(SignalType.UNIFORM_NOISE, samples, sampleTime, amplitude);
    }

    public static Signal createGaussNoise() {
        return null;
    }

    public static Signal createSineSignal() {
        List<Double> samples = new ArrayList<>();
        double sampleTime = 5.0;
        double amplitude = 1.0;

        double period = 2.0;
        double timeIncrease = period / MIN_PERIODIC_SAMPLE_SIZE;

        for (double i = 0; i < sampleTime; i += timeIncrease) {
            samples.add(amplitude * Math.sin(2 * Math.PI / period * i));
        }

        return new Signal(SignalType.SINE, samples, sampleTime, amplitude);
    }

    public static double generateUniform(double range) {
        return (Math.random() * 2 - 1) * range;
    }
}
