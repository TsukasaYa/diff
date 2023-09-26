package jp.ac.titech.c.se.diff;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.diff.*;

public interface JGitDifferencer {
    class Myers<T> implements Differencer<T> {
        @Override
        public List<Chunk> computeDiff(List<T> source, List<T> target) {
            EditList edits = MyersDiff.INSTANCE.diff(new SeqComparator<>(), new Seq<>(source), new Seq<>(target));
            return toChunkList(edits);
        }
    }

    class Histogram<T> implements Differencer<T> {
        @Override
        public List<Chunk> computeDiff(List<T> source, List<T> target) {
            EditList edits = new HistogramDiff().diff(new SeqComparator<>(), new Seq<>(source), new Seq<>(target));
            return toChunkList(edits);
        }
    }

    static List<Chunk> toChunkList(EditList edits) {
        List<Chunk> result = new ArrayList<>();
        for (Edit edit : edits) {
            Chunk.Type type = switch (edit.getType()) {
                case DELETE -> Chunk.Type.DEL;
                case INSERT -> Chunk.Type.INS;
                case REPLACE -> Chunk.Type.MOD;
                default -> Chunk.Type.EQL;
            };
            result.add(new Chunk(type, edit.getBeginA(), edit.getEndA(), edit.getBeginB(), edit.getEndB()));
        }
        return result;
    }

    class SeqComparator<T> extends SequenceComparator<Seq<T>> {
        @Override
        public boolean equals(Seq<T> a, int ai, Seq<T> b, int bi) {
            T source = a.list.get(ai);
            T target = b.list.get(bi);
            return source.equals(target);
        }

        @Override
        public int hash(Seq<T> seq, int ptr) {
            return seq.list.get(ptr).hashCode();
        }
    }

    class Seq<T> extends Sequence {
        final List<T> list;

        public Seq(List<T> list) {
            this.list = list;
        }

        @Override
        public int size() {
            return list.size();
        }
    }
}
