package jp.ac.titech.c.se.diff;

import java.util.List;
import java.util.Collection;

public interface CorrectionDifferencer<T> {
    List<Chunk> computeDiff(Collection<Chunk> correction);
}
