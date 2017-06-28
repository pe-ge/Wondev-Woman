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
    static HashMap<String, String[]> pushToDirections;

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

        pushToDirections = new HashMap<String, String[]>();
        pushToDirections.put("NW",  new String[]{"N", "NW", "W"});
        pushToDirections.put("N",  new String[]{"N", "NW", "NE"});
        pushToDirections.put("NE",  new String[]{"N", "NE", "E"});
        pushToDirections.put("W",  new String[]{"NW", "SW", "W"});
        pushToDirections.put("E",  new String[]{"NE", "SE", "E"});
        pushToDirections.put("SW",  new String[]{"W", "SW", "S"});
        pushToDirections.put("S",  new String[]{"SW", "S", "SE"});
        pushToDirections.put("SE",  new String[]{"S", "SE", "E"});
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

    public String toString() {
        return position.toString();
    }

    public void moveAndBuild(Action action, char[][] map) {
        move(action.move);
        build(action.build, map);
    }

    public void pushAndBuild(Action action, Unit[] pushedUnits, char[][] map) {
        push(action.move, action.build, pushedUnits);
        build(action.move, map);
    }

    public void push(String push, String where, Unit[] pushedUnits) {
        Point pushDirection = actionToPos.get(push);
        int x = position.x + pushDirection.x;
        int y = position.y + pushDirection.y;

        for (Unit unit : pushedUnits) {
            if (unit.position.x == x && unit.position.y == y) {
                // we found unit to be pushed
                Point whereDirection = actionToPos.get(where);
                unit.position.x += whereDirection.x;
                unit.position.y += whereDirection.y;
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

    int myScore, enemyScore;

    static ArrayList<String> directions;

    static {
        directions = new ArrayList<String>();
        directions.add("NW");
        directions.add("N");
        directions.add("NE");
        directions.add("W");
        directions.add("E");
        directions.add("SW");
        directions.add("S");
        directions.add("SE");
    }

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
        System.err.print(" ");
        for (int i = 0; i < map.length; i++) {
            System.err.print(i);
        }
        System.err.println();
        for (int i = 0; i < map.length; i++) {
            System.err.print(i);
            for (int j = 0; j < map[i].length; j++) {
                System.err.print(map[i][j]);
            }
            System.err.println();
        }
        for (int i = 0; i < unitsPerGame; i++) {
            System.err.println("My unit: " + myUnits[i]);
            System.err.println("Enemy unit: " + enemyUnits[i]);
        }
    }

    public State copyState(boolean switchTurn) {
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
        copied.myUnitTurn = switchTurn ? !myUnitTurn : myUnitTurn;

        copied.myScore = myScore;
        copied.enemyScore = enemyScore;

        return copied;
    }

    public State executeAction(Action action) {
        Unit executingUnit = myUnitTurn ? myUnits[action.index] : enemyUnits[action.index];
        switch (action.atype) {
            case "MOVE&BUILD":
                executingUnit.moveAndBuild(action, map);
                if (map[executingUnit.position.y][executingUnit.position.x] == '3') {
                    if (myUnitTurn) {
                        myScore++;
                    } else {
                        enemyScore++;
                    }
                }
                break;
            case "PUSH&BUILD":
                Unit[] otherUnits = myUnitTurn ? enemyUnits : myUnits;
                executingUnit.pushAndBuild(action, otherUnits, map);
                break;
        }
        return this;
    }

    public Point canMove(String move, Point position, Unit[] units, Unit[] otherUnits, char[][] map) {
        Point moveTo = canBuild(move, position, units, otherUnits, map);
        if (moveTo == null) {
            return null;
        }

        char currentLevel = map[position.y][position.x];
        char moveToLevel = map[moveTo.y][moveTo.x];
        if (currentLevel + 1 < moveToLevel) {
            return null;
        }

        return moveTo;
    }

    public Point canBuild(String move, Point position, Unit[] units, Unit[] otherUnits, char[][] map) {
        Point direction = Unit.actionToPos.get(move);
        int x = position.x + direction.x;
        int y = position.y + direction.y;

        if (!legalCell(x, y, map)) {
            return null;
        }

        for (Unit unit : units) {
            if (unit.position.x == x && unit.position.y == y) {
                return null;
            }
        }
        for (Unit unit : otherUnits) {
            if (unit.position.x == x && unit.position.y == y) {
                return null;
            }
        }
        return new Point(x, y);
    }

    public boolean legalCell(int x, int y, char[][] map) {
        if (x < 0 || y < 0 || x >= State.size || y >= State.size) {
            return false;
        }
        if (map[y][x] == '.' || map[y][x] == '4') {
            return false;
        }

        return true;
    }

    public Unit getNeighbourUnit(Unit myUnit, String direction, Unit[] units, char[][] map) {
        Point directionPoint = Unit.actionToPos.get(direction);
        int x = myUnit.position.x + directionPoint.x;
        int y = myUnit.position.y + directionPoint.y;

        if (!legalCell(x, y, map)) {
            return null;
        }

        for (Unit unit : units) {
            if (unit.position.x == x && unit.position.y == y) {
                return unit;
            }
        }

        return null;
    }

    // public Unit canPush(String push, Point position, Unit[] units, Unit[] otherUnits, char[][] map) {
        // Point direction = Unit.actionToPos.get(push);
        // int x = position.x + direction.x;
        // int y = position.y + direction.y;

        // if (!legalCell(x, y, map)) {
            // return null;
        // }

        // for (Unit unit : otherUnits) {
            // if (unit.position.x == x && unit.position.y == y) {
                // int pushedX = x + direction.x;
                // int pushedY = y + direction.y;
                // if (legalCell(pushedX, pushedY, map)) {
                    // return unit;
                // }
                // return null;
            // }
        // }
        // return null;
    // }

    public ArrayList<Action> legalActions() {
        ArrayList<Action> legalActions = new ArrayList<Action>();
        Unit[] units = myUnitTurn ? myUnits : enemyUnits;
        Unit[] otherUnits = myUnitTurn ? enemyUnits : myUnits;

        for (int i = 0; i < unitsPerGame; i++) {
            Unit unit = units[i];
            // MOVE&BUILD
            // first move
            for (String moveDirection : directions) {
                Point movePoint = canMove(moveDirection, unit.position, units, otherUnits, map);
                if (movePoint != null) {
                    // then build
                    int tmpX = unit.position.x;
                    int tmpY = unit.position.y;
                    unit.position.x = -1;
                    unit.position.y = -1;
                    for (String buildDirection : directions) {
                        Point buildPoint = canBuild(buildDirection, movePoint, units, otherUnits, map);
                        if (buildPoint != null) {
                            Action action = new Action("MOVE&BUILD", i, moveDirection, buildDirection);
                            legalActions.add(action);
                        }
                    }
                    unit.position.x = tmpX;
                    unit.position.y = tmpY;
                }
            }

            // PUSH&BUILD
            // push
            for (String pushDirection : directions) {
                Unit pushedUnit = getNeighbourUnit(unit, pushDirection, otherUnits, map);
                if (pushedUnit != null) {
                    // System.err.println("mozem tiskat " + pushDirection);
                    for (String moveDirection : Unit.pushToDirections.get(pushDirection)) {
                        // check whether pushedTo position is free
                        Point pushedTo = canMove(moveDirection, pushedUnit.position, units, otherUnits, map);
                        if (pushedTo != null) {
                            Action action = new Action("PUSH&BUILD", i, pushDirection, moveDirection);
                            legalActions.add(action);
                        }
                    }
                }
            }
        }
        return legalActions;
    }
}

class Game {

    Scanner input;

    State state;

    Random random;

    public Game() {
        input = new Scanner(System.in);

        State.size = input.nextInt();
        State.unitsPerGame = input.nextInt();

        state = new State(true);

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

    // public Action findMove() {
        // Action bestAction = new Action("MOVE&BUILD", 0, null, null);
        // for (String direction : directions) {
            // // System.err.println(action);
            // Point nextPosition = next(state.myUnits[0].position, direction);

            // if (!checkPosition(nextPosition)) {
                // continue;
            // }
            // int nextPositionLevel = state.map[nextPosition.y][nextPosition.x];
            // if (nextPositionLevel == state.myUnits[0].level + 1) {
                // bestAction.move = direction;
                // bestAction.level = nextPositionLevel;
                // bestAction.position = nextPosition;
                // // System.err.println(nextPosition);
                // return bestAction;
            // }
            // if (nextPositionLevel >= bestAction.level && nextPositionLevel <= state.myUnits[0].level + 1) {
                // bestAction.move = direction;
                // bestAction.level = nextPositionLevel;
                // bestAction.position = nextPosition;
            // }

        // }

        // if (bestAction.move == null) {
            // System.err.println("No possible action found");
        // }

        // return bestAction;
    // }

    // public Action findBuild(Action action) {
        // Point nextPosition = action.position;
        // int maxLevel = 0;
        // for (String direction : directions) {
            // Point buildPosition = next(nextPosition, direction);
            // if (!checkPosition(buildPosition)) {
                // continue;
            // }
            // int buildPositionLevel = state.map[buildPosition.y][buildPosition.x];
            // if (buildPositionLevel >= maxLevel && buildPositionLevel <= action.level) {
                // maxLevel = buildPositionLevel;
                // action.build = direction;
            // }
        // }
        // return action;
    // }

    public void mainLoop() {
        state.readState(input);
        state.printMap();
        while (true) {
            long time = System.currentTimeMillis();
            System.err.println(state.legalActions());
            System.err.println("passed time for legal actions :" + (System.currentTimeMillis() - time));
            Action a = new Action(
                input.next(), // atype
                input.nextInt(), // index
                input.next(), // move
                input.next()
            ); // build
            state.executeAction(a);
            state.printMap();
            System.err.println("SCORE: " + state.myScore);
        }
        // while (true) {
            // state.readState(input);
            // state.printMap();
            // Action action = findMove();
            // action = findBuild(action);
            // System.out.println(action);
        // }
    }

    public static void main(String args[]) {
        Game Game = new Game();
        Game.mainLoop();
    }
}
