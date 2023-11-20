package jp.ac.titech.c.se.diff;

import java.util.List;
import java.util.Collection;
import java.util.HashSet;

import es.usc.citius.hipster.util.Predicate;
import es.usc.citius.hipster.model.Node;
import es.usc.citius.hipster.algorithm.Algorithm;
import es.usc.citius.hipster.algorithm.Hipster;
import es.usc.citius.hipster.algorithm.Algorithm.SearchResult;
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
    final static int STEPWEIGHT = 1000;
    final boolean detail;

    static int SearchCount = 0;

    public DiffSearch(App.DifferencerType type, List<String> source, List<String> target, boolean detail){
        this.source = source;
        this.target = target;
        this.detail = detail;
        corrctionDifferencer = getCorrectionDifferencer(type, source, target);
        targetDiff = computeTargetDiff(source, target);
    }

    private List<Chunk> computeTargetDiff(List<String> source, List<String> target){
        List<Chunk> diff = new JGitDifferencer.Histogram<String>().computeDiff(source, target);
        //diff = Chunkase.degrade(diff, source.size(), target.size());
        //diff = getCorrectDiff(source, target);
        return diff;
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

    public String getPathAsString(List<Chunk> path){
        StringBuffer sb = new StringBuffer();
        for(Chunk c : path){
            char type = switch (c.type){
                case EQL -> 'E';
                case DEL -> 'D';
                case INS -> 'I';
                case MOD -> 'M';
            };
            sb.append(type);
        }
        return sb.toString();
    }

    public String getCorrectionAsString(Collection<Chunk> correction){
        StringBuffer sb = new StringBuffer();
        for(Chunk c : correction){
            char type = switch (c.type){
                case EQL -> 'E';
                case DEL -> 'D';
                case INS -> 'I';
                case MOD -> 'M';
            };
            sb.append(String.format("[%c:%d,%d]", type, c.sourceStart, c.targetStart));
            //sb.append(type);
            //sb.append(c.sourceStart);
            //sb.append(c.targetStart);
        }
        return sb.toString();
    }

    public CorrectionDifferencer<String> getCorrectionDifferencer(App.DifferencerType differencerType, List<String> source, List<String> target) {
        return switch (differencerType) {
            case dp -> new CorrectionDynamicProgrammingDifferencer<>(source, target);
            case astar -> throw new IllegalArgumentException("yet implementation");
            case myers -> throw new IllegalArgumentException("no implementation");
            case histogram -> throw new IllegalArgumentException("no implementation");
        };
    }
    
    public void search(){
        Predicate<WeightedNode<Chunk, ModificationState, Integer>> gp = new GoalPredicate<>(targetDiff);
        ModificationState initState = new ModificationState(corrctionDifferencer.computeDiff(new HashSet<>()));

        if(detail){
            System.out.printf("differencer:%s\n", corrctionDifferencer.getClass().getSimpleName());
            System.out.printf("initial:");
            System.out.println(getPathAsString(initState.path));
            System.out.printf("target :");
            System.out.println(getPathAsString(targetDiff));
        }

        var searchResult = Hipster.createAStar(createProblem(initState)).search(gp);
        WeightedNode<Chunk, ModificationState, Integer> result = searchResult.getGoalNode();
        System.out.printf("%d corrections:",result.state().correction.size());
        System.out.println(getCorrectionAsString(result.state().correction));

        if(detail){
            System.out.printf("path   :");
            System.out.println(getPathAsString(result.state().path));
            System.out.println(searchResult.toString());
            /*
            System.out.printf("time   :%d\n",searchResult.getElapsed());
            System.out.printf("iterate:%d\n",searchResult.getIterations());
            */
        }
    }

    public SearchProblem<Chunk, ModificationState, WeightedNode<Chunk, ModificationState, Integer>> createProblem(ModificationState initState) {
        return ProblemBuilder.create()
            .initialState(initState)
            .defineProblemWithExplicitActions()
            .useActionFunction(this)
            .useTransitionFunction(this)
            .useGenericCostFunction(this, new BinaryOperation<>(Integer::sum, 0, Integer.MAX_VALUE))
            .useHeuristicFunction(this)
            .build();
    }

    @Override
    public Integer estimate(ModificationState state) {
        return CollectionUtils.subtract(targetDiff, state.path).size()*STEPWEIGHT;
        //全探索
        //return 0;
    }

    @Override
    public Integer evaluate(Transition<Chunk, ModificationState> transition) {
        return STEPWEIGHT;
    }

    @Override
    public ModificationState apply(Chunk action, ModificationState state) {
        Collection<Chunk> correction = new HashSet<>(state.correction);
        correction.add(action);
        List<Chunk> path = corrctionDifferencer.computeDiff(correction);

        String pathString = getPathAsString(path);
        int sameNum = CollectionUtils.intersection(targetDiff,path).size();
        String correctionString = getCorrectionAsString(correction);
        System.out.println(String.format("<%d:%d> %s",SearchCount++, sameNum, correctionString));

        /*
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        */

        return new ModificationState(correction,path);
    }

    @Override
    public Iterable<Chunk> actionsFor(ModificationState state) {
        return new HashSet<Chunk>(CollectionUtils.subtract(state.path, targetDiff));
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