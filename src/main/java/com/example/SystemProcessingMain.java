package com.example;

import com.example.message.ExceptionMessage;
import com.example.message.SensorDataMessage;
import com.example.message.SimpleMissage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class SystemProcessingMain {
    public static void main(String[] args) throws InterruptedException, TimeoutException {

        List<SimpleMissage> messages = new ArrayList<>() {
            {
                add(new SensorDataMessage("temperature", 2));
                add(new SensorDataMessage("temperature", 3));
                add(new SensorDataMessage("temperature", 4));
                add(new SensorDataMessage("humidity", 4));
                add(new SensorDataMessage("humidity", 5));
                add(new SensorDataMessage("humidity", 6));
                add(new SensorDataMessage("temperature", 5));
                add(new SensorDataMessage("temperature", 5));
                add(new ExceptionMessage("NULL"));
                add(new SensorDataMessage("humidity", 3));
                add(new SensorDataMessage("humidity", 2));
                add(new SensorDataMessage("temperature", 3));
                add(new SensorDataMessage("humidity", 1));
            }
        };

        MainPipeline pipeline = new MainPipeline(messages, 2, 1, 3, 3, 3, 1);

        pipeline.start();

    }

}
