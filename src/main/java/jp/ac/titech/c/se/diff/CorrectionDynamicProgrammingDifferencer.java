package jp.ac.titech.c.se.diff;

import java.util.Collection;
import java.util.List;
import java.util.function.BiPredicate;

public final class CorrectionDynamicProgrammingDifferencer<T> implements CorrectionDifferencer {

    private final BiPredicate<T, T> equality;

    final DynamicProgrammingDifferencer<T> dpDifferencer;
    final List<T> source, target;
    final int CORRECTION_WEIGHT;
    final int[][] editGraph;

    public CorrectionDynamicProgrammingDifferencer(final List<T> source, final List<T> target) {
        this(Object::equals, source, target);
    }

    public CorrectionDynamicProgrammingDifferencer(BiPredicate<T, T> equality, final List<T> source, final List<T> target) {
        this.equality = equality;
        dpDifferencer = new DynamicProgrammingDifferencer<>(equality);
        this.source = source;
        this.target = target;
        editGraph = new int[source.size()+1][target.size()+1];
        CORRECTION_WEIGHT = source.size() + target.size() + 1;
    }

    void initGraph(){

    }

    @Override
    public List<Chunk> computeDiff(Collection<Chunk> correction){
        for(Chunk c: correction){
            switch (c.type) {
                case DEL:
                    break;                 
                case INS:
                case EQL:
                case MOD:
                    assert false;
                    break;
                default:
                    assert false;
            }
        }
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'computeDiff'");
    }

    public List<Chunk> computeDiff(final List<T> source, final List<T> target) {
        final DynamicProgrammingDifferencer.Node[][] matrix = new DynamicProgrammingDifferencer.Node[source.size() + 1][target.size() + 1];
        for (int i = 0; i <= source.size(); i++) {
            for (int j = 0; j <= target.size(); j++) {
                matrix[i][j] = new DynamicProgrammingDifferencer.Node(0);
            }
        }

        for (int i = 1; i <= source.size(); i++) {
            matrix[i][0].cost = matrix[i - 1][0].cost + 1;
            matrix[i][0].fromHorizontal = true;
            matrix[i][0].fromVertical = false;
            matrix[i][0].fromDiagonal = false;
        }
        for (int j = 1; j <= target.size(); j++) {
            matrix[0][j].cost = matrix[0][j - 1].cost + 1;
            matrix[0][j].fromHorizontal = false;
            matrix[0][j].fromVertical = true;
            matrix[0][j].fromDiagonal = false;
        }

        for (int i = 1; i <= source.size(); i++) {
            for (int j = 1; j <= target.size(); j++) {
                boolean eq = equality.test(source.get(i - 1), target.get(j - 1));
                final int diagonal = matrix[i - 1][j - 1].cost + (eq ? 0 : 100);
                final int horizontal = matrix[i - 1][j].cost + 1;
                final int vertical = matrix[i][j - 1].cost + 1;
                final int min = Math.min(diagonal, Math.min(vertical, horizontal));

                final DynamicProgrammingDifferencer.Node node = matrix[i][j];
                node.fromHorizontal = false;
                node.fromVertical = false;
                node.fromDiagonal = false;
                node.cost = min;
                if (min == horizontal) {
                    node.fromHorizontal = true;
                }
                if (min == vertical) {
                    node.fromVertical = true;
                }
                if (min == diagonal) {
                    node.fromDiagonal = true;
                }
            }
        }
        return dpDifferencer.findSolution(matrix, source, target);
    }

}
