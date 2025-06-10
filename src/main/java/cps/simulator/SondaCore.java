package cps.simulator;

import cps.dto.SondaDto;
import cps.dto.SondaInTimeDto;
import cps.model.signals.Signal;
import cps.model.SignalFactory;
import cps.model.SignalOperations;
import lombok.Getter;

import java.util.*;
import java.util.logging.Logger;

@Getter
public class SondaCore {
    private static final Logger logger = Logger.getLogger(SondaCore.class.getName());

    private SondaCore() {}

    public static SondaInTimeDto runInTime(Signal signal, double initialDistance, double objectVelocity, double signalVelocity, double deltaT, int bufferSize) {
        Map<Double, Double> theoreticalSamples = new HashMap<>();
        Map<Double, Double> measuredSamples = new HashMap<>();

        for (double time = 0; time <= 5.0; time += deltaT) {
            double currentDistance = initialDistance + objectVelocity * time;
            SondaDto result = run(signal, currentDistance, signalVelocity, bufferSize);
            if (result == null) {
                logger.warning("Sonda failed at time: " + time);
                continue;
            }
            theoreticalSamples.put(time, currentDistance);
            measuredSamples.put(time, result.measuredDistance());
        }

        Signal theoreticalSignal = SignalFactory.createSignal(theoreticalSamples);
        theoreticalSignal.setName("theoretical");
        Signal measuredSignal = SignalFactory.createSignal(measuredSamples);
        measuredSignal.setName("measured");

        return new SondaInTimeDto(theoreticalSignal, measuredSignal);
    }

    public static SondaDto run(Signal signal, double actualDistance, double signalVelocity, int bufferSize) {
        List<Double> timestamps = signal.getTimestampSamples().keySet().stream().toList();
        List<Double> samples = signal.getTimestampSamples().values().stream().toList();

        double timeStep = timestamps.get(1) - timestamps.getFirst();
        int arrivalIndex = (int) Math.round(2 * actualDistance / signalVelocity / timeStep);
        logger.info("timeStep: %s | arrivalIndex: %s".formatted(timeStep, arrivalIndex));

        if (signal.getDurationTime() < (2 * actualDistance / signalVelocity) + (bufferSize * timeStep)) {
            logger.warning("Signal is too short.");
            return null;
        }

        List<Double> baseValues = samples.subList(0, bufferSize);
        List<Double> shiftedValues = samples.subList(arrivalIndex, arrivalIndex + bufferSize);
        List<Double> correlationProduct = SignalOperations.crossCorrelate(baseValues, shiftedValues);

        Map<Double, Double> correlationProductTimestampSamples = createTimestampSamples(correlationProduct, timeStep);

        double measuredDistance = getMeasuredDistance(signalVelocity, correlationProduct, timeStep);

        Signal correlationSignal = SignalFactory.createSignal(correlationProductTimestampSamples);
        Signal baseBufforedSignal = SignalFactory.createSignal(createTimestampSamples(baseValues, timeStep));
        Signal shiftedBufforedSignal = SignalFactory.createSignal(createTimestampSamples(shiftedValues, timeStep));

        return new SondaDto(correlationSignal, baseBufforedSignal, shiftedBufforedSignal, measuredDistance);
    }

    private static double getMeasuredDistance(double signalVelocity, List<Double> correlationProduct, double timeStep) {
        int correlationSize = correlationProduct.size();
        int correlationMiddle = correlationSize / 2;
        int maxIndex = correlationMiddle;
        for (int i = correlationMiddle; i < correlationSize; i++) {
            maxIndex = correlationProduct.get(i) > correlationProduct.get(maxIndex) ? i : maxIndex;
        }
        logger.info("maxIndex: " + maxIndex);

        double detectedDelay = (maxIndex - correlationMiddle) * timeStep;
        return (detectedDelay * signalVelocity) / 2.0;
    }

    private static Map<Double, Double> createTimestampSamples(List<Double> samples, double timeStep) {
        Map<Double, Double> timeStampSamples = new HashMap<>();
        for (int i = 0; i < samples.size(); i++) {
            timeStampSamples.put(i * timeStep, samples.get(i));
        }
        return timeStampSamples;
    }
}
