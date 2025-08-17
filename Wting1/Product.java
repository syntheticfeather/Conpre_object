package Wting1;

import java.io.Serializable;

public class Product implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;             // 商品ID
    private String name;           // 商品名称
    private double price;          // 商品价格
    private int stock;             // 库存数量
    private String description;    // 商品描述
    private double score;         // 商品评分

    // 构造方法、getter和setter
    public Product(String id, String name, double price, int stock,double score ,String description) {
        this.id = id;
        this.price = price;
        this.stock = stock;
        this.description = description;
        this.name = name;
        this.score = score;
    }

    // Getters and Setters
    public String getId() {
        return id; 
    }
    public void setId(String id) {
        this.id = id; 
    }
    public String getName() {
        return name; 
    }
    public void setName(String name) {
        this.name = name; 
    }
    
    public double getPrice() {
        return price; 
    }
    public void setPrice(double price) {
        this.price = price; 
    }
    
    public int getStock() {
        return stock; 
    }
    public void setStock(int stock) {
        this.stock = stock; 
    }   
    public double getScore() {
        return score; 
    }
    public void setScore(double score) {
        this.score = score; 
    }
    public String getDescription() {
        return description; 
    }
    public void setDescription(String description) {
        this.description = description; 
    }
    @Override
    public String toString() {
        return String.format("%s. %-15s | 价格: ¥%-8.2f | 库存: %-3d | 评分: %.1f",
                id, name, price, stock, score);
    }
}
