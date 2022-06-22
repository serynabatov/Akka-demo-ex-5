package com.example.persistence;

import com.example.message.AvgMessage;
import com.example.message.SimpleMissage;
import com.example.message.StdMessage;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Queue;

public class QueueDoubleState implements Serializable {
    private static final long serialVersionUID = 1L;
    private final HashMap<String, Queue<Double>> events;

    public QueueDoubleState() {
        events = new HashMap<String, Queue<Double>>();
    }

    public QueueDoubleState(HashMap<String, Queue<Double>> events) {
        this.events = events;
    }

    public QueueDoubleState copy() {
        return new QueueDoubleState(new HashMap<String, Queue<Double>>(events));
    }

    public void update(AvgMessage message) {
        events.computeIfAbsent(message.getKey(), k -> new ArrayDeque<>()).add(message.getValue());
    }

    public void update(StdMessage message) {
        events.computeIfAbsent(message.getKey(), k -> new ArrayDeque<>()).add(message.getValue());
    }

    public int size() {
        return events.size();
    }

    public void replace(SimpleMissage message, Queue<Double> values) {
        events.replace(message.getKey(), values);
    }

    public int getSizeByKey(SimpleMissage message) {
        return events.get(message.getKey()).size();
    }

    public Queue<Double> get(String key) {
        return events.get(key);
    }

    @Override
    public String toString() {
        return events.toString();
    }
}
