package com.company;

import processing.core.PApplet;

public class Main {

    public static void main(String[] args) {
        Simulation sim = new Simulation();

        // Graphic mode
        Graphics graphics = new Graphics(sim, 0);
        String[] processingArgs = {"Graphics"};
        PApplet.runSketch(processingArgs, graphics);

        // Non graphic mode
//        sim.run(1000000, false);

    }
}
