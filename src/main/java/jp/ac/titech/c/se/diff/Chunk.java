package jp.ac.titech.c.se.diff;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@RequiredArgsConstructor
@Value
public class Chunk {
    public enum Type {
        MOD, DEL, INS, EQL
    }

    public final Type type;
    public final int sourceStart;
    public final int sourceEnd;
    public final int targetStart;
    public final int targetEnd;

}
