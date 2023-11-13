package jp.ac.titech.c.se.diff;

import java.util.Collection;
import java.util.List;

import lombok.Value;

import java.util.HashSet;

public class ModificationState {

    Collection<Chunk> correction;
    List<Chunk> path;

    public ModificationState(List<Chunk> path){
        correction = new HashSet<>();
        this.path = path;
    }

    public ModificationState(Collection<Chunk> correction, List<Chunk> path){
        this.correction = correction;
        this.path = path;
    }
    
    public boolean equals(ModificationState obj) {
        // TODO Auto-generated method stub
        return correction.equals(obj.correction);
    }

    @Override
    public int hashCode() {
        return correction.hashCode();
    }
}
