package com.company;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

public class Simulation {

    private Environment environment;
    private ArrayList<String> orderList;
    private int currentAgentIndex;

    public Simulation() {
        this.environment = new Environment();
        this.orderList = new ArrayList<>();
        this.currentAgentIndex = 0;
        this.setupConfiguration();
    }

    public void setupConfiguration() {
        Collection<Agent> agents = this.environment.initialize();
        this.orderList = agents.stream().map(Agent::getId).collect(Collectors.toCollection(ArrayList::new));
    }

    public void run(int numberOfSteps, boolean display) {
        System.out.println("Running " + numberOfSteps + " steps.");
        for (int i = 1; i <= numberOfSteps; ++i) {
            if (display) {
                System.out.println("Step "+ i +":");
                display();
            } else if (i % 1000000 == 0) {
                System.out.println("Step "+ i +":");
            }

            step();
        }
        System.out.println("Résultat");
        System.out.println(environment.cubeDisplay());
    }

    public void step() {
        // Get the id of next agent turn
        String currentAgentId = orderList.get(currentAgentIndex);
        currentAgentIndex = (currentAgentIndex + 1) % orderList.size();

        // Play agent turn
        environment.updatePerception(currentAgentId);
        environment.makeAction(currentAgentId);
    }

    private void reset() {
        environment = new Environment();
        orderList = new ArrayList<>();
        currentAgentIndex = 0;
        setupConfiguration();
    }

    public void display() {
        System.out.println(environment.toString());
    }

    public Environment.State getEnvironmentState() {
        return environment.getState();
    }
}
