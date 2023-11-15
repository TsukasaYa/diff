package jp.ac.titech.c.se.diff;

import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import es.usc.citius.hipster.model.impl.WeightedNode;
import jp.ac.titech.c.se.diff.DiffSearch.GoalPredicate;

import java.util.ArrayList;

public final class App implements Callable<Integer> {
    enum DifferencerType { dp, astar, myers, histogram }

    @Option(names = {"-d", "--differencer"}, paramLabel = "<t>",
            description = "Specify differencer: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})")
    DifferencerType differencerType = DifferencerType.dp;

    @Option(names = {"--loc"}, description = "Show location")
    boolean showLocation;

    @Option(names = {"--search"}, description = "calculate step")
    boolean search;

    @Option(names = {"--manual"}, description = "make diff with manual correction")
    boolean manual;

    @Parameters(index = "0", description = "Source file")
    Path sourceFile;

    @Parameters(index = "1", description = "Target file")
    Path targetFile;

    @SuppressWarnings("unused")
    @Option(names = "--help", description = "show this help message and exit", usageHelp = true)
    boolean helpRequested;

    @SuppressWarnings("unused")
    @Option(names = "--version", description = "print version and exit", versionHelp = true)
    boolean versionInfoRequested;

    @Override
    public Integer call() throws IOException {
        final List<String> source = Files.readAllLines(sourceFile);
        final List<String> target = Files.readAllLines(targetFile);
        List<Chunk> diff = getDifferencer().computeDiff(source, target);
        diff = Chunkase.degrade(diff, source.size(), target.size());
        //List<Chunk> diff = getCorrectDiff(source, target);
        if(search){
            DiffSearch ds = new DiffSearch(differencerType, source, target);
            ds.search();
            //System.out.printf("step:%d\n",ds.search().size());
        }else if(manual){
            diff = getCorrectDiff(source, target);
            List<Chunk> hisDiff = new JGitDifferencer.Histogram<String>().computeDiff(source, target);
            hisDiff = Chunkase.degrade(hisDiff, source.size(), target.size());
            //show(diff, source, target);
            //show(hisDiff, source, target);
            DiffSearch ds = new DiffSearch(differencerType, source, target);
            GoalPredicate<WeightedNode<Chunk, ModificationState, Integer>> gp = ds.new GoalPredicate<>(hisDiff);
            WeightedNode<Chunk,ModificationState,Integer> prevNode = null;
            System.out.println(gp.apply(new WeightedNode<Chunk,ModificationState,Integer>(prevNode,new ModificationState(diff), null,null,null,null)));
        }else{
            show(diff, source, target);
        }
        return 0;
    }

    private List<Chunk> getCorrectDiff(final List<String> source, final List<String> target) {
        List<Chunk> diff;
        CorrectionDifferencer<String> corrctionDifferencer = new CorrectionDynamicProgrammingDifferencer<>(source, target);
        List<Chunk> correction = new ArrayList<>();
        correction.add(new Chunk(Chunk.Type.EQL, 3, 4, 11, 12));
        correction.add(new Chunk(Chunk.Type.EQL, 4, 5, 12, 13));
        diff = corrctionDifferencer.computeDiff(correction);
        return diff;
    }

    public void show(List<Chunk> diff, List<String> source, List<String> target) {
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

    public Differencer<String> getDifferencer() {
        return switch (differencerType) {
            case dp -> new DynamicProgrammingDifferencer<>();
            case astar -> new AStarDifferencer<>();
            case myers -> new JGitDifferencer.Myers<>();
            case histogram -> new JGitDifferencer.Histogram<>();
        };
    }

    public static void main(String[] args) {
        //args = new String[] { "--loc", "--differencer=histogram", "samples/expand1.c", "samples/expand2.c" };
        final App app = new App();
        final CommandLine cmdline = new CommandLine(app);
        cmdline.setExpandAtFiles(false);
        final int status = cmdline.execute(args);
        System.exit(status);
    }
}
