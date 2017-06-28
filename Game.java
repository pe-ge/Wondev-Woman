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
    char level;

    static HashMap<String, Point> actionToPos; // action -> (x, y)

    static {
        actionToPos = new HashMap<String, Point>();
        actionToPos.put("NW",   new Point(-1, -1));
        actionToPos.put("N",    new Point(0, -1));
        actionToPos.put("NE",   new Point(1, -1));
        actionToPos.put("W",    new Point(-1, 0));
        actionToPos.put("E",    new Point(1, 0));
        actionToPos.put("SW",   new Point(-1, 1));
        actionToPos.put("S",    new Point(0, 1));
        actionToPos.put("SE",   new Point(1, 1));
    }

    public Unit(int x, int y) {
        this.position = new Point(x, y);
    }

    public Unit(int x, int y, char[][] map) {
        this(x, y);
        this.level = map[y][x];
    }

    public Unit(Unit other) {
        this(other.position.x, other.position.y);
        this.level = other.level;
    }

    public void moveAndBuild(Action action, char[][] map) {
        move(action.move);
        build(action.build, map);
    }

    public void pushAndBuild(Action action, Unit[] pushedUnits, char[][] map) {
        push(action.move, pushedUnits);
        build(action.build, map);
    }

    public void push(String push, Unit[] pushedUnits) {
        Point direction = actionToPos.get(push);
        int x = position.x + direction.x;
        int y = position.y + direction.y;

        for (Unit unit : pushedUnits) {
            if (unit.position.x == x && unit.position.y == y) {
                // we found unit to be pushed
                unit.position.x += direction.x;
                unit.position.y += direction.y;
                return;
            }
        }

        throw new RuntimeException("SHOULD NOT BE REACHED!!! DONT MAKE BUGS PLS");
    }

    public void move(String move) {
        Point direction = actionToPos.get(move);
        position.x += direction.x;
        position.y += direction.y;
    }

    public void build(String build, char[][] map) {
        Point direction = actionToPos.get(build);
        map[position.y + direction.y][position.x + direction.x]++;
    }
}

class Action {
    String atype;
    int index;
    String move;
    String build;

    // when action executed
    Point position;
    int level;

    public Action(String atype, int index, String move, String build) {
        this.atype = atype;
        this.index = index;
        this.move = move;
        this.build = build;
    }

    public String toString() {
        return String.format("%s %d %s %s", atype, index, move, build);
    }
}

class State {
    static int size;
    static int unitsPerGame;

    char[][] map;

    Unit[] myUnits;
    Unit[] enemyUnits;

    boolean myUnitTurn;

    public State(boolean myUnitTurn) {
        map = new char[size][size];
        this.myUnitTurn = myUnitTurn;
    }

    public void readState(Scanner input) {
        for (int i = 0; i < size; i++) {
            String row = input.next();
            map[i] = row.toCharArray();
        }

        myUnits = new Unit[unitsPerGame];
        for (int i = 0; i < unitsPerGame; i++) {
            myUnits[i] = new Unit(input.nextInt(), input.nextInt(), map);
        }
        enemyUnits = new Unit[unitsPerGame];
        for (int i = 0; i < unitsPerGame; i++) {
            enemyUnits[i] = new Unit(input.nextInt(), input.nextInt(), map);
        }
        int legalActions = input.nextInt();
        for (int i = 0; i < legalActions; i++) {
            Action a = new Action(
                input.next(), // atype
                input.nextInt(), // index
                input.next(), // move
                input.next()
            ); // build
            System.err.println(a);
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

    public State copyState() {
        State copied = new State(this.myUnitTurn);
        copied.map = new char[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                copied.map[i][j] = map[i][j];
            }
        }

        copied.myUnits = new Unit[unitsPerGame];
        copied.enemyUnits = new Unit[unitsPerGame];
        for (int i = 0; i < unitsPerGame; i++) {
            copied.myUnits[i] = new Unit(myUnits[i]);
            copied.enemyUnits[i] = new Unit(enemyUnits[i]);

        }
        return copied;
    }

    public State executeAction(Action action) {
        Unit executingUnit = myUnitTurn ? myUnits[action.index] : enemyUnits[action.index];
        switch (action.atype) {
            case "MOVE&BUILD":
                executingUnit.moveAndBuild(action, map);
                break;
            case "PUSH&BUILD":
                Unit[] otherUnits = myUnitTurn ? enemyUnits : myUnits;
                executingUnit.pushAndBuild(action, otherUnits, map);
                break;
        }
        return this;
    }
}

class Game {

    Scanner input;

    State state;

    ArrayList<String> directions;

    Random random;

    public Game() {
        input = new Scanner(System.in);

        State.size = input.nextInt();
        State.unitsPerGame = input.nextInt();

        state = new State(true);

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

        if (x < 0 || y < 0 || x >= State.size || y >= State.size) {
            return false;
        }
        // -2 == '.'
        return state.map[y][x] != -2 && state.map[y][x] != 4 && state.map[y][x] <= 10;
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
                bestAction.move = direction;
                bestAction.level = nextPositionLevel;
                bestAction.position = nextPosition;
                // System.err.println(nextPosition);
                return bestAction;
            }
            if (nextPositionLevel >= bestAction.level && nextPositionLevel <= state.myUnits[0].level + 1) {
                bestAction.move = direction;
                bestAction.level = nextPositionLevel;
                bestAction.position = nextPosition;
            }

        }

        if (bestAction.move == null) {
            System.err.println("No possible action found");
            // int randomIdx = random.nextInt(directions.size());
            // bestAction.move = directions.get(randomIdx);
            // bestAction.position = next(state.myUnits[0].position, bestAction.move);
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
                action.build = direction;
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
        Game Game = new Game();
        Game.mainLoop();
    }
}
