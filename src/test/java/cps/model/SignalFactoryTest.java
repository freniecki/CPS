package cps.model;

import org.junit.jupiter.api.Test;

class SignalFactoryTest {

    @Test
    void generateUniform() {
        for (int i = 0; i < 5; i++) {
            System.out.println(SignalFactory.getUniformValue(1.0));
        }
    }

}