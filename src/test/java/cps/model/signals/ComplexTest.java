package cps.model.signals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComplexTest {

    @Test
    void times() {
        Complex a = new Complex(0.0, -4.0);
        Complex W = new Complex(-0.7071, -0.7071);

        Complex result = a.times(W);
        System.out.printf("Result: %.4f + %.4fi\n", result.real(), result.imaginary());
    }
}