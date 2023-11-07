package jp.ac.titech.c.se.diff;

import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashSet;

import es.usc.citius.hipster.util.Predicate;
import es.usc.citius.hipster.model.Node;

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

import org.apache.commons.collections4.CollectionUtils;

public final class DiffSearch implements
    ActionFunction<Chunk, ModificationState>,
    ActionStateTransitionFunction<Chunk, ModificationState>,
    CostFunction<Chunk, ModificationState, Integer>,
    HeuristicFunction<ModificationState, Integer>{

    final List<String> source;
    final List<String> target;
    final List<Chunk> targetDiff;

    public DiffSearch(List<String> source, List<String> target){
        this.source = source;
        this.target = target;
        targetDiff = computeTargetDiff(source, target);
        List<Chunk> diff = getCorrectDiff(source, target);
        GoalPredicate gp = new GoalPredicate(diff);
        
        show(diff, true);
    }

    private List<Chunk> computeTargetDiff(List<String> source, List<String> target){
        List<Chunk> diff = new JGitDifferencer.Histogram<String>().computeDiff(source, target);
        diff = Chunkase.degrade(diff, source.size(), target.size());
        return diff;
    }

    //多分テスト用
    private List<Chunk> getCorrectDiff(final List<String> source, final List<String> target) {
        CorrectionDifferencer<String> corrctionDifferencer = new CorrectionDynamicProgrammingDifferencer<>(source, target);
        List<Chunk> correction = new ArrayList<>();

        //この辺に指摘入れたいエッジを追加する
        correction.add(new Chunk(Chunk.Type.EQL, 3, 4, 11, 12));
        correction.add(new Chunk(Chunk.Type.EQL, 4, 5, 12, 13));

        return corrctionDifferencer.computeDiff(correction);
    }

    public void show(List<Chunk> diff, boolean showLocation) {
        for (Chunk c : diff) {
            switch (c.type) {
                case DEL:
                    if (showLocation) {
                        System.out.printf("@@ -%d,%d +%d @@\n", c.sourceStart, c.sourceEnd - 1, c.targetStart);
                    }
                    for (int i = c.sourceStart; i < c.sourceEnd; i++) {
                        System.out.println("- " + source.get(i));
                    }
                    break;
                case INS:
                    if (showLocation) {
                        System.out.printf("@@ -%d +%d,%d @@\n", c.sourceStart, c.targetStart, c.targetEnd - 1);
                    }
                    for (int i = c.targetStart; i < c.targetEnd; i++) {
                        System.out.println("+ " + target.get(i));
                    }
                    break;
                case MOD:
                    if (showLocation) {
                        System.out.printf("@@ -%d,%d +%d,%d @@\n", c.sourceStart, c.sourceEnd - 1, c.targetStart, c.targetEnd - 1);
                    }
                    for (int i = c.sourceStart; i < c.sourceEnd; i++) {
                        System.out.println("- " + source.get(i));
                    }
                    for (int i = c.targetStart; i < c.targetEnd; i++) {
                        System.out.println("+ " + target.get(i));
                    }
                    break;
                case EQL:
                    if (showLocation) {
                        System.out.printf("@@ -%d,%d +%d,%d @@\n", c.sourceStart, c.sourceEnd - 1, c.targetStart, c.targetEnd - 1);
                    }
                    for (int i = c.sourceStart; i < c.sourceEnd; i++) {
                        System.out.println("  " + source.get(i));
                    }
                    break;
                default:
                    assert false;
            }
        }
    }

    public CorrectionDifferencer<String> getCorrectionDifferencer(App.DifferencerType differencerType, List<String> source, List<String> target) {
        return switch (differencerType) {
            case dp -> new CorrectionDynamicProgrammingDifferencer<>(source, target);
            case astar -> throw new IllegalArgumentException("yet implementation");
            case myers -> throw new IllegalArgumentException("no implementation");
            case histogram -> throw new IllegalArgumentException("no implementation");
        };
    }

    @Override
    public Integer estimate(ModificationState state) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'estimate'");
    }

    @Override
    public Integer evaluate(Transition<Chunk, ModificationState> transition) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'evaluate'");
    }

    @Override
    public ModificationState apply(Chunk action, ModificationState state) {
        Collection<Chunk> correction = new HashSet<>(state.correction);
        List<Chunk> path = new ArrayList<>(state.path);
        path.add(action);
        return new ModificationState(correction, path);
    }

    @Override
    public Iterable<Chunk> actionsFor(ModificationState state) {
        return new ArrayList<Chunk>(CollectionUtils.subtract(targetDiff, state.path));
    }
}

class GoalPredicate implements Predicate<Node<?, ModificationState, ?>> {

    final private List<Chunk> basePath;

    public GoalPredicate(List<Chunk> path){
    basePath = path;
    }

    @Override
    public boolean apply(Node<?, ModificationState, ?> node) {
        return basePath.equals(node.state().path);
    }
}