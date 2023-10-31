package jp.ac.titech.c.se.diff;

import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

public final class App implements Callable<Integer> {
    enum DifferencerType { dp, astar, myers, histogram }

    @Option(names = {"-d", "--differencer"}, paramLabel = "<t>",
            description = "Specify differencer: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})")
    DifferencerType differencerType = DifferencerType.dp;

    @Option(names = {"--loc"}, description = "Show location")
    boolean showLocation;

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
        //diff = Chunkase.degrade(diff, source.size(), target.size());
        show(diff, source, target);
        return 0;
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
            //case dp -> new DynamicProgrammingDifferencer<>();
            case dp -> new DynamicProgrammingDifferencerWithPoint<>();
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
