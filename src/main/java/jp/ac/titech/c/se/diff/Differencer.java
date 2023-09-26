package jp.ac.titech.c.se.diff;

import java.util.List;

public interface Differencer<T> {
    List<Chunk> computeDiff(List<T> source, List<T> target);
}
