package jp.ac.titech.c.se.diff;

import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import es.usc.citius.hipster.util.Predicate;
import es.usc.citius.hipster.model.Node;

public final class DiffSearch{
    final List<String> source;
    final List<String> target;

    public DiffSearch(List<String> source, List<String> target){
        this.source = source;
        this.target = target;
        final List<Chunk> targetDiff = computeTargetDiff(source, target);
        List<Chunk> diff = getCorrectDiff(source, target);
        show(diff, true);
    }

    private List<Chunk> computeTargetDiff(List<String> source, List<String> target){
        List<Chunk> diff = new JGitDifferencer.Histogram<String>().computeDiff(source, target);
        diff = Chunkase.degrade(diff, source.size(), target.size());
        return diff;
    }

    private List<Chunk> getCorrectDiff(final List<String> source, final List<String> target) {
        List<Chunk> diff;
        CorrectionDifferencer corrctionDifferencer = new CorrectionDynamicProgrammingDifferencer<String>(source, target);
        List<Chunk> correction = new ArrayList<>();
        correction.add(new Chunk(Chunk.Type.EQL, 3, 4, 11, 12));
        correction.add(new Chunk(Chunk.Type.EQL, 4, 5, 12, 13));
        diff = corrctionDifferencer.computeDiff(correction);
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

    public Differencer<String> getDifferencer(App.DifferencerType differencerType) {
        return switch (differencerType) {
            case dp -> new DynamicProgrammingDifferencer<>();
            case astar -> new AStarDifferencer<>();
            case myers -> new JGitDifferencer.Myers<>();
            case histogram -> new JGitDifferencer.Histogram<>();
        };
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