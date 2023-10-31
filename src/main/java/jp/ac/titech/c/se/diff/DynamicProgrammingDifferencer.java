package jp.ac.titech.c.se.diff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.function.BiPredicate;

public final class DynamicProgrammingDifferencer<T> implements Differencer<T> {
    private final BiPredicate<T, T> equality;

    public DynamicProgrammingDifferencer() {
        this(Object::equals);
    }

    public DynamicProgrammingDifferencer(BiPredicate<T, T> equality) {
        this.equality = equality;
    }

    public static class Node {
        public int cost = 0;
        public boolean fromDiagonal = false;
        public boolean fromHorizontal = false;
        public boolean fromVertical = false;
        public Node(final int cost) {
            this.cost = cost;
        }
    }

    public List<Chunk> computeDiff(final List<T> source, final List<T> target) {
        final Node[][] matrix = new Node[source.size() + 1][target.size() + 1];
        for (int i = 0; i <= source.size(); i++) {
            for (int j = 0; j <= target.size(); j++) {
                matrix[i][j] = new Node(0);
            }
        }
        for (int i = 1; i <= source.size(); i++) {
            matrix[i][0].cost = matrix[i - 1][0].cost + 1;
            matrix[i][0].fromVertical = true;
            matrix[i][0].fromHorizontal = false;
            matrix[i][0].fromDiagonal = false;
        }
        for (int j = 1; j <= target.size(); j++) {
            matrix[0][j].cost = matrix[0][j - 1].cost + 1;
            matrix[0][j].fromVertical = false;
            matrix[0][j].fromHorizontal = true;
            matrix[0][j].fromDiagonal = false;
        }

        for (int i = 1; i <= source.size(); i++) {
            for (int j = 1; j <= target.size(); j++) {
                boolean eq = equality.test(source.get(i - 1), target.get(j - 1));
                final int diagonal = matrix[i - 1][j - 1].cost + (eq ? 0 : 100);
                final int vertical = matrix[i - 1][j].cost + 1;
                final int horizontal = matrix[i][j - 1].cost + 1;
                final int min = Math.min(diagonal, Math.min(horizontal, vertical));

                final Node node = matrix[i][j];
                node.fromVertical = false;
                node.fromHorizontal = false;
                node.fromDiagonal = false;
                node.cost = min;
                if (min == vertical) {
                    node.fromVertical = true;
                }
                if (min == horizontal) {
                    node.fromHorizontal = true;
                }
                if (min == diagonal) {
                    node.fromDiagonal = true;
                }
            }
        }
        return findSolution(matrix, source, target);
    }

    List<Chunk> findSolution(Node[][] matrix, List<T> source, List<T> target) {
        final Stack<Chunk> stack = findSolution0(matrix, source.size(), target.size(), new Stack<>());
        final List<Chunk> result = new ArrayList<>(stack);
        Collections.reverse(result);
        return result;
    }

    Stack<Chunk> findSolution0(Node[][] matrix, final int i, final int j, final Stack<Chunk> path) {
        if (i == 0 && j == 0) {
            return path;
        }

        final Node n = matrix[i][j];
        if (n.fromHorizontal) {
            path.push(new Chunk(Chunk.Type.INS, i, i, j - 1, j));
            return findSolution0(matrix, i, j - 1, path);
        }
        else if (n.fromVertical) {
            path.push(new Chunk(Chunk.Type.DEL, i - 1, i, j, j));
            return findSolution0(matrix, i - 1, j, path);
        }
        else if (n.fromDiagonal) {
            path.push(new Chunk(Chunk.Type.EQL, i - 1, i, j - 1, j));
            return findSolution0(matrix, i - 1, j - 1, path);
        }
        else {
            assert false;
            return null;
        }
    }
}
