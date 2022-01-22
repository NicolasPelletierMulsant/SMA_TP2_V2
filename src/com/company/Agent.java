package com.company;

import java.util.concurrent.ThreadLocalRandom;

public class Agent {

    private final String id;

    private final Perception perception;
    private final Memory memory;

    private static final double K_PLUS = 0.1;
    private static final double K_MINUS = 0.3;
    private static final double STEP_SIZE = 1; // i
    private static final double PERCEPTION_SIZE = 10; // t

    public static final int PHEROMONE_INTENSITY = 10; // S
    public static final int PHEROMONE_DISPERSION = 7; // ds
    public static final double PHEROMONE_EVAPORATION = 0.1; // r

    public Agent(String id) {
        this.id = id;

        this.perception = new Perception();
        this.memory = new Memory();
    }

    public void updatePerception(Cube detected, Cube carrying, boolean canFollow, boolean isSuperPowered) {
        perception.update(detected, carrying, canFollow, isSuperPowered);
        memory.add(perception.getDetected());
    }

    public Action queryAction() {
        // nombre aléatoire entre 0 et 1
        double random = ThreadLocalRandom.current().nextDouble(1);

        // Toujours aider si c'est possible
        if (perception.getCarrying() == null && perception.canFollow()) {
            return Action.FOLLOW;
        }

        // Poser un cube
        if (perception.getCarrying() != null && perception.getDetected() == null) {
            double f = memory.numberOf(perception.getCarrying());
            f /= PERCEPTION_SIZE;

            double formula = f / (K_MINUS + f);
            formula = formula * formula;

            if (random < formula) {
                return Action.DROP;
            }
        }

        // Prendre un cube
        if (perception.getCarrying() == null && perception.getDetected() != null) {
            double f = memory.numberOf(perception.getDetected());
            f /= PERCEPTION_SIZE;

            double formula = K_PLUS / (K_PLUS + f);
            formula = formula * formula;

            if (random < formula) {
                if (perception.getDetected().equals(Cube.C)) {
                    if (perception.isSuperPowered()) {
                        return Action.TAKE;
                    } else {
                        return Action.EMIT;
                    }
                } else {
                    return Action.TAKE;
                }
            }
        }

        if (perception.getCarrying() != null) {
            return Action.MOVE;
        } else {
            return Action.TRACK;
        }

    }

    public String getId() {
        return id;
    }

    public double getStepSize() {
        return STEP_SIZE;
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof Agent)) {
            return false;
        }

        Agent a = (Agent) o;

        return id.equals(a.id);
    }

    private class Perception {

        private Cube detected;
        private Cube carrying;
        private boolean canFollow;
        private boolean isSuperPowered;

        public Perception() {
            this.detected = null;
            this.carrying = null;
            this.canFollow = false;
            this.isSuperPowered = false;
        }

        public void update(Cube detected, Cube carrying, boolean canFollow, boolean isSuperPowered) {
            this.detected = detected;
            this.carrying = carrying;
            this.canFollow = canFollow;
            this.isSuperPowered = isSuperPowered;
        }

        public Cube getDetected() {
            return detected;
        }

        public Cube getCarrying() {
            return carrying;
        }

        public boolean canFollow() {
            return canFollow;
        }

        public boolean isSuperPowered() {
            return isSuperPowered;
        }
    }

    private class Memory {
        private String memory;

        public void add(Cube cube) {
            if (cube == null) {
                memory = "0" + memory;
            } else {
                memory = cube.name() + memory;
            }

            if (memory.length() > 10) {
                memory = memory.substring(0, 10);
            }
        }

        public double numberOf(Cube cube) {
            String searchPattern = "0";
            if (cube != null) {
                searchPattern = cube.name();
            }

            // Compte le nombre d'occurrences de searchPattern
            return (memory + " ").split(searchPattern).length - 1;
        }
    }

    public enum Action {
        MOVE,
        TAKE,
        DROP,
        EMIT,
        FOLLOW,
        TRACK,
    }
}
