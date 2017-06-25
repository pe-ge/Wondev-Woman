import java.util.*;
import java.io.*;
import java.math.*;

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
    char[][] map;
    int unitX, unitY;
    int otherX, otherY;
    ArrayList<Action> actions;
}

class Player {

    Scanner input;

    int size;
    int unitsPerPlayer;

    State state;

    // x * size + y -> action
    HashMap<Integer, String> posToAction;

    public Player() {
        input = new Scanner(System.in);

        size = input.nextInt();
        unitsPerPlayer = input.nextInt();

        state = new State();
        state.map = new char[size][size];
        state.actions = new ArrayList<Action>();

        posToAction = new HashMap<Integer, String>();
        posToAction.put(-size - 1,  "NW");
        posToAction.put(-size,      "W");
        posToAction.put(-size + 1,  "SW");
        posToAction.put(-1,          "N");
        posToAction.put(1,          "S");
        posToAction.put(size - 1,   "NE");
        posToAction.put(size,       "E");
        posToAction.put(size + 1,   "SE");
    }

    public void readInput() {
        for (int i = 0; i < size; i++) {
            String row = input.next();
            for (int j = 0; j < size; j++) {
                state.map[i][j] = row.charAt(j);
            }
        }
        for (int i = 0; i < unitsPerPlayer; i++) {
            state.unitX = input.nextInt();
            state.unitY = input.nextInt();
        }
        for (int i = 0; i < unitsPerPlayer; i++) {
            state.otherX = input.nextInt();
            state.otherY = input.nextInt();
        }
        int legalActions = input.nextInt();
        for (int i = 0; i < legalActions; i++) {
            Action action = new Action(
                input.next(),
                input.nextInt(),
                input.next(),
                input.next()
            );
            state.actions.add(action);
        }
    }

    public String getMove() {
        char myLevel = state.map[state.unitY][state.unitX];
        String moveAction = "";
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int x = state.unitX + i;
                int y = state.unitY + j;
                if (x < 0 || y < 0 || x >= size || y >= size) {
                    continue;
                }
                char level = state.map[y][x];

                if (level != '4' && level != '.') {
                    if (level <= myLevel) {
                        moveAction = posToAction.get(i * size + j);
                    } else if (level == myLevel+1) {
                        moveAction = posToAction.get(i * size + j);
                        System.err.println(posToAction);
                        return moveAction;
                    }
                }
            }
        }
        return moveAction;
    }

    public void mainLoop() {
        while (true) {
            readInput();
            System.out.println(String.format("MOVE&BUILD 0 %s %s", getMove(), "W"));
        }
    }

    public static void main(String args[]) {
        Player player = new Player();
        player.mainLoop();
    }
}
