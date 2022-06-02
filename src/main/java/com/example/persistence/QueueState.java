package com.example.persistence;

import com.example.message.SensorDataMessage;
import com.example.message.SimpleMissage;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Queue;

public class QueueState implements Serializable {

    private static final long serialVersionUID = 1L;
    private final HashMap<String, Queue<Integer>> events;

    public QueueState() {
        events = new HashMap<String, Queue<Integer>>();
    }

    public QueueState(HashMap<String, Queue<Integer>> events) {
        this.events = events;
    }

    public QueueState copy() {
        return new QueueState(new HashMap<String, Queue<Integer>>(events));
    }

    public void update(SensorDataMessage message) {
        events.computeIfAbsent(message.getKey(), k -> new ArrayDeque<>()).add(message.getValue());
    }

    public int size() {
        return events.size();
    }

    public void replace(SensorDataMessage message, Queue<Integer> values) {
        events.replace(message.getKey(), values);
    }

    public int getSizeByKey(SensorDataMessage message) {
        return events.get(message.getKey()).size();
    }

    public Queue<Integer> get(String key) {
        return events.get(key);
    }

    @Override
    public String toString() {
        return events.toString();
    }

}
