package zff;

import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class Start {
    public static void main(String[] args) {
        Scanner sc=new Scanner(System.in);
        ArrayList<User> list = FileUtils.loadUsers();
        while (true) { 
            System.out.println("欢迎来到学生管理系统");
            System.out.println("请选择操作: 1.登录 2.注册 3.忘记密码 4.退出");
            String choose=sc.next();
            switch(choose){
                case "1" -> login(list);
                case "2" -> register(list);
                case "3" -> forgetPassword(list);
                case "4" -> {
                    System.out.println("谢谢使用");
                    System.exit(0);
                }
                default -> System.out.println("没有这个选项");
            }
        }
    }

    //登录
    private static void login(ArrayList<User> list){
        Scanner sc=new Scanner(System.in);
        for(int i=0;i<3;i++){
            //输入用户名，判断是否存在
            System.out.println("请输入用户名:");
            String name=sc.next();
            boolean flag=userIsContained(list, name);
            if(!flag){
                System.out.println("用户名"+name+"未注册,请先注册再登录");
                return;
            }

            //输入密码
            System.out.println("请输入密码:");
            String password=sc.next();
    
            //输入验证码
            while (true) { 
                String rightCode=getCode();
                System.out.println("验证码为:"+rightCode);
                System.out.println("请输入验证码:");
                String code=sc.next();
                if(code.equalsIgnoreCase(rightCode)){
                    System.out.println("验证码正确");
                    break;
                }else{
                    System.out.println("验证码错误,请重新输入");
                }
            }
            //用户名和密码是否正确
            User user=new User(name,password,null,null);
            if(checkUserInfo(list, user)){//没有保存
                System.out.println("登录成功");
                StuSystem use=new StuSystem();
                use.useStudentSystem();
                break;
            }else{
                System.out.println("登录失败,用户名或密码错误");
                if(i==2){
                    System.out.println("账号"+name+"已被锁定");
                    return;
                }
            }
        }
    }

    //注册
    private static void register(ArrayList<User> list){
        Scanner sc=new Scanner(System.in);
        String name;
        String password1;
        String personId;
        String phoneNumber;
        //录入用户名
        while(true){
            System.out.println("请输入用户名:");
            name=sc.next();
            boolean flag1=checkUserName(name);
            if(!flag1){
                System.out.println("用户名格式错误,请重新输入");
                continue;
            }
            boolean flag2=userIsContained(list, name);
            if(flag2){
                System.out.println("该用户名已存在,请重新输入");
            }else{
                break;
            }
        }
        //录入密码
        while(true){
            System.out.println("请输入密码:");
            password1=sc.next();
            System.out.println("请再次输入密码:");
            String password2=sc.next();
            if(!password1.equals(password2)){
                System.out.println("两次密码输入不一致");
            }else{
                break;
            }
        }
        //录入身份证号
        while(true){
            System.out.println("请输入身份证号码:");
            personId=sc.next();
            if(!checkPersonId(personId)){
                System.out.println("身份证号码格式错误");
            }else{
                break;
            }
        }
        //录入手机号码
        while(true){
            System.out.println("请输入手机号码:");
            phoneNumber=sc.next();
            if(!checkPhoneNumber(phoneNumber)){
                System.out.println("手机号码格式错误");
            }else{
                break;
            }
        }

        User user=new User(name,password1,personId,phoneNumber);
        list.add(user);
        System.out.println("注册成功");
    }

    //忘记密码
    private static void forgetPassword(ArrayList<User> list){
        Scanner sc=new Scanner(System.in);
        System.out.println("请输入用户名:");
        String userName=sc.next();
        if(!userIsContained(list, userName)){
            System.out.println("用户名"+userName+"未注册,请先注册");
            return;
        }
        //验证身份信息
        System.out.println("请输入身份证号:");
        String personId=sc.next();
        System.out.println("请输入手机号:");
        String phoneNumber=sc.next();

        int index=findUserIndex(list, userName);
        User user=list.get(index);
        if(!(user.getPersonId().equalsIgnoreCase(personId)&&user.getPhoneNumber().equals(phoneNumber))){
            System.out.println("身份证号或手机号错误");
            return;
        }
        //重新设置密码
        String password;
        while(true){
            System.out.println("请输入新的密码:");
            password=sc.next();
            System.out.println("请再次输入密码:");
            String password2=sc.next();
            if(password.equals(password2)){
                break;
            }else{
                System.out.println("两次密码输入不一致,请重新输入");
            }
        }
        user.setPassword(password);
        System.out.println("密码修改成功");
    }

    //验证用户名格式
    private static boolean checkUserName(String userName){
        /*int len=userName.length();
        if(len<3||len>15){
            return false;
        }
        int count=0;
        for(int i=0;i<len;i++){
            char c=userName.charAt(i);
            if(!((c>='a'&&c<='z')||(c>='A'&&c<='Z')||(c>='0'&&c<='9'))){
                return false;
            }
            if((c>='a'&&c<='z')||c>='A'&&c<='Z'){
                count++;
            }
        }
        return count>0;*/
        return userName.matches("^(?![0-9]+$)[a-zA-Z0-9]{3,15}$");
    }

    //判断用户名是否存在
    private static boolean userIsContained(ArrayList<User> list,String userName){
        for(int i=0;i<list.size();i++){
            User user=list.get(i);
            String name=user.getUserName();
            if(name.equals(userName)){
                return true;
            }
        }
        return false;
    }

    //验证身份证号格式
    private static boolean checkPersonId(String personId){
        /*if(personId.length()!=18){
            return false;
        }
        if(personId.startsWith("0")){
            return false;
        }
        for(int i=0;i<personId.length()-1;i++){
            char c=personId.charAt(i);
            if(!(c>='0'&&c<='9')){
                return false;
            }
        }
        char end=personId.charAt(personId.length()-1);
        if((end>='0'&&end<='9')||(end=='x')||(end=='X')){
            return true;
        }else{
            return false;
        }*/
        return personId.matches("^[1-9]\\d{16}[0-9Xx]$");
    }

    //验证手机号码格式
    private static boolean checkPhoneNumber(String phoneNumber){
        /*if(phoneNumber.length()!=11){
            return false;
        }
        if(phoneNumber.startsWith("0")){
            return false;
        }
        for(int i=0;i<phoneNumber.length();i++){
            char c=phoneNumber.charAt(i);
            if(!(c>='0'&&c<='9')){
                return false;
            }
        }
        return true;*/
        return phoneNumber.matches("^[1-9]\\d{10}$");
    }

    //生成验证码
    private static String getCode(){
        ArrayList<Character> list=new ArrayList<>();
        for(int i=0;i<26;i++){
            list.add((char)('a'+i));
            list.add((char)('A'+i));
        }

        StringBuilder sb=new StringBuilder();
        Random r=new Random();
        for(int i=0;i<4;i++){
            int index=r.nextInt(list.size());
            char c=list.get(index);
            sb.append(c);
        }
        int number=r.nextInt(10);
        sb.append(number);

        char[] code=sb.toString().toCharArray();
        int randomIndex=r.nextInt(code.length);
        char temp=code[randomIndex];
        code[randomIndex]=code[code.length-1];
        code[code.length-1]=temp;
        return new String(code);
    }

    //判断用户名和密码是否正确
    private static boolean checkUserInfo(ArrayList<User> list,User userInfo){
        for(int i=0;i<list.size();i++){
            User user=list.get(i);
            String name=user.getUserName();
            String password=user.getPassword();
            if(userInfo.getUserName().equals(name)&&userInfo.getPassword().equals(password)){
                return true;
            }
        }
        return false;
    }

    //通过用户名获取索引
    private static int findUserIndex(ArrayList<User> list,String name){
        for(int i=0;i<list.size();i++){
            User user=list.get(i);
            if(user.getUserName().equals(name)){
                return i;
            }
        }
        return -1;
    }
}
