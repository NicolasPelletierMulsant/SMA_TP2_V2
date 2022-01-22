package com.company;

public class Pheromone {
    private String emitter;
    private double value;

    public Pheromone() {
        this.emitter = null;
        this.value = 0;
    }

    public void set(String emitter, double value) {
        this.emitter = emitter;
        this.value = value;
    }

    public void evaporate(double rate) {
        value -= rate;
        if (value < 0) {
            value = 0;
        }
        if (value == 0) {
            this.emitter = null;
        }
    }

    public String getEmitter() {
        return emitter;
    }

    public double getValue() {
        return value;
    }
}
