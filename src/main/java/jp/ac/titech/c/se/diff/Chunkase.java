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
            // EQL in DP or AStar
            if(s.type.equals(Chunk.Type.EQL)){
                products.add(new Chunk(Chunk.Type.EQL, sourceLine, ++sourceLine, targetLine, ++targetLine));
            }
            // EQL in Hipster4J
            while(sourceLine < s.sourceStart && targetLine < s.targetStart){
                products.add(new Chunk(Chunk.Type.EQL, sourceLine, ++sourceLine, targetLine, ++targetLine));
            }
            // DEL
            while(sourceLine < s.sourceEnd){
                products.add(new Chunk(Chunk.Type.DEL, sourceLine, ++sourceLine, targetLine, targetLine));
            }
            // INS
            while(targetLine < s.targetEnd){
                products.add(new Chunk(Chunk.Type.INS, sourceLine, sourceLine, targetLine, ++targetLine));
            }
        }

        return products;
    }

}
