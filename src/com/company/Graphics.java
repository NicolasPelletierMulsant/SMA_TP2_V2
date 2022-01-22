package com.company;

import processing.core.PApplet;

import java.util.HashMap;

public class Graphics extends PApplet {
    private final int SIZE = 600;

    private Simulation sim;

    private int speed;
    private int lastRender;

    private final int GRID_SIZE;
    private final int CELL_SIZE;

    private final float AGENT_SIZE;
    private final float CUBE_SIZE;

    public HashMap<Cube, Integer> CUBE_COLORS;

    public Graphics(Simulation sim, int speed) {
        super();

        this.sim = sim;

        this.speed = speed;
        this.lastRender = 0;

        this.GRID_SIZE = sim.getEnvironmentState().getGRID_SIZE();
        this.CELL_SIZE = (SIZE - (this.GRID_SIZE + 1)) / this.GRID_SIZE;

        this.AGENT_SIZE = (float)0.6 * CELL_SIZE;
        this.CUBE_SIZE = (float)0.8 * CELL_SIZE;

        this.CUBE_COLORS = new HashMap<>();
        this.CUBE_COLORS.put(Cube.A, color(255, 0, 0));
        this.CUBE_COLORS.put(Cube.B, color(0, 255, 0));
        this.CUBE_COLORS.put(Cube.C, color(0, 0, 255));
    }

    public void settings(){
        size(SIZE, SIZE);
    }

    public void setup() {
        rectMode(CENTER);
        color(0);
    }

    public void draw(){
        if (millis() - lastRender < speed) {
            return;
        }
        lastRender = millis();

        sim.step();

        Environment.State state = sim.getEnvironmentState();

        background(255);

        // Cases
        strokeWeight(1);
        stroke(color(0));
        fill(color(0));
        int realSize = GRID_SIZE * (CELL_SIZE + 1);
        for (int i = 0; i <= GRID_SIZE; i++) {
            line(0, i * (CELL_SIZE + 1), realSize, i * (CELL_SIZE + 1));
            line(i * (CELL_SIZE + 1), 0, i * (CELL_SIZE + 1), realSize);
        }

        // Phéromones
        noStroke();
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                Pheromone current = state.getPheromones()[i][j];
                if (current != null) {
                    colorMode(HSB, 360, 100, 100);
                    fill(color(244, (int)(current.getValue() / 10 * 100), 100));
                    colorMode(RGB, 255, 255, 255);
                    square(indexToCoordinate(i), indexToCoordinate(j), CELL_SIZE);
                }
            }
        }

        // Cubes
        noStroke();
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                Cube current = state.getGrid()[i][j];
                if (current != null) {
                    fill(CUBE_COLORS.get(current));
                    square(indexToCoordinate(i), indexToCoordinate(j), CUBE_SIZE);
                }
            }
        }

        // Agents#
        strokeWeight(1);
        stroke(color(0));
        for (Environment.AgentInfo info: state.getAgentInfos().values()) {
            Cube carrying = info.getCarrying();
            if (carrying == null) {
                fill(color(0));
            } else {
                fill(CUBE_COLORS.get(carrying));
            }
            circle(indexToCoordinate((int)info.getPos().getX()), indexToCoordinate((int)info.getPos().getY()), AGENT_SIZE);
        }
    }

    public int indexToCoordinate(int index) {
        return index * (CELL_SIZE + 1) + (CELL_SIZE / 2) + 1;
    }
}
