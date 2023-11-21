// hoge
public class Test1 {
    public static void main(String[] args){
        checkArgument(args);

        int rslt = Integer.valueOf(args[0])+Integer.valueOf(args[1]);
        System.out.println(rslt);

    }

    private static void checkArgument(String[] args) {
        if(args.length < 2){
            throw new IllegalArgumentException("need 2 argument");
        }else if(!args[0].matches("-?\\d+") || !args[1].matches("-?\\d+")){
            throw new IllegalArgumentException("argument must be integer");
        }
    }

}
//piyo
//puyo