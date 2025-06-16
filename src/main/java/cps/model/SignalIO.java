package cps.model;

import cps.model.signals.Signal;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

public class SignalIO {
    private static final Logger logger = Logger.getLogger(SignalIO.class.getName());

    private SignalIO() {}

    public static Signal readSignalFromFile(String path) {
        Signal signal = null;
        try (FileInputStream fileInputStream = new FileInputStream(path);
             ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);) {
            signal = (Signal) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            logger.warning("dupa, nie działa :<");
        }
        return signal;
    }

    public static void writeSignalToFile(Signal signal) throws IOException {
        Path folder = Paths.get("signals/");
        Files.createDirectories(folder);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH-mm-ss");
        String timestamp = LocalTime.now().format(formatter);

        String filename = signal.getName() + "_" + LocalDate.now() + "_" + timestamp + ".ser";
        Path path = folder.resolve(filename);

        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(path.toFile()))) {
            objectOutputStream.writeObject(signal);
            objectOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
            logger.warning("dupa, nie działa :<");
        }
    }
}
