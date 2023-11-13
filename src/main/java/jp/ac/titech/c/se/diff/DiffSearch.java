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
    final CorrectionDifferencer<String> corrctionDifferencer;

    static int SearchCount = 0;

    public DiffSearch(App.DifferencerType type, List<String> source, List<String> target){
        this.source = source;
        this.target = target;
        corrctionDifferencer = getCorrectionDifferencer(type, source, target);
        targetDiff = computeTargetDiff(source, target);
    }

    private List<Chunk> computeTargetDiff(List<String> source, List<String> target){
        List<Chunk> diff = new JGitDifferencer.Histogram<String>().computeDiff(source, target);
        diff = Chunkase.degrade(diff, source.size(), target.size());
        //diff = getCorrectDiff(source, target);
        return diff;
    }

    //多分テスト用
    private List<Chunk> getCorrectDiff(final List<String> source, final List<String> target) {
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

    public void dumpPath(List<Chunk> path){
        for(Chunk c : path){
            char type = switch (c.type){
                case EQL -> 'E';
                case DEL -> 'D';
                case INS -> 'I';
                case MOD -> 'M';
            };
            System.out.printf("%c",type);
        }
        System.out.printf("\n");
    }

    public CorrectionDifferencer<String> getCorrectionDifferencer(App.DifferencerType differencerType, List<String> source, List<String> target) {
        return switch (differencerType) {
            case dp -> new CorrectionDynamicProgrammingDifferencer<>(source, target);
            case astar -> throw new IllegalArgumentException("yet implementation");
            case myers -> throw new IllegalArgumentException("no implementation");
            case histogram -> throw new IllegalArgumentException("no implementation");
        };
    }
    
    public Collection<WeightedNode<Chunk, ModificationState, Integer>> search(){
        Predicate<WeightedNode<Chunk, ModificationState, Integer>> gp = new GoalPredicate<>(targetDiff);
        return Hipster.createAStar(createProblem()).search(gp).getGoalNode().path();
        //show(targetDiff, true);
    }

    public SearchProblem<Chunk, ModificationState, WeightedNode<Chunk, ModificationState, Integer>> createProblem() {
        return ProblemBuilder.create()
            .initialState(new ModificationState(corrctionDifferencer.computeDiff(new HashSet<>())))
            .defineProblemWithExplicitActions()
            .useActionFunction(this)
            .useTransitionFunction(this)
            .useGenericCostFunction(this, new BinaryOperation<>(Integer::sum, 0, Integer.MAX_VALUE))
            .useHeuristicFunction(this)
            .build();
    }

    @Override
    public Integer estimate(ModificationState state) {
        return CollectionUtils.subtract(targetDiff, state.path).size();
        //全探索
        //return 0;
    }

    @Override
    public Integer evaluate(Transition<Chunk, ModificationState> transition) {
        return 1;
    }

    @Override
    public ModificationState apply(Chunk action, ModificationState state) {
        Collection<Chunk> correction = new HashSet<>(state.correction);
        correction.add(action);
        List<Chunk> path = corrctionDifferencer.computeDiff(correction);

        System.out.printf("<%d:%d>",correction.size(), ++SearchCount);
        dumpPath(path);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return new ModificationState(correction,path);
    }

    @Override
    public Iterable<Chunk> actionsFor(ModificationState state) {
        return new HashSet<Chunk>(CollectionUtils.subtract(targetDiff, state.path));
    }

    class GoalPredicate<N extends Node<Chunk, ModificationState, N>> implements Predicate<N> {

        final private List<Chunk> basePath;

        public GoalPredicate(List<Chunk> path){
        basePath = path;
        }

        @Override
        public boolean apply(N node) {
            return basePath.equals(node.state().path);
        }
    }

}