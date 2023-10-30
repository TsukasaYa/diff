package jp.ac.titech.c.se.diff;

import java.util.List;
import java.util.ArrayList;

public class Chunkase {

    public Chunkase(){}

    static List<Chunk> degrade(List<Chunk> substrate, int sourceSize, int targetSize){
        List<Chunk> products = new ArrayList<>();
        int sourceLine = 0;
        int targetLine = 0;

        for (Chunk s : substrate) {
            while(sourceLine < s.sourceStart && targetLine< s.targetStart){
                products.add(new Chunk(Chunk.Type.EQL, sourceLine, ++sourceLine, targetLine, ++targetLine));
            }

            Chunk.Type type = switch (s.type) {
                case DEL -> Chunk.Type.DEL;
                case INS -> Chunk.Type.INS;
                case MOD -> Chunk.Type.MOD;
                default -> Chunk.Type.EQL;
            };
            
            products.add(new Chunk(s.type, s.sourceStart, s.sourceEnd, s.targetStart, s.targetEnd));
            sourceLine = s.sourceEnd;
            targetLine = s.targetEnd;
        }

        return products;
    }

}
