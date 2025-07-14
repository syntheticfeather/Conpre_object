package JavaStudy.genericity;

public class L {
    public void test() {        
        int[] arr = {1, 2, 3, 4, 5};
        int l = arr.length;
    }

    public static void main(String[] args) throws Exception {
        new L().test();
        Integer a = 5;        
        System.out.println(a);
    }

}
