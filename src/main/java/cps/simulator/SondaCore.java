package cps.simulator;

import cps.model.Signal;
import cps.model.SignalFactory;

import java.util.*;
import java.util.logging.Logger;

public class SondaCore {
    private static final Logger logger = Logger.getLogger(SondaCore.class.getName());

    private SondaCore() {}

    public static double run(Signal signal, double actualDistance, double signalVelocity, int bufferSize) {
        List<Double> timestamps = signal.getTimestampSamples().keySet().stream().toList();
        List<Double> samples = signal.getTimestampSamples().values().stream().toList();

        double timeStep = timestamps.get(1) - timestamps.getFirst();
        int arrivalIndex = (int) Math.round(2 * actualDistance / signalVelocity / timeStep);
        logger.info("dt: %s | arrivalIndex: %s".formatted(timeStep, arrivalIndex));

        List<Double> baseValues = samples.subList(0, bufferSize);
        logger.fine("baseValues: " + baseValues);

        List<Double> shiftedValues = samples.subList(arrivalIndex, arrivalIndex + bufferSize);
        logger.fine("shiftedValues: " + shiftedValues);

        List<Double> correlationProduct = SignalFactory.crossCorrelate(baseValues, shiftedValues);
        logger.fine("correlationProduct: " + correlationProduct);

        int correlationSize = correlationProduct.size();
        int correlationMiddle = correlationSize / 2;
        int maxIndex = correlationMiddle;
        for (int i = correlationMiddle; i < correlationSize; i++) {
            maxIndex = correlationProduct.get(i) > correlationProduct.get(maxIndex) ? i : maxIndex;
        }
        logger.info("maxIndex: " + maxIndex);

        double detectedDelay = (maxIndex) * timeStep;
        double measuredDistance = (detectedDelay * signalVelocity) / 2.0;

        String message =
                """
                === RAPORT POMIARU ===
                Rzeczywista odległość: %.3f
                Wyznaczona odległość: %.3f
                """.formatted(actualDistance, measuredDistance);
        logger.info(message);

        return measuredDistance;
    }
}
