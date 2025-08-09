package zff;

public class User {
    private String userName;
    private String password;
    private String personId;
    private String phoneNumber;

    public User(){

    }
    public User(String userName,String password,String personId,String phoneNumber){
        this.userName=userName;
        this.password=password;
        this.personId=personId;
        this.phoneNumber=phoneNumber;
    }

    public String getUserName(){
        return this.userName;
    }
    public void setUserName(String name){
        this.userName=name;
    }

    public String getPassword(){
        return this.password;
    }
    public void setPassword(String password){
        this.password=password;
    }

    public String getPersonId(){
        return this.personId;
    }
    public void setPersonId(String id){
        this.personId=id;
    }

    public String getPhoneNumber(){
        return this.phoneNumber;
    }
    public void setPhoneNumber(String phoneNumber){
        this.phoneNumber=phoneNumber;
    }
}