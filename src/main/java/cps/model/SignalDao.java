package cps.model;

import cps.model.signals.Signal;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.logging.Logger;

public class SignalDao {
    private static final Logger logger = Logger.getLogger(SignalDao.class.getName());

    private SignalDao() {}

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

    public static void writeSignalToFile(Signal signal) {
        String pathString = "resources/signals/" + LocalDate.now() + "_" + LocalTime.now() + ".ser";

        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(pathString))) {
            objectOutputStream.writeObject(signal);
            objectOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
            logger.warning("dupa, nie działa :<");
        }
    }
}
