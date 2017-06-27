import java.util.*;
import java.io.*;
import java.math.*;

class Point {
    int x, y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public String toString() {
        return String.format("(x=%d, y=%d)", x, y);
    }

    public boolean equals(Point other) {
        return this.x == other.x && this.y == other.y;
    }
}

class Unit {
    Point position;
    int level;

    public Unit(int x, int y, int[][] map) {
        this.position = new Point(x, y);
        this.level = map[y][x];
    }
}

class Action {
    String atype;
    int index;
    String dir1;
    String dir2;

    // when action executed
    Point position;
    int level;

    public Action(String atype, int index, String dir1, String dir2) {
        this.atype = atype;
        this.index = index;
        this.dir1 = dir1;
        this.dir2 = dir2;
    }

    public String toString() {
        return String.format("%s %d %s %s", atype, index, dir1, dir2);
    }
}

class State {
    int size;
    int unitsPerPlayer;

    int[][] map;

    Unit[] myUnits;
    Unit[] otherUnits;

    public State(int size, int unitsPerPlayer) {
        this.size = size;
        this.unitsPerPlayer = unitsPerPlayer;

        map = new int[size][size];
    }

    public void readState(Scanner input) {
        for (int i = 0; i < size; i++) {
            String row = input.next();
            for (int j = 0; j < size; j++) {
                map[i][j] = row.charAt(j) - '0';
            }
        }

        myUnits = new Unit[unitsPerPlayer];
        for (int i = 0; i < unitsPerPlayer; i++) {
            myUnits[i] = new Unit(input.nextInt(), input.nextInt(), map);
            map[myUnits[i].position.y][myUnits[i].position.x] += 10;
        }
        otherUnits = new Unit[unitsPerPlayer];
        for (int i = 0; i < unitsPerPlayer; i++) {
            otherUnits[i] = new Unit(input.nextInt(), input.nextInt(), map);
            map[otherUnits[i].position.y][otherUnits[i].position.x] += 20;
        }
        int legalActions = input.nextInt();
        for (int i = 0; i < legalActions; i++) {
            input.next(); // atype
            input.nextInt(); // index
            input.next(); // dir1
            input.next(); // dir2
        }
    }

    public void printMap() {
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                System.err.print(String.format("%02d ", map[i][j]));
            }
            System.err.println();
        }
    }

}

class Player {

    Scanner input;

    int size;
    int unitsPerPlayer;

    State state;

    HashMap<String, Point> actionToPos; // action -> (x, y)
    ArrayList<String> directions;

    Random random;

    public Player() {
        input = new Scanner(System.in);

        size = input.nextInt();
        unitsPerPlayer = input.nextInt();

        state = new State(size, unitsPerPlayer);

        actionToPos = new HashMap<String, Point>();
        actionToPos.put("NW",   new Point(-1, -1));
        actionToPos.put("N",    new Point(0, -1));
        actionToPos.put("NE",   new Point(1, -1));
        actionToPos.put("W",    new Point(-1, 0));
        actionToPos.put("E",    new Point(1, 0));
        actionToPos.put("SW",   new Point(-1, 1));
        actionToPos.put("S",    new Point(0, 1));
        actionToPos.put("SE",   new Point(1, 1));

        directions = new ArrayList<String>();
        directions.add("NW");
        directions.add("N");
        directions.add("NE");
        directions.add("W");
        directions.add("E");
        directions.add("SW");
        directions.add("S");
        directions.add("SE");

        random = new Random();
    }

    public boolean checkPosition(Point position) {
        int x = position.x;
        int y = position.y;

        if (x < 0 || y < 0 || x >= size || y >= size) {
            return false;
        }
        // -2 == '.'
        return state.map[y][x] != -2 && state.map[y][x] != 4 && state.map[y][x] <= 10;
    }

    public Point next(Point position, String moveAction) {
        Point direction = actionToPos.get(moveAction);
        return new Point(position.x + direction.x, position.y + direction.y);
    }

    public Action findMove() {
        Action bestAction = new Action("MOVE&BUILD", 0, null, null);
        for (String direction : directions) {
            // System.err.println(action);
            Point nextPosition = next(state.myUnits[0].position, direction);

            if (!checkPosition(nextPosition)) {
                continue;
            }
            int nextPositionLevel = state.map[nextPosition.y][nextPosition.x];
            if (nextPositionLevel == state.myUnits[0].level + 1) {
                bestAction.dir1 = direction;
                bestAction.level = nextPositionLevel;
                bestAction.position = nextPosition;
                // System.err.println(nextPosition);
                return bestAction;
            }
            if (nextPositionLevel >= bestAction.level && nextPositionLevel <= state.myUnits[0].level + 1) {
                bestAction.dir1 = direction;
                bestAction.level = nextPositionLevel;
                bestAction.position = nextPosition;
            }

        }

        if (bestAction.dir1 == null) {
            System.err.println("No possible action found");
            // int randomIdx = random.nextInt(directions.size());
            // bestAction.dir1 = directions.get(randomIdx);
            // bestAction.position = next(state.myUnits[0].position, bestAction.dir1);
            // bestAction.level = state.map[bestAction.position.y][bestAction.position.x];
        }

        return bestAction;
    }

    public Action findBuild(Action action) {
        Point nextPosition = action.position;
        int maxLevel = 0;
        for (String direction : directions) {
            Point buildPosition = next(nextPosition, direction);
            if (!checkPosition(buildPosition)) {
                continue;
            }
            int buildPositionLevel = state.map[buildPosition.y][buildPosition.x];
            if (buildPositionLevel >= maxLevel && buildPositionLevel <= action.level) {
                maxLevel = buildPositionLevel;
                action.dir2 = direction;
            }
        }
        return action;
    }

    public void mainLoop() {
        while (true) {
            state.readState(input);
            state.printMap();
            Action action = findMove();
            action = findBuild(action);
            System.out.println(action);
        }
    }

    public static void main(String args[]) {
        Player player = new Player();
        player.mainLoop();
    }
}
