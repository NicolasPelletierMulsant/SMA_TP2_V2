package com.company;

import java.awt.Point;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Environment {

    private static final int GRID_SIZE = 20; // 50
    private static final int NB_AGENTS = 10; // 20
    private static final HashMap<Cube, Integer> NB_CUBES = new HashMap<Cube, Integer>() {{
        put(Cube.A, 20);
        put(Cube.B, 20);
        put(Cube.C, 20);
    }};

    private HashMap<String, Agent> agents;
    private HashMap<String, AgentInfo> agentInfos;
    public class AgentInfo {
        private Point pos;
        private Cube carrying;

        private String following;
        private boolean superPowered;

        public AgentInfo(Point pos, Cube carrying) {
            this.pos = pos;
            this.carrying = carrying;
            this.following = null;
            this.superPowered = false;
        }

        public Point getPos() {
            return pos;
        }

        public void setPos(Point pos) {
            this.pos = pos;
        }

        public String getFollowing() {
            return following;
        }

        public boolean isSuperPowered() {
            return superPowered;
        }

        public Cube getCarrying() {
            return carrying;
        }

        public void setCarrying(Cube carrying) {
            this.carrying = carrying;
        }

        public void setFollowing(String following) {
            this.following = following;
        }

        public void setSuperPowered(boolean superPowered) {
            this.superPowered = superPowered;
        }
    }
    // Liste permettant de savoir si un agent est en train d'émettre sur la case
    private HashMap<Point, String> emittingAgents;

    private Cube[][] grid;
    private Pheromone[][] pheromones;

    private State state;

    // == Init ==

    public Environment() {
        this.grid = new Cube[GRID_SIZE][GRID_SIZE];
        for (int i = 0; i < GRID_SIZE; i++) {
            this.grid[i] = new Cube[GRID_SIZE];
        }

        this.pheromones = new Pheromone[GRID_SIZE][GRID_SIZE];
        for (int i = 0; i < GRID_SIZE; i++) {
            this.pheromones[i] = new Pheromone[GRID_SIZE];
        }

        this.agents = new HashMap<>();
        this.agentInfos = new HashMap<>();
        this.emittingAgents = new HashMap<>();

        this.state = new State(GRID_SIZE, agentInfos, grid, pheromones);
    }

    public Collection<Agent> initialize() {
        // Reset grid
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                grid[i][j] = null;
            }
        }

        for (Cube cubeType: Cube.values()) {
            int cubeNumber = NB_CUBES.get(cubeType);
            for (int i = 0; i < cubeNumber; i++) {
                setRandomEmptyCell(cubeType);
            }
        }

        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                pheromones[i][j] = new Pheromone();
            }
        }

        // Reset agents
        agents = new HashMap<>();
        agentInfos = new HashMap<>();

        for (int i = 0; i < NB_AGENTS; i++) {
            Agent agent = new Agent(String.valueOf(i));
            agents.put(agent.getId(), agent);
            agentInfos.put(agent.getId(), new AgentInfo(getRandomCell(), null));
        }


        this.state = new State(GRID_SIZE, agentInfos, grid, pheromones);
        return agents.values();
    }

    // == Tour par tour ==

    public void updatePerception(String agentId) {
        Agent agent = agents.get(agentId);
        AgentInfo infos = agentInfos.get(agentId);

        // Mise à jour des phéromones
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                pheromones[i][j].evaporate(Agent.PHEROMONE_EVAPORATION);
            }
        }

        // Délier le follower si le maître ne porte pas de cube
        if (infos.getFollowing() != null) {
            AgentInfo masterInfos = agentInfos.get(infos.getFollowing());
            if (masterInfos.getCarrying() == null) {
                infos.setFollowing(null);
                infos.setPos(masterInfos.getPos());
                masterInfos.setSuperPowered(false);
            }
        }

        Cube detected = getFromGrid(infos.getPos());
        Cube carrying = infos.getCarrying();

        boolean canFollow = false;
        String emitter = emittingAgents.get(infos.getPos());
        if (emitter != null) {
            if (emitter.equals(agent.getId())) {
                // Enlever le mode émission
                emittingAgents.put(infos.getPos(), null);
            } else {
                // Un autre agent est en train d'émettre sur la case
                canFollow = true;
            }
        }

        agent.updatePerception(detected, carrying, canFollow, infos.isSuperPowered());
    }

    public void makeAction(String agentId) {
        Agent agent = agents.get(agentId);
        AgentInfo infos = agentInfos.get(agentId);

        if (infos.getFollowing() != null) {
            // Les followers ne font pas d'action
            return;
        }

        switch (agent.queryAction()) {
            case MOVE:
                // Se déplacer de "i" pas
                for (int i = 0; i < agent.getStepSize(); i++) {
                    infos.setPos(getRandomNeighbor(infos.getPos()));
                }
                break;

            case DROP:
                if (infos.getCarrying() == null) {
                    System.out.println("Utilisation de DROP sans cube");
                    break;
                }
                if (getFromGrid(infos.getPos()) != null) {
                    System.out.println("Utilisation de DROP sur une case non vide");
                    break;
                }
                setOnGrid(infos.getPos(), infos.getCarrying());
                infos.setCarrying(null);
                break;

            case TAKE:
                if (infos.getCarrying() != null) {
                    System.out.println("Utilisation de TAKE avec un cube dans les mains");
                    break;
                }
                if (getFromGrid(infos.getPos()) == null) {
                    System.out.println("Utilisation de TAKE sur une case vide");
                    break;
                }

                Cube target = getFromGrid(infos.getPos());
                if (target.equals(Cube.C) && !infos.isSuperPowered()) {
                    System.out.println("Utilisation de TAKE sur un cube lourd sans aide");
                    break;
                }

                infos.setCarrying(target);
                setOnGrid(infos.getPos(), null);
                break;

            case EMIT:
                emittingAgents.put(infos.getPos(), agent.getId());
                putPheromones(agent.getId(), infos.getPos(), Agent.PHEROMONE_INTENSITY, Agent.PHEROMONE_DISPERSION);
                break;

            case FOLLOW:
                String emitterId = emittingAgents.get(infos.getPos());
                if (emitterId == null) {
                    System.out.println("Utilisation de FOLLOW sans agent disponible");
                    break;
                }

                if (infos.getCarrying() != null) {
                    System.out.println("Utilisation de FOLLOW en portant un cube");
                    break;
                }

                infos.following = emitterId;
                agentInfos.get(emitterId).setSuperPowered(true);
                break;

            case TRACK:
                // Traquer sur "i" pas
                for (int i = 0; i < agent.getStepSize(); i++) {
                    int x = (int)infos.getPos().getX();
                    int y = (int)infos.getPos().getY();

                    Point max = infos.getPos().getLocation();
                    double maxValue = 0;

                    for (int xDelta = -1; xDelta < 2; xDelta++) {
                        for (int yDelta = -1; yDelta < 2; yDelta++) {
                            if (isNeighborInvalid(infos.getPos(), xDelta, yDelta)) {
                                continue;
                            }

                            Pheromone current = pheromones[x + xDelta][y + yDelta];

                            // Empêcher l'agent de suivre ses propres phéromones
                            if (current.getValue() == 0 || current.getEmitter().equals(agent.getId())) {
                                continue;
                            }

                            if (pheromones[x + xDelta][y + yDelta].getValue() > maxValue) {
                                max.setLocation(x + xDelta, y + yDelta);
                                maxValue = pheromones[x + xDelta][y + yDelta].getValue();
                            }
                        }
                    }

                    if (maxValue == 0) {
                        // Pas de phéromones dans les environs
                        infos.setPos(getRandomNeighbor(infos.getPos()));
                    } else {
                        // On remarque que si la case de l'agent contient plus de phéromones que les cases autour,
                        // il ne bouge pas
                        infos.setPos(max);
                    }
                }
                break;
        }
    }

    // == Helpers ==

    private void setRandomEmptyCell(Cube cube) {
        Point random;

        do {
            random = getRandomCell();
        } while (getFromGrid(random) != null);

        setOnGrid(random, cube);
    }

    private Point getRandomCell() {
        int x = ThreadLocalRandom.current().nextInt(GRID_SIZE);
        int y = ThreadLocalRandom.current().nextInt(GRID_SIZE);
        return new Point(x, y);
    }

    private Point getRandomNeighbor(Point current) {
        int x, y;
        do {
            x = ThreadLocalRandom.current().nextInt(-1, 2);
            y = ThreadLocalRandom.current().nextInt(-1, 2);
        } while (isNeighborInvalid(current, x, y));
        return new Point(current.x + x, current.y + y);
    }

    private Cube getFromGrid(Point pos) {
        return grid[pos.x][pos.y];
    }

    public void setOnGrid(Point pos, Cube cube) {
        grid[pos.x][pos.y] = cube;
    }

    public void putPheromones(String emitter, Point pos, int intensity, int maxDispersion) {
        Set<Point> visited = new HashSet<>();
        Queue<Point> queue = new LinkedList<>();
        HashMap<Point, Integer> dispersion = new HashMap<>();

        visited.add(pos);
        queue.add(pos);
        dispersion.put(pos, 0);

        int[][] neighborDeltas = {
            {0, -1},
            {0, 1},
            {-1, 0},
            {1, 0}
        };

        while (queue.size() != 0) {
            pos = queue.poll();
            int currentDispersion = dispersion.get(pos);

            double attenuation = 1 - ((double)currentDispersion / maxDispersion);
            pheromones[pos.x][pos.y].set(emitter, attenuation * intensity);

            if (currentDispersion + 1 == maxDispersion) {
                continue;
            }

            for (int[] neighborDelta: neighborDeltas) {
                if (isNeighborInvalid(pos, neighborDelta[0], neighborDelta[1])) {
                    continue;
                }

                Point neighbor = pos.getLocation();
                neighbor.translate(neighborDelta[0], neighborDelta[1]);

                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                    dispersion.put(neighbor, currentDispersion + 1);
                }
            }
        }
    }

    public boolean isNeighborInvalid(Point current, int xDelta, int yDelta) {
        return (current.x + xDelta < 0 ||
                current.x + xDelta >= GRID_SIZE ||
                current.y + yDelta < 0 ||
                current.y + yDelta >= GRID_SIZE ||
                (xDelta == 0 && yDelta == 0));
    }

    public String cubeDisplay() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                Cube current = grid[i][j];
                if (current == null) {
                    sb.append(".");
                } else {
                    switch (current) {
                        case A:
                            sb.append("A");
                            break;
                        case B:
                            sb.append("B");
                            break;
                        case C:
                            sb.append("C");
                            break;
                    }
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        // Affichage des cubes + des agents
        char[] result = cubeDisplay().toCharArray();
        for (AgentInfo info: agentInfos.values()) {
            if (info.getFollowing() == null) {
                result[info.getPos().x * (GRID_SIZE + 1) + info.getPos().y] = 'X';
            }
        }
        return new String(result);
    }

    // == Export ==
    public class State {
        private int GRID_SIZE;
        private HashMap<String, AgentInfo> agentInfos;
        private Cube[][] grid;
        private Pheromone[][] pheromones;

        public State(int GRID_SIZE, HashMap<String, AgentInfo> agentInfos, Cube[][] grid, Pheromone[][] pheromones) {
            this.GRID_SIZE = GRID_SIZE;
            this.agentInfos = agentInfos;
            this.grid = grid;
            this.pheromones = pheromones;
        }

        public int getGRID_SIZE() {
            return GRID_SIZE;
        }

        public HashMap<String, AgentInfo> getAgentInfos() {
            return agentInfos;
        }

        public Cube[][] getGrid() {
            return grid;
        }

        public Pheromone[][] getPheromones() {
            return pheromones;
        }
    }

    public State getState() {
        return state;
    }
}
