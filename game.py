from random import randint, shuffle
from collections import namedtuple
from copy import deepcopy

DIRECTIONS = [(-1, -1),
              (-1, 0),
              (-1, 1),
              (0, -1),
              (0, 1),
              (1, -1),
              (1, 0),
              (1, 1)]

PUSH_DIRECTIONS = {(-1, -1): [(-1, -1),
                              (-1, 0),
                              (0, -1)],

                   (-1, 0): [(-1, -1),
                             (-1, 0),
                             (-1, 1)],

                   (-1, 1): [(-1, 0),
                             (-1, 1),
                             (0, 1)],

                   (0, -1): [(-1, -1),
                             (0, -1),
                             (1, -1)],

                   (0, 1): [(-1, 1),
                            (0, 1),
                            (1, 1)],

                   (1, -1): [(0, -1),
                             (1, -1),
                             (1, 0)],

                   (1, 0): [(1, -1),
                            (1, 0),
                            (1, 1)],

                   (1, 1): [(1, 0),
                            (1, 1),
                            (0, 1)]}

WORLD_SIZE = 5


class State():

    def __init__(self):
        self.world = [[0 for i in range(WORLD_SIZE)] for j in range(WORLD_SIZE)]

        positions = list([i, j] for i in range(WORLD_SIZE) for j in range(WORLD_SIZE))
        shuffle(positions)
        self.p1_units = positions[0:2]
        self.p2_units = positions[2:4]

        self.p1_score = 0
        self.p2_score = 0
        # self.my_turn = True if randint(0, 1) else False
        self.my_turn = True

    def __str__(self):
        result = ''
        for row in self.world:
            for char in row:
                result += str(char)
            result += '\n'
        result += '\n'

        with_positions = [[0 for i in range(WORLD_SIZE)] for j in range(WORLD_SIZE)]
        for unit in self.p1_units:
            with_positions[unit[1]][unit[0]] = 1
        for unit in self.p2_units:
            with_positions[unit[1]][unit[0]] = 2

        for row in with_positions:
            for char in row:
                result += str(char)
            result += '\n'
        result += '\n'
        result += str(self.p1_units) + '\n'
        result += str(self.p2_units) + '\n'
        result += str(self.p1_score) + '\n'
        result += str(self.p2_score) + '\n'
        return result


class Action():

    def __init__(self, atype, index, dir1, dir2, state):
        def get_level(pos, state):
            return state.world[pos[1]][pos[0]]

        self.type = atype
        self.index = index
        self.dir1 = dir1
        self.dir2 = dir2

        my_units = state.p1_units if state.my_turn else state.p2_units
        enemy_units = state.p2_units if state.my_turn else state.p1_units

        self.move_from = my_units[index]

        if atype == 'MOVE&BUILD':
            self.move_to = [self.move_from[0] + dir1[0], self.move_from[1] + dir1[1]]
            self.build_to = [self.move_to[0] + dir2[0], self.move_to[1] + dir2[1]]
            self.move_to_level = get_level(self.move_to, state)
        else:

            self.push_from = [self.move_from[0] + dir1[0], self.move_from[1] + dir1[1]]
            self.push_to = [self.push_from[0] + dir2[0], self.push_from[1] + dir2[1]]
            self.build_to = self.push_from
            self.push_from_level = get_level(self.push_from, state)
            self.push_to_level = get_level(self.push_to, state)

        self.move_from_level = get_level(self.move_from, state)
        self.build_level_before = get_level(self.build_to, state)

        self.state_before = state
        self.state_after = deepcopy(state)
        self.state_after = execute(self.state_after, self)



    def __repr__(self):
        return '{} {} {} {} '.format(self.type,
                                     self.index,
                                     self.dir1,
                                     self.dir2)


def can_move(unit, direction, units1, units2, world):
    moved_pos = can_build(unit, direction, units1, units2, world)
    if moved_pos is None:
        return None
    [old_x, old_y] = unit
    [x, y] = moved_pos
    current_height = world[old_y][old_x]
    new_height = world[y][x]
    if current_height + 1 < new_height:
        return None

    return moved_pos


def can_build(unit, direction, units1, units2, world):
    x = unit[0] + direction[0]
    y = unit[1] + direction[1]

    if not legal_cell(x, y, world):
        return None

    for u in units1:
        if [x, y] == u:
            return None

    for u in units2:
        if [x, y] == u:
            return None

    return [x, y]

def legal_cell(x, y, world):
    if (x < 0 or y < 0 or x >= WORLD_SIZE or y >= WORLD_SIZE):
        return False

    if (world[y][x] == -1 or world[y][x] == 4):
        return False

    return True

def legal_actions(state):
    actions = []
    my_units = state.p1_units if state.my_turn else state.p2_units
    enemy_units = state.p2_units if state.my_turn else state.p1_units

    for i in range(2):
        my_unit = my_units[i]
        for direction in DIRECTIONS:
            # MOVE&BUILD
            moved_pos = can_move(my_unit, direction, my_units, enemy_units, state.world)
            # print(my_unit, direction, moved_pos)
            if moved_pos is not None:
                tmp = my_unit[:]
                my_unit[0], my_unit[1] = -1, -1
                for build_dir in DIRECTIONS:
                    build_pos = can_build(moved_pos, build_dir, my_units, enemy_units, state.world)
                    if build_pos is not None:
                        action = Action('MOVE&BUILD', i, direction, build_dir, state)
                        actions.append(action)

                my_unit[0], my_unit[1] = tmp

            # PUSH&BUILD
            pushed_unit = get_neighbour_unit(my_unit, direction, enemy_units, state.world)
            if pushed_unit is not None:
                for push_dir in PUSH_DIRECTIONS[direction]:
                    pushed_to = can_move(pushed_unit, push_dir, my_units, enemy_units, state.world)
                    if pushed_to is not None:
                        action = Action('PUSH&BUILD', i, direction, push_dir, state)
                        actions.append(action)

    return actions


def get_neighbour_unit(my_unit, direction, enemy_units, world):
    x = my_unit[0] + direction[0]
    y = my_unit[1] + direction[1]

    if not legal_cell(x, y, world):
        return None

    for unit in enemy_units:
        if unit == [x, y]:
            return unit

    return None


def execute(state, action):
    unit = state.p1_units[action.index] if state.my_turn else state.p2_units[action.index]
    if action.type == 'MOVE&BUILD':
        unit[0] += action.dir1[0]
        unit[1] += action.dir1[1]
        x, y = (unit[0] + action.dir2[0], unit[1] + action.dir2[1])
        state.world[y][x] += 1
        if state.world[y][x] == 3:
            if state.my_turn:
                state.p1_score += 1
            else:
                state.p2_score += 1
    else:
        other_units = state.p2_units if state.my_turn else state.p1_units
        pushed_unit = [unit[0] + action.dir1[0], unit[1] + action.dir1[1]]

        for other in other_units:
            if other == pushed_unit:
                state.world[other[1]][other[0]] += 1
                other[0] += action.dir2[0]
                other[1] += action.dir2[1]
                break

    state.my_turn = not state.my_turn
    return state


if __name__ == "__main__":
    state = State()
    while True:
        actions = legal_actions(state)
        for idx, action in enumerate(actions):
            print(idx, action)
        print(state)
        user = int(input())
        state = execute(state, actions[user])
