import random

from game import Action, State, execute, legal_actions

from deap import base
from deap import creator
from deap import tools

from time import time, sleep


# OPPONENT = [-10000, 1000, -100, 100, -10, 1, 100000, -100000, -1000]
OPPONENT = [-8933, -4201, 7209, -2430, -3781, 2546, -4545, -7932, -195]
NUM_WEIGHTS = len(OPPONENT)
MUTATIONS = [-10000, -1000, -100, -10, -1, 1, 10, 100, 1000, 10000]


def mutate(individual, indpb):
    for idx, attr in enumerate(individual):
        individual[idx] += MUTATIONS[random.randint(0, len(MUTATIONS) - 1)]
    return individual,


def distance(unit1, unit2):
    return max(abs(unit1[0] - unit2[0]), abs(unit1[1] - unit2[1]))


def evaluate(action, state, weights):
    if action.type == 'MOVE&BUILD':
        helps_enemy = 0
        blocks_enemy = 0
        build_on_three = 0

        enemy_units = state.p2_units if state.my_turn else state.p1_units
        for enemy in enemy_units:
            enemy_height = state.world[enemy[1]][enemy[0]]
            dist = (action.build_to, enemy)
            if dist == 1 and action.build_level_before == 2 and enemy_height >= 2:
                helps_enemy = 1
            if dist == 1 and action.build_level_before == 3 and enemy_height >= 2:
                blocks_enemy = 1

        if action.build_level_before == 3:
            build_on_three = 1

        build = 0
        if action.build_level_before == action.move_to_level:
            build = 2
        elif action.build_level_before > action.move_to_level:
            build = -1
        elif action.build_level_before < action.move_to_level:
            build = 1

        # t1 = time()
        # actions_before = len(legal_actions(action.state_before))
        # actions_after = len(legal_actions(action.state_after))
        # print(time() - t1)
        # if actions_after == 0:
            # return -weights[7]

        # actions_lost = actions_before - actions_after
        actions_lost = 0

        return weights[0] * helps_enemy + weights[1] * blocks_enemy -weights[2] * actions_lost + weights[3] * action.move_to_level - weights[4] * build_on_three + weights[5] * build
    else:
        if action.push_from_level >= action.push_to_level + 1:
            return weights[6]
        # actions_before = len(legal_actions(action.state_before))
        # actions_after = len(legal_actions(action.state_after))
        # if actions_after == 0:
            # return -weights[7]

        return -weights[8]

def choose_best(state, weights):
    actions = legal_actions(state)
    # t1 = time()
    if (len(actions) == 0):
        return None
    best = max(actions, key=lambda a: evaluate(a, state, weights))
    # print(time() - t1)
    return best


def fitness(individual):
    state = State()
    first = True
    while True:
        # print(state)
        # sleep(0.5)
        action = choose_best(state, individual) if first else choose_best(state, OPPONENT)
        if action is None:
            return state.p1_score - state.p2_score,

        # print(individual)
        first = not first
        state = execute(state, action)
    pass


creator.create("FitnessMax", base.Fitness, weights=(1.0,))
creator.create("Individual", list, fitness=creator.FitnessMax)

toolbox = base.Toolbox()

toolbox.register("attr", random.randint, -10000, 10000)

toolbox.register("individual", tools.initRepeat, creator.Individual, toolbox.attr, NUM_WEIGHTS)

toolbox.register("population", tools.initRepeat, list, toolbox.individual)

toolbox.register("evaluate", fitness)

toolbox.register("mate", tools.cxUniform, indpb=0.5)

toolbox.register("mutate", mutate)

toolbox.register("select", tools.selTournament, tournsize=3)


def main():
    # random.seed(64)

    pop = toolbox.population(n=100)

    CXPB, MUTPB = 0.5, 0.2

    fitnesses = list(map(toolbox.evaluate, pop))
    for ind, fit in zip(pop, fitnesses):
        ind.fitness.values = fit

    fits = [ind.fitness.values[0] for ind in pop]

    g = 0

    # Begin the evolution
    while max(fits) < 100 and g < 1000:
        # A new generation
        g = g + 1
        print("-- Generation %i --" % g)

        # Select the next generation individuals
        offspring = toolbox.select(pop, len(pop))
        # Clone the selected individuals
        offspring = list(map(toolbox.clone, offspring))

        # Apply crossover and mutation on the offspring
        for child1, child2 in zip(offspring[::2], offspring[1::2]):

            # cross two individuals with probability CXPB
            if random.random() < CXPB:
                toolbox.mate(child1, child2)

                # fitness values of the children
                # must be recalculated later
                del child1.fitness.values
                del child2.fitness.values

        for mutant in offspring:

            # mutate an individual with probability MUTPB
            if random.random() < MUTPB:
                toolbox.mutate(mutant, indpb=0.3)
                del mutant.fitness.values

        # Evaluate the individuals with an invalid fitness
        invalid_ind = [ind for ind in offspring if not ind.fitness.valid]
        fitnesses = map(toolbox.evaluate, invalid_ind)
        for ind, fit in zip(invalid_ind, fitnesses):
            ind.fitness.values = fit
        # The population is entirely replaced by the offspring
        pop[:] = offspring

        # Gather all the fitnesses in one list and print the stats
        fits = [ind.fitness.values[0] for ind in pop]
        best_ind = tools.selBest(pop, 1)[0]
        print("Best individual is %s, %s" % (best_ind, best_ind.fitness.values))



if __name__ == "__main__":
    main()
