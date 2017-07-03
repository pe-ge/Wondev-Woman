import java.util.*;
import java.io.*;
import java.math.*;

class Point {

    int x, y;

    static HashMap<String, Point> actionToDirection; // action -> (x, y)
    static HashMap<String, String[]> pushToDirections;

    static {
        actionToDirection = new HashMap<String, Point>();
        actionToDirection.put("NW",   new Point(-1, -1));
        actionToDirection.put("N",    new Point(0, -1));
        actionToDirection.put("NE",   new Point(1, -1));
        actionToDirection.put("W",    new Point(-1, 0));
        actionToDirection.put("E",    new Point(1, 0));
        actionToDirection.put("SW",   new Point(-1, 1));
        actionToDirection.put("S",    new Point(0, 1));
        actionToDirection.put("SE",   new Point(1, 1));

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

    public Point(int x, int y) {
        set(x, y);
    }

    public Point(Point other) {
        set(other);
    }

    public void set(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void set(Point other) {
        this.x = other.x;
        this.y = other.y;
    }

    public String toString() {
        return String.format("(x=%d, y=%d)", x, y);
    }

    public boolean equals(Point other) {
        return this.x == other.x && this.y == other.y;
    }

    public int distance(Point other) {
        return Math.max(Math.abs(this.x - other.x), Math.abs(this.y - other.y));
    }

    public static Point getDirection(String action) {
        return actionToDirection.get(action);
    }

    public void moveAndBuild(Action action, char[][] map) {
        move(action.move);
        build(action.build, map);
    }

    public void pushAndBuild(Action action, Point[] pushedUnits, char[][] map) {
        push(action.move, action.build, pushedUnits);
        build(action.move, map);
    }

    public void push(String push, String where, Point[] pushedUnits) {
        Point pushDirection = actionToDirection.get(push);
        int x = this.x + pushDirection.x;
        int y = this.y + pushDirection.y;

        for (Point unit : pushedUnits) {
            if (unit.x == x && unit.y == y) {
                // we found unit to be pushed
                Point whereDirection = actionToDirection.get(where);
                unit.x += whereDirection.x;
                unit.y += whereDirection.y;
                return;
            }
        }

        throw new RuntimeException("SHOULD NOT BE REACHED!!! DONT MAKE BUGS PLS");
    }

    public void move(String move) {
        Point direction = actionToDirection.get(move);
        x += direction.x;
        y += direction.y;
    }

    public void build(String build, char[][] map) {
        Point direction = actionToDirection.get(build);
        map[y + direction.y][x + direction.x]++;
    }
}

class Action {
    String atype;
    int index;
    String move;
    String build;

    Point moveFrom;
    Point moveTo;
    Point buildTo;
    int moveFromLevel;
    int moveToLevel;
    int buildLevelBefore;

    Point pushFrom;
    Point pushTo;
    int pushFromLevel;
    int pushToLevel;

    State before;
    State after;

    public Action(String atype, int index, String move, String build) {
        this.atype = atype;
        this.index = index;
        this.move = move;
        this.build = build;

        this.moveFrom = new Point(-1, -1);
        this.moveTo = new Point(-1, -1);
        this.buildTo = new Point(-1, -1);

        this.pushFrom = new Point(-1, -1);
        this.pushTo = new Point(-1, -1);
    }

    public Action(String atype, int index, String move, String build, State state) {
        this(atype, index, move, build);
        computeParams(state);

        before = state;
        after = before.executeAction(this, true);
    }

    public void computeParams(State state) {
        if (this.atype.equals("MOVE&BUILD")) {
            this.moveFrom = state.myUnits[index];

            Point moveDirection = Point.getDirection(move);
            this.moveTo.x = moveFrom.x + moveDirection.x;
            this.moveTo.y = moveFrom.y + moveDirection.y;

            Point buildDirection = Point.getDirection(build);
            this.buildTo.x = moveTo.x + buildDirection.x;
            this.buildTo.y = moveTo.y + buildDirection.y;

            this.moveFromLevel = state.getLevel(moveFrom);
            this.moveToLevel = state.getLevel(moveTo);
            this.buildLevelBefore = state.getLevel(buildTo);
        } else { // PUSH&BUILD
            this.moveFrom = state.myUnits[index];

            Point unitDirection = Point.getDirection(move);
            this.pushFrom.x = moveFrom.x + unitDirection.x;
            this.pushFrom.y = moveFrom.y + unitDirection.y;
            this.pushFromLevel = state.getLevel(pushFrom);

            Point pushDirection = Point.getDirection(build);
            this.pushTo.x = pushFrom.x + pushDirection.x;
            this.pushTo.y = pushFrom.y + pushDirection.y;
            this.pushToLevel = state.getLevel(pushTo);

            this.buildTo.x = this.pushFrom.x;
            this.buildTo.y = this.pushFrom.y;
            this.buildLevelBefore = state.getLevel(buildTo);
        }
    }

    public String toString() {
        return String.format("%s %d %s %s", atype, index, move, build);
    }
}

class State {
    static int size;
    static int unitsPerPlayer;

    char[][] map;

    Point[] myUnits;
    Point[] enemyUnits;
    Point[] prevPositions;

    ArrayList<Action> inputActions;

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

    public State() {
        map = new char[size][size];

        myUnits = new Point[unitsPerPlayer];
        enemyUnits = new Point[unitsPerPlayer];
        prevPositions = new Point[unitsPerPlayer];
        for (int i = 0; i < unitsPerPlayer; i++) {
            myUnits[i] = new Point(-1, -1);
            enemyUnits[i] = new Point(-1, -1);
            prevPositions[i] = new Point(-1, -1);
        }
    }

    public void readState(Scanner input) {
        for (int i = 0; i < size; i++) {
            String row = input.next();
            map[i] = row.toCharArray();
        }

        for (int i = 0; i < unitsPerPlayer; i++) {
            myUnits[i].set(input.nextInt(), input.nextInt());
        }
        for (int i = 0; i < unitsPerPlayer; i++) {
            prevPositions[i].set(enemyUnits[i]);
            enemyUnits[i].set(input.nextInt(), input.nextInt());
        }
        int legalActions = input.nextInt();
        inputActions = new ArrayList<Action>(legalActions);
        for (int i = 0; i < legalActions; i++) {
            Action a = new Action(
                input.next(), // atype
                input.nextInt(), // index
                input.next(), // move
                input.next(),
                this
            );
            inputActions.add(a);
            // System.err.println(a);
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
        for (int i = 0; i < unitsPerPlayer; i++) {
            System.err.println("My unit: " + myUnits[i]);
            System.err.println("Enemy unit: " + enemyUnits[i]);
        }
    }

    public State clone() {
        State copied = new State();
        copied.map = new char[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                copied.map[i][j] = map[i][j];
            }
        }

        copied.myUnits = new Point[unitsPerPlayer];
        copied.enemyUnits = new Point[unitsPerPlayer];
        for (int i = 0; i < unitsPerPlayer; i++) {
            copied.myUnits[i] = new Point(myUnits[i]);
            copied.enemyUnits[i] = new Point(enemyUnits[i]);
        }

        copied.myScore = myScore;
        copied.enemyScore = enemyScore;

        return copied;
    }

    public State executeAction(Action action, boolean myUnitTurn) {
        State nextState = this.clone();
        Point executingUnit = myUnitTurn ? nextState.myUnits[action.index] : nextState.enemyUnits[action.index];
        switch (action.atype) {
            case "MOVE&BUILD":
                executingUnit.moveAndBuild(action, nextState.map);
                if (nextState.map[executingUnit.y][executingUnit.x] == '3') {
                    if (myUnitTurn) {
                        nextState.myScore++;
                    } else {
                        nextState.enemyScore++;
                    }
                }
                break;
            case "PUSH&BUILD":
                Point[] otherUnits = myUnitTurn ? nextState.enemyUnits : nextState.myUnits;
                executingUnit.pushAndBuild(action, otherUnits, nextState.map);
                break;
        }
        return nextState;
    }

    public Point canMove(String move, Point position, Point[] units, Point[] otherUnits, char[][] map) {
        Point moveTo = canBuild(move, position, units, otherUnits, map);
        if (moveTo == null) {
            return null;
        }

        int currentLevel = getLevel(position);
        int moveToLevel = getLevel(moveTo);
        if (currentLevel + 1 < moveToLevel) {
            return null;
        }

        return moveTo;
    }

    public Point canBuild(String move, Point position, Point[] units, Point[] otherUnits, char[][] map) {
        Point direction = Point.actionToDirection.get(move);
        int x = position.x + direction.x;
        int y = position.y + direction.y;

        if (!legalCell(x, y, map)) {
            return null;
        }

        for (Point unit : units) {
            if (unit.x == x && unit.y == y) {
                return null;
            }
        }
        for (Point unit : otherUnits) {
            if (unit.x == x && unit.y == y) {
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

    public Point getNeighbourUnit(Point myUnit, String direction, Point[] units, char[][] map) {
        Point directionPoint = Point.actionToDirection.get(direction);
        int x = myUnit.x + directionPoint.x;
        int y = myUnit.y + directionPoint.y;

        if (!legalCell(x, y, map)) {
            return null;
        }

        for (Point unit : units) {
            if (unit.x == x && unit.y == y) {
                return unit;
            }
        }

        return null;
    }

    public ArrayList<Action> legalActions(boolean myUnitTurn) {
        ArrayList<Action> actions = new ArrayList<Action>();
        for (int i = 0; i < unitsPerPlayer; i++) {
            actions.addAll(legalActions(i, myUnitTurn));
        }
        return actions;
    }

    public ArrayList<Action> legalActions(int unitIdx, boolean myUnitTurn) {
        ArrayList<Action> legalActions = new ArrayList<Action>();
        Point[] units = myUnitTurn ? myUnits : enemyUnits;
        Point[] otherUnits = myUnitTurn ? enemyUnits : myUnits;

        Point unit = units[unitIdx];
        // MOVE&BUILD
        // first move
        for (String moveDirection : directions) {
            //System.err.println(unit + " " + moveDirection);
            Point movePoint = canMove(moveDirection, unit, units, otherUnits, map);
            if (movePoint != null) {
                // then build
                int tmpX = unit.x;
                int tmpY = unit.y;
                unit.x = -1;
                unit.y = -1;
                for (String buildDirection : directions) {
                    Point buildPoint = canBuild(buildDirection, movePoint, units, otherUnits, map);
                    if (buildPoint != null) {
                        Action action = new Action("MOVE&BUILD", unitIdx, moveDirection, buildDirection);
                        legalActions.add(action);
                    }
                }
                unit.x = tmpX;
                unit.y = tmpY;
            }
        }

        // PUSH&BUILD
        // push
        for (String pushDirection : directions) {
            Point pushedUnit = getNeighbourUnit(unit, pushDirection, otherUnits, map);
            if (pushedUnit != null) {
                // System.err.println("mozem tiskat " + pushDirection);
                for (String moveDirection : Point.pushToDirections.get(pushDirection)) {
                    // check whether pushedTo position is free
                    Point pushedTo = canMove(moveDirection, pushedUnit, units, otherUnits, map);
                    if (pushedTo != null) {
                        Action action = new Action("PUSH&BUILD", unitIdx, pushDirection, moveDirection);
                        legalActions.add(action);
                    }
                }
            }
        }
        return legalActions;
    }
    public int getLevel(Point point) {
        return map[point.y][point.x] - '0';
    }
}

class Player {

    Scanner input;

    State state;

    Random random;

    public int evaluate(Action action) {
        if (action.atype.equals("PUSH&BUILD")) {
            return evaluatePushAndBuild(action);
        } else {
            return evaluateMoveAndBuild(action);
        }
    }

    public int evaluatePushAndBuild(Action action) {
        if (action.pushFromLevel >= action.pushToLevel + 1) {
            return 100000 * (action.pushFromLevel - action.pushToLevel + 1);
        }
        int actionsBefore = action.before.legalActions(action.index, true).size();
        int actionsAfter = action.after.legalActions(action.index, true).size();
        if (actionsAfter == 0) {
            return -100000 * (actionsBefore - actionsAfter);
        }
        return -1000 * (actionsBefore - actionsAfter);
    }

    public int evaluateMoveAndBuild(Action action) {
        int helpsEnemy = 0;
        int blocksEnemy = 0;
        int buildOnThree = 0;
        int buildHeight = 0;

        for (Point enemy : state.enemyUnits) {
            if (enemy.x == -1) continue;
            int enemyHeight = state.getLevel(enemy);
            int dist = action.buildTo.distance(enemy);
            if (dist == 1 && action.buildLevelBefore == 2 && enemyHeight >= 2) {
                helpsEnemy = 1;
            }
            if (dist == 1 && action.buildLevelBefore == 3 && enemyHeight >= 2) {
                blocksEnemy = 1;
            }
        }
        if (action.buildLevelBefore == 3) {
            buildOnThree = 1;
        }
        // System.err.println(action.buildLevelBefore);

        int build = 0;
        if (action.buildLevelBefore == action.moveToLevel) {
            build = 2;
        } else if (action.buildLevelBefore > action.moveToLevel) {
            build = -1;
        } else if (action.buildLevelBefore < action.moveToLevel) {
            build = 1;
        }

        int actionsBefore = action.before.legalActions(action.index, true).size();
        int actionsAfter = action.after.legalActions(action.index, true).size();
        if (actionsAfter == 0) {
            return -100000 * (actionsBefore - actionsAfter);
        }

        int actionsLost = actionsBefore - actionsAfter;

        return -10000 * helpsEnemy + 1000 * blocksEnemy -10 * actionsLost + 100 * action.moveToLevel - 5 * buildOnThree + build;
    }

    public Action findBestAction() {
        Action best = null;
        int bestValue = Integer.MIN_VALUE;
        for (Action action : state.inputActions) {
            int actionValue = evaluate(action);
            //System.err.println(action + " " + actionValue);
            if (actionValue > bestValue) {
                bestValue = actionValue;
                best = action;
            }
        }
        return best;
    }

    public Player() {
        input = new Scanner(System.in);

        State.size = input.nextInt();
        State.unitsPerPlayer = input.nextInt();

        state = new State();

        random = new Random();
    }

    public void mainLoop() {
        // DEBUG
        // state.readState(input);
        // state.printMap();
        // while (true) {
            // long time = System.currentTimeMillis();
            // System.err.println(state.legalActions());
            // System.err.println("passed time for legal actions :" + (System.currentTimeMillis() - time));
            // Action a = new Action(
                // input.next(), // atype
                // input.nextInt(), // index
                // input.next(), // move
                // input.next()
            // ); // build
            // state.executeAction(a);
            // state.printMap();
            // System.err.println("SCORE: " + state.myScore);
        // }
        while (true) {
            state.readState(input);
            // state.printMap();
            // Action action = findMove();
            // action = findBuild(action);
            System.out.println(findBestAction());
        }
    }

    public static void main(String args[]) {
        Player player = new Player();
        player.mainLoop();
    }
}
