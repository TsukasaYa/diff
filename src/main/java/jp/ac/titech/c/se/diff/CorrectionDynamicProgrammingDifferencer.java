package jp.ac.titech.c.se.diff;

import java.util.Collection;
import java.util.List;
import java.util.function.BiPredicate;

import java.lang.StringBuffer;

public final class CorrectionDynamicProgrammingDifferencer<T> implements CorrectionDifferencer<T> {

    private final BiPredicate<T, T> equality;

    final DynamicProgrammingDifferencer<T> dpDifferencer;
    final List<T> source, target;
    final int CORRECTION_WEIGHT;
    final int[][][] editGraph;

    public CorrectionDynamicProgrammingDifferencer(final List<T> source, final List<T> target) {
        this(Object::equals, source, target);
    }

    public CorrectionDynamicProgrammingDifferencer(BiPredicate<T, T> equality, final List<T> source, final List<T> target) {
        this.equality = equality;
        dpDifferencer = new DynamicProgrammingDifferencer<>(equality);
        this.source = source;
        this.target = target;
        CORRECTION_WEIGHT = source.size() + target.size() + 1;
        editGraph = new int[source.size()+1][target.size()+1][];
        initGraph();
    }

    void initGraph(){
        // editgraph[][][*] : DEL(vertical), INS(horizontal), EQL(diagonal)
        editGraph[0][0] = new int[]{CORRECTION_WEIGHT,CORRECTION_WEIGHT,CORRECTION_WEIGHT};
        for(int i = 1; i<= source.size(); i++){
            editGraph[i][0] = new int[]{1,CORRECTION_WEIGHT,CORRECTION_WEIGHT};
        }
        for(int j = 1; j<= target.size(); j++){
            editGraph[0][j] = new int[]{CORRECTION_WEIGHT,1,CORRECTION_WEIGHT};
        }
        for(int i = 1; i<= source.size(); i++){
            for(int j=1; j<= target.size(); j++){
                if(equality.test(source.get(i - 1), target.get(j - 1))){
                    editGraph[i][j] = new int[]{1,1,0};
                }else{
                    editGraph[i][j] = new int[]{1,1,CORRECTION_WEIGHT};
                }
            }
        }
    }

    @Override
    public List<Chunk> computeDiff(Collection<Chunk> correction){
        for(Chunk c: correction){
            setWeight(c);
        }
        //showEditGraph();
        DynamicProgrammingDifferencer.Node[][] matrix = makeMatrix();
        for(Chunk c: correction){
            resetWeight(c);
        }
        return dpDifferencer.findSolution(matrix, source, target);
    }

    private void setWeight(Chunk c) {
        editGraph[c.sourceEnd][c.targetEnd][typeToIndex(c.type)] = CORRECTION_WEIGHT;
    }

    private void resetWeight(Chunk c){
        editGraph[c.sourceEnd][c.targetEnd][typeToIndex(c.type)] = 1;
    }

    private int typeToIndex(Chunk.Type t){
        return switch(t){
            case DEL -> 0;
            case INS -> 1;
            case EQL -> 2;
            default -> throw new IllegalStateException();
        };
    }

    void showEditGraph(){
        StringBuffer sb = new StringBuffer("@");
        StringBuffer sb2;
        for(int j = 1; j<= target.size(); j++){
            if(editGraph[0][j][1] == 1){
                sb.append("-@");
            }else{
                sb.append(" @");
            }
        }
        System.out.println(sb.toString());
        for(int i = 1; i<= source.size(); i++){
            sb = new StringBuffer();//ノードとノードの間の行
            if(editGraph[i][0][0] == 1){
                sb.append("|");
            }else{
                sb.append(" ");
            }
            sb2 = new StringBuffer("@"); //ノードのある行
            for(int j=1; j<= target.size(); j++){
                if(editGraph[i][j][2] <= 1){
                    sb.append("\\");
                }else{
                    sb.append(" ");
                }
                if(editGraph[i][j][0] == 1){
                    sb.append("|");
                }else{
                    sb.append(" ");
                }
                if(editGraph[i][j][1] == 1){
                    sb2.append("-@");
                }else{
                    sb2.append(" @");
                }
            }
            System.out.println(sb.toString());
            System.out.println(sb2.toString());
        }
    }

    public DynamicProgrammingDifferencer.Node[][] makeMatrix() {
        final DynamicProgrammingDifferencer.Node[][] matrix = new DynamicProgrammingDifferencer.Node[source.size() + 1][target.size() + 1];
        for (int i = 0; i <= source.size(); i++) {
            for (int j = 0; j <= target.size(); j++) {
                matrix[i][j] = new DynamicProgrammingDifferencer.Node(0);
            }
        }

        for (int i = 1; i <= source.size(); i++) {
            matrix[i][0].cost = matrix[i - 1][0].cost + editGraph[i][0][0];
            matrix[i][0].fromVertical = true;
            matrix[i][0].fromHorizontal = false;
            matrix[i][0].fromDiagonal = false;
        }
        for (int j = 1; j <= target.size(); j++) {
            matrix[0][j].cost = matrix[0][j - 1].cost + editGraph[0][j][1];
            matrix[0][j].fromVertical = false;
            matrix[0][j].fromHorizontal = true;
            matrix[0][j].fromDiagonal = false;
        }

        for (int i = 1; i <= source.size(); i++) {
            for (int j = 1; j <= target.size(); j++) {
                final int diagonal = matrix[i - 1][j - 1].cost + editGraph[i][j][2];
                final int vertical = matrix[i - 1][j].cost + editGraph[i][j][0];
                final int horizontal = matrix[i][j - 1].cost + editGraph[i][j][1];
                final int min = Math.min(diagonal, Math.min(horizontal, vertical));

                final DynamicProgrammingDifferencer.Node node = matrix[i][j];
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
        return matrix;
    }

}
