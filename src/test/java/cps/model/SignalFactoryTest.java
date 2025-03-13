package cps.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
class SignalFactoryTest {

    @Test
    void createSineSignal() {
        Signal sine = SignalFactory.createSineSignal();

        System.out.println(sine.getSampleSize());
        System.out.println(sine.getSampleTime());
        System.out.println(sine.getAmplitude());

        for (double sample : sine.samples) {
            System.out.println(sample);
        }
    }
}