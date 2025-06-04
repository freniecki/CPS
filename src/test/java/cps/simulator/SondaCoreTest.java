package cps.simulator;

import cps.model.SignalFactory;
import cps.model.signals.SignalType;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

class SondaCoreTest {

    @Test
    void run() {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("A", "1.0");
        params.put("t", "0.0");
        params.put("d", "100.0");
        params.put("T", "1.0");

        SondaCore sonda = new SondaCore();

        sonda.run(
                SignalFactory.createSignal(SignalType.SINE, params),
                2.78,
                1.0,
                300
        );
    }
}