package cps.model;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

class SignalFactoryTest {

    @Test
    void convolveTest() {
        LinkedHashMap<Double, Double> signal1 = new LinkedHashMap<>();
        signal1.put(0.0, 1.0);
        signal1.put(0.1, 2.0);
        signal1.put(0.2, 3.0);
        signal1.put(0.3, 4.0);

        LinkedHashMap<Double, Double> signal2 = new LinkedHashMap<>();
        signal2.put(0.0, 1.0);
        signal2.put(0.1, 2.0);
        signal2.put(0.2, 3.0);

        List<Double> product = SignalOperations.convolve(
                signal1.values().stream().toList(),
                signal2.values().stream().toList());

        System.out.println(product);
    }
}