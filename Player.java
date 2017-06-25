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
}

class Action {
    String atype;
    int index;
    String dir1;
    String dir2;

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

    char[][] map;
    int unitX, unitY, unitLevel;
    int otherX, otherY, otherLevel;
    ArrayList<Action> actions;

    public State(int size, int unitsPerPlayer) {
        this.size = size;
        this.unitsPerPlayer = unitsPerPlayer;

        map = new char[size][size];
        actions = new ArrayList<Action>();
    }

    public void readState(Scanner input) {
        for (int i = 0; i < size; i++) {
            String row = input.next();
            for (int j = 0; j < size; j++) {
                map[i][j] = row.charAt(j);
            }
        }
        for (int i = 0; i < unitsPerPlayer; i++) {
            unitX = input.nextInt();
            unitY = input.nextInt();
        }
        for (int i = 0; i < unitsPerPlayer; i++) {
            otherX = input.nextInt();
            otherY = input.nextInt();
        }
        int legalActions = input.nextInt();
        for (int i = 0; i < legalActions; i++) {
            Action action = new Action(
                input.next(),
                input.nextInt(),
                input.next(),
                input.next()
            );
            actions.add(action);
        }

        unitLevel = map[unitY][unitX];
        otherLevel = map[otherY][otherY];
    }
}

class Player {

    Scanner input;

    int size;
    int unitsPerPlayer;

    State state;

    HashMap<String, Point> actionToPos; // action -> (x, y)

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

        random = new Random();
    }

    public Point move(int x, int y, String moveAction) {
        Point direction = actionToPos.get(moveAction);
        return new Point(x + direction.x, y + direction.y);
    }

    public boolean checkPosition(Point position) {
        int x = position.x;
        int y = position.y;
        if (state.otherX == x && state.otherY == y) {
            return false;
        }
        if (x > 0 && y > 0 && x < size && y < size) {
            return true;
        }
        return false;
    }

    public Action getAction() {
        Action bestAction = null;
        for (Action action : state.actions) {
            // System.err.println(action);
            Point nextPosition = move(state.unitX, state.unitY, action.dir1);
            if (!checkPosition(nextPosition)) {
                continue;
            }
            int nextLevel = state.map[nextPosition.y][nextPosition.x];
            if (nextLevel == '3') {
                System.err.println(nextPosition);
                return action;
            }
            if (nextLevel == state.unitLevel + 1) {
                bestAction = action;
            }

        }

        if (bestAction == null) {
            int randomIdx = random.nextInt(state.actions.size());
            bestAction = state.actions.get(randomIdx);
        }

        return bestAction;
    }

    public void mainLoop() {
        while (true) {
            state.readState(input);
            System.out.println(getAction());
        }
    }

    public static void main(String args[]) {
        Player player = new Player();
        player.mainLoop();
    }
}
