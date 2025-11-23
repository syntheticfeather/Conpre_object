package zff;

import java.util.ArrayList;
import java.util.Scanner;

public class StuSystem{
    public static void useStudentSystem(){
        ArrayList<Student> list=new ArrayList<>();
        Scanner sc=new Scanner(System.in);
        while (true) { 
            //初始菜单
            System.out.println("--------------学生管理系统--------------");
            System.out.println("1:添加学生");
            System.out.println("2:删除学生");
            System.out.println("3:修改学生");
            System.out.println("4:查询学生");
            System.out.println("5:退出");
            System.out.println("请输入选择：");
            String choose=sc.next();
            switch(choose){
                case "1" -> addStudent(list);
                case "2" -> deleteStudent(list);
                case "3" -> updateStudent(list);
                case "4" -> queryStudent(list);
                case "5" -> {
                    System.out.println("退出");
                    System.exit(0);
                }
                default -> System.out.println("没有这个选项");
            }
        }
    }
    //添加学生
    public static void addStudent(ArrayList<Student> list){
        Student s=new Student();

        Scanner sc=new Scanner(System.in);
        while (true) { 
            System.out.println("请输入学号:");
            String id=sc.next();
            boolean flag=isContained(list, id);
            if(flag){
                System.out.println("该学号已存在,请重新输入");
            }else{
                s.setId(id);
                break;
            }
        }

        System.out.println("请输入姓名:");
        String name=sc.next();
        s.setName(name);

        System.out.println("请输入年龄:");
        int age=sc.nextInt();
        s.setAge(age);

        System.out.println("请输入学生住址:");
        String address=sc.next();
        s.setAddress(address);

        list.add(s);
        System.out.println("信息添加成功");
    }

    //删除学生
    public static void deleteStudent(ArrayList<Student> list){
        Scanner sc=new Scanner(System.in);
        System.out.println("请输入要删除的学号:");
        String id=sc.next();
        int index=getIndex(list, id);
        if(index>=0){
            list.remove(index);
            System.out.println("删除成功");
        }else{
            System.out.println("该学号不存在,删除失败");
        }
    }

    //修改学生
    public static void updateStudent(ArrayList<Student> list){
        System.out.println("请输入需要修改的学生的学号:");
        Scanner sc=new Scanner(System.in);
        String id=sc.next();
        int index=getIndex(list, id);
        if(index==-1){
            System.out.println("该学号不存在,请重新输入");
        }
        Student stu=list.get(index);
        
    }

    //查询学生
    public static void queryStudent(ArrayList<Student> list){
        if(list.size()==0){
            System.out.println("当前无学生信息，请添加后再查询");
            return;
        }
        System.out.println("学号\t\t姓名\t年龄\t住址");
        for(int i=0;i<list.size();i++){
            Student stu=list.get(i);
            System.out.println(stu.getId()+"\t\t"+stu.getName()+"\t"+stu.getAge()+"\t"+stu.getAddress());
        }
    }

    //判断学号是否已经存在
    public static boolean isContained(ArrayList<Student> list,String id){
        return getIndex(list, id)>=0;
    }

    //通过学号获取索引
    public static int getIndex(ArrayList<Student> list,String id){
        for(int i=0;i<list.size();i++){
            Student stu=list.get(i);
            String stuId=stu.getId();
            if(stuId.equals(id)){
                return i;
            }
        }
        return -1;
    }
}