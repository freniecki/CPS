package cps.model;

import org.junit.jupiter.api.Test;

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

    @Test
    void generateUniform() {
        for (int i = 0; i < 5; i++) {
            System.out.println(SignalFactory.generateUniform(1.0));
        }
    }

}