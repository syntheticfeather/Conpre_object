package JavaStudy.If;
public  class L {

    public void test(int x) {
        if (x > 0) {
            System.out.println("x is positive");
            if (x % 2 == 0) {
                System.out.println("x is even");
            } else {
                System.out.println("x is odd");
            }
        } else if (x < 0) {
            System.out.println("x is negative");
        } else {
            System.out.println("x is zero");
        }
    }

    public static void main(String[] args) {
        L l = new L();
        l.test(5);
    }
}
