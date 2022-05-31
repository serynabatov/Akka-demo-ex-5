package com.example;

import com.example.message.ExceptionMessage;
import com.example.message.SensorDataMessage;
import com.example.message.SimpleMissage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class FailToleranceTest {

    private final PrintStream standardOut = System.out;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

    @BeforeEach
    public void setUp() {
        System.setOut(new PrintStream(outputStreamCaptor));
    }

    @Test
    public void errorAtTheStart() throws InterruptedException, TimeoutException {

        List<SimpleMissage> messages = new ArrayList<>() {
            {
                add(new ExceptionMessage("NULL"));
                add(new SensorDataMessage("temperature", 2));
                add(new SensorDataMessage("temperature", 3));
                add(new SensorDataMessage("temperature", 4));
                add(new SensorDataMessage("humidity", 4));
                add(new SensorDataMessage("humidity", 5));
                add(new SensorDataMessage("humidity", 6));
                add(new SensorDataMessage("temperature", 5));
                add(new SensorDataMessage("temperature", 5));
                add(new SensorDataMessage("humidity", 3));
                add(new SensorDataMessage("humidity", 2));
                add(new SensorDataMessage("temperature", 3));
                add(new SensorDataMessage("humidity", 1));
            }
        };

        MainPipeline pipeline = new MainPipeline(messages, 2, 1, 3, 3, 3, 1);

        pipeline.start();
        String result = "Here we are emulating an error! NULL\nResult: max-temperature 0.816\nResult: max-humidity 1.247";
        String result2 = "Here we are emulating an error! NULL\nResult: max-humidity 1.247\nResult: max-temperature 0.816";

        Assertions.assertTrue(result.equalsIgnoreCase(outputStreamCaptor.toString().trim())
                || result2.equalsIgnoreCase(outputStreamCaptor.toString().trim()));
    }

    @Test
    public void errorAtTheEnd() throws InterruptedException, TimeoutException {

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
                add(new SensorDataMessage("humidity", 3));
                add(new SensorDataMessage("humidity", 2));
                add(new SensorDataMessage("temperature", 3));
                add(new SensorDataMessage("humidity", 1));
                add(new ExceptionMessage("NULL"));
            }
        };

        MainPipeline pipeline = new MainPipeline(messages, 2, 1, 3, 3, 3, 1);

        pipeline.start();
        String result = "Here we are emulating an error! NULL\nResult: max-temperature 0.816\nResult: max-humidity 1.247";
        String result2 = "Here we are emulating an error! NULL\nResult: max-humidity 1.247\nResult: max-temperature 0.816";

        Assertions.assertTrue(result.equalsIgnoreCase(outputStreamCaptor.toString().trim())
                || result2.equalsIgnoreCase(outputStreamCaptor.toString().trim()));
    }

    @Test
    public void errorAtTheMiddle() throws InterruptedException, TimeoutException {

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
        String result = "Here we are emulating an error! NULL\nResult: max-temperature 0.816\nResult: max-humidity 1.247";
        String result2 = "Here we are emulating an error! NULL\nResult: max-humidity 1.247\nResult: max-temperature 0.816";

        Assertions.assertTrue(result.equalsIgnoreCase(outputStreamCaptor.toString().trim())
                || result2.equalsIgnoreCase(outputStreamCaptor.toString().trim()));
    }


    @AfterEach
    public void tearDown() {
        System.setOut(standardOut);
    }

}
