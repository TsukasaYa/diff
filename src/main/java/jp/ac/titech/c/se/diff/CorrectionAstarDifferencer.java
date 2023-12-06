package jp.ac.titech.c.se.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;

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

public class CorrectionAstarDifferencer<T> implements CorrectionDifferencer<T>{
    
    private final BiPredicate<T, T> equality;
    public static final int LINE_WEIGHT = 10000;

    final List<T> source, target;
    final int CORRECTION_WEIGHT;
    final int[][] editGraphDiag;
    final int[][] verticalCorrection;
    final int[][] horizontalCorrection;
    final int[][] diagonalCorrection;

    public CorrectionAstarDifferencer(final List<T> source, final List<T> target) {
        this(Object::equals, source, target);
    }

    public CorrectionAstarDifferencer(BiPredicate<T, T> equality, final List<T> source, final List<T> target) {
        this.equality = equality;
        this.source = source;
        this.target = target;
        CORRECTION_WEIGHT = source.size() + target.size() + 1;

        editGraphDiag = new int[source.size()][target.size()];
        verticalCorrection = new int[source.size()+1][target.size()+1];
        horizontalCorrection = new int[source.size()+1][target.size()+1];
        diagonalCorrection = new int[source.size()][target.size()];
        initGraph();
    }

    private void initGraph(){
        for(int i = 0; i< source.size(); i++){
            for(int j=0; j< target.size(); j++){
                diagonalCorrection[i][j] = 0;
                if(equality.test(source.get(i), target.get(j))){
                    editGraphDiag[i][j] = 1;
                }else{
                    editGraphDiag[i][j] = CORRECTION_WEIGHT;
                }
            }
        }
        for(int i = 0; i<= source.size(); i++){
            for(int j=0; j<= target.size(); j++){
                verticalCorrection[i][j] = 0;
                horizontalCorrection[i][j] = 0;
            }
        }
    }

    @Override
    public List<Chunk> computeDiff(Collection<Chunk> correction){
        for(Chunk c: correction){
            setWeight(c);
        }
        //showEditGraph();
        final State goal = new State(source.size(), target.size());
        final List<Chunk> result = new ArrayList<>();
        for (final WeightedNode<Chunk.Type, State, Integer> node : Hipster.createAStar(new Search().createProblem()).search(goal).getGoalNode().path()) {
            if (node.action() != null) {
                result.add(new Chunk(node.action(),
                        node.action() == Chunk.Type.INS ? node.state().s : node.state().s - 1,
                        node.state().s,
                        node.action() == Chunk.Type.DEL ? node.state().t : node.state().t - 1,
                        node.state().t));
            }
        }

        for(Chunk c: correction){
            resetWeight(c);
        }
        return result;
    }

    private void setWeight(Chunk c){
        switch (c.type) {
            case EQL:
                diagonalCorrection[c.sourceStart][c.targetStart] = CORRECTION_WEIGHT;
                break;
            case DEL:
                verticalCorrection[c.sourceStart][c.targetStart] = CORRECTION_WEIGHT;
                break;
            case INS:
                horizontalCorrection[c.sourceStart][c.targetStart] = CORRECTION_WEIGHT;
                break;
            default:
                throw new IllegalStateException();
        }
    }

    private void resetWeight(Chunk c){
        switch (c.type) {
            case EQL:
                diagonalCorrection[c.sourceStart][c.targetStart] = 0;
                break;
            case DEL:
                verticalCorrection[c.sourceStart][c.targetStart] = 0;
                break;
            case INS:
                horizontalCorrection[c.sourceStart][c.targetStart] = 0;
                break;
            default:
                throw new IllegalStateException();
        }
    }

    record State(int s, int t) {}

    class Search implements
            ActionFunction<Chunk.Type, State>,
            ActionStateTransitionFunction<Chunk.Type, State>,
            CostFunction<Chunk.Type, State, Integer>,
            HeuristicFunction<State, Integer> {

        public Search() {
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
                return OP_ALL;
            } else if (state.s < source.size()) {
                return OP_DEL;
            } else if (state.t < target.size()) {
                return OP_INS;
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public State apply(final Chunk.Type op, final State state) {
            //System.err.println(op+ " "+ state);
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
            int cost = LINE_WEIGHT;
            if(transition.getAction() == Chunk.Type.EQL){
                cost = LINE_WEIGHT*editGraphDiag[transition.getState().s-1][transition.getState().t-1];
                cost += LINE_WEIGHT*diagonalCorrection[transition.getState().s-1][transition.getState().t-1];
            }else if(transition.getAction() == Chunk.Type.INS){
                cost += LINE_WEIGHT*horizontalCorrection[transition.getState().s][transition.getState().t-1];
            }else{
                cost += LINE_WEIGHT*verticalCorrection[transition.getState().s-1][transition.getState().t];
                // control the order of operations that prefers DEL-INS rather than INS-DEL
                cost += transition.getState().t;
            }
            return cost;
        }

        @Override
        public Integer estimate(final State state) {
            return Math.max(source.size() - state.s, target.size() - state.t) * LINE_WEIGHT;
        }
    }
}
