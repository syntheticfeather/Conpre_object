package JavaStudy.Clas;

public class Task_qyx {

    public static void main(String[] args) {
       
    }
}

interface Creature {
    String LOCATION = "Earth"; // 默认访问权限为 public static final

    void eat(int food);

    void sleep(int time);

    void breed(int energy);

    void introduce(); // 用该函数实现多态

    default void move() {
        System.out.println("This creature is moving.");
    }
}

abstract class Animal implements Creature {
    // 种族
    protected String species;

    @Override
    public void eat(int food) {
        System.out.println("Animal eats " + food + " units of food.");
    }

    @Override
    public void sleep(int time) {
        System.out.println("Animal sleeps for " + time + " hours.");
    }

    @Override
    public void breed(int energy) {
        System.out.println("Animal breeds with " + energy + " energy.");
    }

    @Override
    public void introduce() {
        System.out.println("This is an animal of species: " + species);
    }
}

class Human extends Animal {
    // 字段
    private int lifespan; // 寿命
    private String region; // 地域（默认为 80， Asia）

    // 构造方法
    public Human() {
        this.lifespan = 80;
        this.region = "Asia";
    }

    public Human(int lifespan) {
        this.lifespan = lifespan;
        this.region = "Asia";
    }

    public Human(String region) {
        this.lifespan = 80;
        this.region = region;
    }

    public Human(int lifespan, String region) {
        this.lifespan = lifespan;
        this.region = region;
    }

    // EAT方法希望有一个传入float的重载方法
    @Override
    public void eat(int food) {
        System.out.println("Human eats " + food + " units of food.");
    }

    public void eat(float food) {
        System.out.println("Human eats " + food + " units of food.");
    }

    @Override
    public void sleep(int time) {
        System.out.println("Human sleeps for " + time + " hours.");
    }

    @Override
    public void breed(int energy) {
        System.out.println("Human breeds with Energy level " + energy);
    }

    @Override
    public void introduce() {
        System.out.println("This is a human from " + region + ", expected to live " + lifespan + " years.");
    }

    @Override
    public void move() {
        System.out.println("Human moves by walking or running.");
    }
}

class Woman extends Human {
    @Override
    public void eat(float food) {
        System.out.println("This woman eats " + food);
    }

    @Override
    public void sleep(int time) {
        System.out.println("This woman sleeps for " + time + " hours.");
    }

    @Override
    public void breed(int energy) {
        System.out.println("This woman breeds with Energy level " + energy);
    }

    @Override
    public void introduce() {
        System.out.println("This is a woman from " + region + ", expected to live " + lifespan + " years.");
    }
}

class Man extends Human {
    @Override
    public void eat(float food) {
        System.out.println("This man eats " + food);
    }

    @Override
    public void sleep(int time) {
        System.out.println("This man sleeps for " + time + " hours.");
    }

    @Override
    public void breed(int energy) {
        System.out.println("This man breeds with Energy level " + energy);
    }

    @Override
    public void introduce() {
        System.out.println("This is a man from " + region + ", expected to live " + lifespan + " years.");
    }
}




