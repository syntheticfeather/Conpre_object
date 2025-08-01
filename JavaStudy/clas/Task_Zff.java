package JavaStudy.clas;

public class Task_Zff {
    public static void main(String[] args) {
        Creature animal=new Animal() {};
        Creature human=new Human();
        Creature woman=new Woman();
        Creature man=new Man();
        animal.introduce();
        animal.move();
        human.introduce();
        woman.introduce();
        man.introduce();
        Human testHuman = new Human();
        testHuman.eat(100);
        testHuman.eat(0.1f);
    }
}

interface Creature{
    String LOCATION = "Earth";
    String NAME="Creatrue";//访问权限：所有类都能访问
    void eat(int food);
    void sleep(int time);
    void breed(int Energy);
    void introduce();
    default void move(){
        System.out.println("Creatrue is moving");
    }
}
abstract class Animal implements Creature{
    String species;
    String name;
    @Override
    public void eat(int food){
        System.out.println("The animal eat:"+food);
    }
    @Override
    public void sleep(int time){
        System.out.println("Animal's sleep time:"+time);
    }
    @Override
    public void breed(int Energy){
        System.out.println("Animal's energy to breed:"+Energy);
    }
    @Override
    public void introduce(){
        System.out.println("Animal introduce");
    }
}
class Human extends Animal{
    protected int lifeSpan;
    protected String area;
    public Human(){
        this.lifeSpan=80;
        this.area="Asia";
    }
    public Human(int lifeSpan){
        this.lifeSpan=lifeSpan;
        this.area="Asia";
    }
    public Human(String area){
        this.area=area;
        this.lifeSpan=80;
    }
    public Human(int lifeSpan,String area){
        this.area=area;
        this.lifeSpan=lifeSpan;
    }
    public void eat(float food){
        System.out.println("Human eats "+food+" kg of food");
    }
    @Override
    public void introduce(){
        System.out.println("Human introduce");
    }
    @Override
    public void move(){
        System.out.println("Human is walking");
    }
}
class Woman extends Human{
    @Override
    public void introduce(){
        System.out.println("Woman introduce");
    }
}
class Man extends Human{
    @Override
    public void introduce(){
        System.out.println("Man introduce");
    }
}
