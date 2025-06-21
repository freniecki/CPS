package cps.model;

import cps.model.signals.Complex;
import org.junit.jupiter.api.Test;

import static cps.model.SignalOperations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SignalOperationsTest {

    @Test
    void fourierTest() {
        double[] test = new double[]{
                0.0, 1.383883, 0.75, 0.383883, 0.0, -0.383883, -0.75, -1.383883
        };
        int log2N = 3;

        Complex[] dftResult = dft(test, log2N);
        Complex[] fftResult = fftDIF(test, log2N);

        StringBuilder sbReal = new StringBuilder();
        sbReal.append("== FOURIER ==").append("\n");
        sbReal.append("---REAL---").append("\n");
        sbReal.append(" DFT | FFT ").append('\n');
        for (int i = 0; i < dftResult.length; i++) {
            sbReal.append("%.5f | %.5f".formatted(dftResult[i].real(), fftResult[i].real())).append('\n');
        }
        System.out.println(sbReal);

        StringBuilder sbImag = new StringBuilder();
        sbImag.append("---IMAG---").append("\n");
        sbImag.append(" DFT | FFT ").append('\n');
        for (int i = 0; i < dftResult.length; i++) {
            sbImag.append("%.5f | %.5f".formatted(dftResult[i].imaginary(), fftResult[i].imaginary())).append('\n');
        }
        System.out.println(sbImag);

    }

    @Test
    void fftTest() {
        Complex[] test = new Complex[]{
                new Complex(1, 0),
                new Complex(2, -1),
                new Complex(0, -1),
                new Complex(-1, 2),
        };
        Complex[] result = fftDIF(test, 2);

        StringBuilder sb = new StringBuilder();
        sb.append("== FFT ==").append("\n");
        sb.append(" R  |  I ").append("\n");
        for (Complex complex : result) {
            sb.append("%.1f".formatted(complex.real())).append(" | ");
            sb.append("%.1f".formatted(complex.imaginary())).append('\n');
        }
        System.out.println(sb);
    }

    @Test
    void cosineTest() {
        double[] test = new double[]{
                1.0, 0.0, -1.0, 0.5, 0.0, -0.5, 1.0, -1.0
        };

        double[] dctResult = SignalOperations.dctII(test);
        double[] fctResult = SignalOperations.fctII(test);


        StringBuilder sb = new StringBuilder();
        sb.append("== COSINE ==").append("\n");
        sb.append("---REAL---").append("\n");
        sb.append(" DCT | FCT ").append('\n');
        for (int i = 0; i < dctResult.length; i++) {
            sb.append("%.2f | %.2f".formatted(dctResult[i], fctResult[i])).append('\n');
        }
        System.out.println(sb);
    }

    @Test
    void flipBits4() {
        Complex[] samples = new Complex[4];
        for (int i = 0; i < 4; i++) {
            samples[i] = new Complex(i, 0);
        }

        Complex[] flipped = SignalOperations.flipBits(samples, 2);
        StringBuilder sb2 = new StringBuilder();
        for (Complex c : flipped) {
            sb2.append(c.real()).append(" ");
        }

        assertEquals("0.0 2.0 1.0 3.0 ", sb2.toString());
    }

    @Test
    void flipBits8() {
        Complex[] samples = new Complex[8];
        for (int i = 0; i < 8; i++) {
            samples[i] = new Complex(i, 0);
        }

        Complex[] flipped = SignalOperations.flipBits(samples, 3);
        StringBuilder sb2 = new StringBuilder();
        for (Complex c : flipped) {
            sb2.append(c.real()).append(" ");
        }

        assertEquals("0.0 4.0 2.0 6.0 1.0 5.0 3.0 7.0 ", sb2.toString());
    }

    @Test
    void testModulo() {
        System.out.println(11 % 2);
        System.out.println(12 % 2);
        System.out.println(16 % 2);
    }
}