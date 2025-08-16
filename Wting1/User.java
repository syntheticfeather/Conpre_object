package Wting1;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class User implements Serializable {
    private String id;             // 自定义ID
    private String phone;          // 手机号（同时作为收货电话）
    private String password;       // 密码
    private String qq;             // QQ号（可选）
    private String address;        // 收货地址
    private List<Order> orders = new ArrayList<>(); // 订单历史
    private List<Product> cart = new ArrayList<>(); // 购物车
    
    // 构造方法
    public User(String id, String phone, String password, String address) {
        this.id = id;
        this.phone = phone;
        this.password = password;
        this.address = address;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getQQ() { return qq; }
    public void setQQ(String qq) { this.qq = qq; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public List<Order> getOrders() { return orders; }
    public List<Product> getCart() { return cart; }

    public String getName() { return this.id; }
}