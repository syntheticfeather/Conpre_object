package Wting1;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Order implements Serializable {
    private String orderId;                // 订单ID
    private String userId;                 // 用户ID
    private List<Product> products;        // 商品列表
    private double totalAmount;            // 总金额
    private String status;                 // 订单状态：待付款、待发货、待收货、待评价、已完成、已取消、退款中
    private String recipientName;          // 收件人姓名（来自用户信息）
    private String recipientPhone;         // 收件人电话（来自用户信息）
    private String address;                // 配送地址（来自用户信息）
    private boolean needInvoice;           // 是否需要发票
    private Date createTime;               // 创建时间
    private Date payTime;                  // 支付时间
    private String refundReason;           // 退款原因

    // 构造方法（直接从用户信息获取收件信息）
    public Order(String orderId, User user, List<Product> products, double totalAmount, boolean needInvoice, Date createTime) {
        this.orderId = orderId;
        this.userId = user.getId();
        this.products = new ArrayList<>(products);
        this.totalAmount = totalAmount;
        this.recipientName = user.getId();
        this.recipientPhone = user.getPhone();
        this.address = user.getAddress();
        this.needInvoice = needInvoice;
        this.createTime = createTime;
        this.status = "待付款";
    }

    // Getters and Setters
    public String getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public List<Product> getProducts() { return products; }
    public double getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }   
    public void setStatus(String status) { this.status = status; }
    public boolean isNeedInvoice() { return needInvoice; }
    public Date getCreateTime() { return createTime; }
    public Date getPayTime() { return payTime; }
    public void setPayTime(Date payTime) { this.payTime = payTime; }
    public String getRefundReason() { return refundReason; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }
    
    @Override
    public String toString() {
        return "订单ID：" + orderId + " | 状态：" + status + " | 总价：¥" + totalAmount + 
               " | 收件人：" + recipientName + " | 电话：" + recipientPhone + 
               " | 地址：" + address + " | 创建时间：" + createTime;
    }
}