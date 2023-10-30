package jp.ac.titech.c.se.diff;

import es.usc.citius.hipster.algorithm.Hipster;
import es.usc.citius.hipster.model.Transition;
import es.usc.citius.hipster.model.function.ActionFunction;
import es.usc.citius.hipster.model.function.ActionStateTransitionFunction;
import es.usc.citius.hipster.model.function.CostFunction;
import es.usc.citius.hipster.model.function.HeuristicFunction;
import es.usc.citius.hipster.model.function.impl.BinaryOperation;
import es.usc.citius.hipster.model.impl.WeightedNode;
import es.usc.citius.hipster.model.problem.ProblemBuilder;
import es.usc.citius.hipster.model.problem.SearchProblem;

import java.util.*;
import java.util.function.BiPredicate;

public final class AStarDifferencer<T> implements Differencer<T> {

    public static final int LINE_WEIGHT = 10000;

    private final BiPredicate<T, T> equality;

    public AStarDifferencer() {
        this(Object::equals);
    }

    public AStarDifferencer(final BiPredicate<T, T> equality) {
        this.equality = equality;
    }

    public List<Chunk> computeDiff(final List<T> source, final List<T> target) {
        final State goal = new State(source.size(), target.size());
        final List<Chunk> result = new ArrayList<>();
        for (final WeightedNode<Chunk.Type, State, Integer> node : Hipster.createAStar(new Search(source, target).createProblem()).search(goal).getGoalNode().path()) {
            if (node.action() != null) {
                result.add(new Chunk(node.action(),
                        node.action() == Chunk.Type.INS ? node.state().s : node.state().s - 1,
                        node.state().s,
                        node.action() == Chunk.Type.DEL ? node.state().t : node.state().t - 1,
                        node.state().t));
            }
        }
        return result;
    }

    record State(int s, int t) {}

    class Search implements
            ActionFunction<Chunk.Type, State>,
            ActionStateTransitionFunction<Chunk.Type, State>,
            CostFunction<Chunk.Type, State, Integer>,
            HeuristicFunction<State, Integer> {
        final List<T> source, target;

        public Search(final List<T> source, final List<T> target) {
            this.source = source;
            this.target = target;
        }

        public SearchProblem<Chunk.Type, State, WeightedNode<Chunk.Type, State, Integer>> createProblem() {
            return ProblemBuilder.create()
                    .initialState(new State(0, 0))
                    .defineProblemWithExplicitActions()
                    .useActionFunction(this)
                    .useTransitionFunction(this)
                    .useGenericCostFunction(this, new BinaryOperation<>(Integer::sum, 0, Integer.MAX_VALUE))
                    .useHeuristicFunction(this)
                    .build();
        }

        static final List<Chunk.Type> OP_ALL = List.of(Chunk.Type.EQL, Chunk.Type.DEL, Chunk.Type.INS);
        static final List<Chunk.Type> OP_NEQL = List.of(Chunk.Type.DEL, Chunk.Type.INS);
        static final List<Chunk.Type> OP_INS = List.of(Chunk.Type.INS);
        static final List<Chunk.Type> OP_DEL = List.of(Chunk.Type.DEL);

        @Override
        public List<Chunk.Type> actionsFor(final State state) {
            if (state.s < source.size() && state.t < target.size()) {
                if (equality.test(source.get(state.s), target.get(state.t))) {
                    return OP_ALL;
                } else {
                    return OP_NEQL;
                }
            } else if (state.s < source.size()) {
                return OP_DEL;
            } else if (state.t < target.size()) {
                return OP_INS;
            } else {
                return Collections.emptyList();
            }
        }

        //static int n=0;

        @Override
        public State apply(final Chunk.Type op, final State state) {
            //System.out.printf("node%d ", n++);
            switch (op) {
                case INS:
                    return new State(state.s, state.t + 1);
                case DEL:
                    return new State(state.s + 1, state.t);
                case EQL:
                    return new State(state.s + 1, state.t + 1);
                default:
                    assert false;
                    return null;
            }
        }

        @Override
        public Integer evaluate(final Transition<Chunk.Type, State> transition) {
            // control the order of operations that prefers DEL-INS rather than INS-DEL
            return LINE_WEIGHT + (transition.getAction() == Chunk.Type.DEL ? transition.getState().t : 0);
        }

        @Override
        public Integer estimate(final State state) {
            return Math.max(source.size() - state.s, target.size() - state.t) * LINE_WEIGHT;
        }
    }
}
