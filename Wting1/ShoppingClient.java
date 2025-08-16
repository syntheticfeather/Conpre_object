package Wting1;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ShoppingClient {
    // 数据文件路径
    private static final String USER_FILE = "data/users.dat";
    private static final String PRODUCT_FILE = "data/products.dat";
    private static final String ORDER_FILE = "data/orders.dat";

    private User currentUser;
    private Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        new ShoppingClient().init().start();
    }

    // 初始化数据
    private ShoppingClient init() {
        try {
            // 确保数据目录存在
            FileUtil.createFile(USER_FILE);
            FileUtil.createFile(PRODUCT_FILE);
            FileUtil.createFile(ORDER_FILE);
            
            if (!FileUtil.fileExists(PRODUCT_FILE) || FileUtil.readList(PRODUCT_FILE).isEmpty()) {
                List<Product> products = new ArrayList<>();
                products.add(new Product("P001", "智能手机", 3999.99, 100, 4.5, "最新款智能手机，全面屏设计"));
                products.add(new Product("P002", "机械键盘", 299.99, 50, 4.2, "青轴机械键盘，背光设计"));
                products.add(new Product("P003", "纯棉T恤", 99.99, 200, 4.7, "100%纯棉材质，舒适透气"));
                products.add(new Product("P004", "运动鞋", 499.99, 80, 4.4, "轻便跑鞋，减震设计"));
                FileUtil.saveList(products, PRODUCT_FILE);
            }
        } catch (Exception e) {
            System.out.println("初始化数据失败：" + e.getMessage());
        }
        return this;
    }
    // 启动程序
    private void start() {
        System.out.println("=== 欢迎使用购物客户端 ===");
        while (true) {
            if (currentUser == null) {
                showLoginMenu();
            } else {
                showMainMenu();
            }
        }
    }
    // 登录菜单
    private void showLoginMenu() {
        System.out.println("\n【请选择操作】");
        System.out.println("1. 注册  2. 登录  3. 密码找回  4. 退出");
        System.out.print("输入选项：");
        int choice = scanner.nextInt();
        scanner.nextLine(); // 消耗换行符
        switch (choice) {
            case 1: register(); break;
            case 2: login(); break;
            case 3: retrievePassword(); break;
            case 4: System.out.println("谢谢使用！"); 
                    System.exit(0);
            default: System.out.println("无效选项，请重试！");
        }
    }
    // 主菜单
    private void showMainMenu() {
        System.out.println("\n【欢迎回来，" + currentUser.getId() + "】");
        System.out.println("1. 商品浏览  2. 购物车  3. 我的订单  4. 个人中心  5. 退出登录");
        System.out.print("输入选项：");
        int choice = scanner.nextInt();
        scanner.nextLine();
        switch (choice) {
            case 1: browseProducts(); break;
            case 2: showCart(); break;
            case 3: showOrders(); break;
            case 4: personalCenter(); break;
            case 5: currentUser = null; System.out.println("已退出登录"); break;
            default: System.out.println("无效选项，请重试！");
        }
    }
    // 用户注册
    private void register() {
        try {
            List<User> users = FileUtil.readList(USER_FILE);
            System.out.println("\n--- 注册 ---");
            System.out.print("输入自定义ID：");
            String id = scanner.nextLine();
            if (users.stream().anyMatch(u -> u.getId().equals(id))) {
                System.out.println("ID已被注册！");
                return;
            }
            System.out.print("输入手机号：");
            String phone = scanner.nextLine();
            System.out.print("设置密码：");
            String password = scanner.nextLine();
            System.out.print("输入QQ号（可选）：");
            String qq = scanner.nextLine();
            System.out.print("详细地址：");
            String address = scanner.nextLine();

            User user = new User(id, phone, password, address);
            user.setQQ(qq.isEmpty() ? null : qq);
            
            users.add(user);
            FileUtil.saveList(users, USER_FILE);
            System.out.println("注册成功！");
        } catch (Exception e) {
            System.out.println("注册失败：" + e.getMessage());
        }
    }
    // 用户登录
    private void login() {
        try {
            List<User> users = FileUtil.readList(USER_FILE);
            System.out.println("\n--- 登录 ---");
            System.out.print("输入账号（ID/手机号/QQ）：");
            String account = scanner.nextLine();
            System.out.print("输入密码：");
            String password = scanner.nextLine();
            
            currentUser = users.stream()
                    .filter(u -> (u.getId().equals(account) || u.getPhone().equals(account) ||
                            (u.getQQ() != null && u.getQQ().equals(account))) &&
                            u.getPassword().equals(password))
                    .findFirst().orElse(null);
            if (currentUser != null) {
                System.out.println("登录成功！");
            } else {
                System.out.println("密码或账号错误,请重新输入");
            }
        } catch (Exception e) {
            System.out.println("登录失败：" + e.getMessage());
        }
    }
    // 密码找回
    private void retrievePassword() {
        try {
            List<User> users = FileUtil.readList(USER_FILE);
            System.out.println("\n--- 密码找回 ---");
            System.out.print("输入ID：");
            String id = scanner.nextLine();
            System.out.print("输入手机号：");
            String phone = scanner.nextLine();
            System.out.print("输入QQ号：");
            String qq = scanner.nextLine();
            User user = users.stream()
                    .filter(u -> u.getId().equals(id) && u.getPhone().equals(phone) &&
                            (qq.equals(u.getQQ())))
                    .findFirst().orElse(null);
            if (user != null) {
                System.out.println("您的密码是：" + user.getPassword());
            } else {
                System.out.println("账号对应错误,请重新输入");
            }
        } catch (Exception e) {
            System.out.println("找回失败：" + e.getMessage());
        }
    }
    // 商品浏览与搜索
    private void browseProducts() {
        try {
            List<Product> products = FileUtil.readList(PRODUCT_FILE);
            System.out.println("\n--- 商品列表 ---");
            products.forEach(System.out::println);
            System.out.println("\n【操作】1. 搜索  2. 筛选  3. 查看详情  4. 返回");
            System.out.print("选择操作：");
            int choice = scanner.nextInt();
            scanner.nextLine();
            switch (choice) {
                case 1: // 搜索
                    System.out.print("输入商品名关键词：");
                    String keyword = scanner.nextLine();
                    List<Product> result = products.stream()
                            .filter(p -> p.getName().contains(keyword))
                            .collect(Collectors.toList());
                    System.out.println("搜索结果：");
                    result.forEach(System.out::println);
                    break;
                case 2: // 筛选
                    //int filterType = scanner.nextInt();
                    System.out.print("输入最低价格：");
                    double min = scanner.nextDouble();
                    System.out.print("输入最高价格：");
                    double max = scanner.nextDouble();
                    List<Product> priceFiltered = products.stream()
                            .filter(p -> p.getPrice() >= min && p.getPrice() <= max)
                            .collect(Collectors.toList());
                    System.out.println("价格筛选结果：");
                    priceFiltered.forEach(System.out::println);
                    break;
                case 3:// 查看详情
                    System.out.print("输入商品ID：");
                    String pid = scanner.nextLine();
                    Product p = products.stream().filter(prod -> prod.getId().equals(pid)).findFirst().orElse(null);
                    if (p != null) {
                        showProductDetail(p);
                    } else {
                        System.out.println("商品不存在！");
                    }
                    break;
                case 4: return;
                default: System.out.println("无效选项！");
            }
        } catch (Exception e) {
            System.out.println("加载商品失败：" + e.getMessage());
        }
    }
    // 商品详情页
    private void showProductDetail(Product p) {
        System.out.println("\n--- 商品详情 ---");
        System.out.println("ID：" + p.getId());
        System.out.println("名称：" + p.getName());
        System.out.println("价格：¥" + p.getPrice());
        System.out.println("库存：" + p.getStock());
        System.out.println("评分：" + p.getScore());
        System.out.println("描述：" + p.getDescription());

        System.out.println("\n【操作】1. 加入购物车  2. 返回");
        System.out.print("选择操作：");
        int choice = scanner.nextInt();
        scanner.nextLine();
        if (choice == 1) {
            if (p.getStock() <= 0) {
                System.out.println("商品已售罄！");
                return;
            }
            currentUser.getCart().add(p);
            try {
                updateUserdata();
                System.out.println("已加入购物车！");
            } catch (Exception e) {
                System.out.println("操作失败：" + e.getMessage());
            }
        }
    }
    // 购物车管理
    private void showCart() {
        List<Product> cart = currentUser.getCart();
        System.out.println("\n--- 购物车 ---");
        if (cart.isEmpty()) {
            System.out.println("购物车为空！");
            return;
        }
        // 显示购物车商品
        for (int i = 0; i < cart.size(); i++) {
            Product p = cart.get(i);
            System.out.println((i+1) + ". " + p.getName() + " - ¥" + p.getPrice());
        }
        System.out.println("\n【操作】1. 结算  2. 删除商品  3. 返回");
        System.out.print("选择操作：");
        int choice = scanner.nextInt();
        scanner.nextLine();
        switch (choice) {
            case 1: checkout(); break;
            case 2: 
                System.out.print("输入要删除的序号：");
                int idx = scanner.nextInt() - 1;
                scanner.nextLine();
                if (idx >= 0 && idx < cart.size()) {
                    cart.remove(idx);
                    try {
                        updateUserdata();
                        System.out.println("已删除！");
                    } catch (Exception e) {
                        System.out.println("操作失败：" + e.getMessage());
                    }
                } else {
                    System.out.println("序号无效！");
                }
                break;
            case 3: return;
            default: System.out.println("无效选项！");
        }
    }
    // 结算流程
    private void checkout() {
        List<Product> cart = currentUser.getCart();
        if (cart.isEmpty()) {
            System.out.println("购物车为空，无法结算！");
            return;
        }
        // 显示收货信息
        System.out.println("\n--- 收货信息 ---");
        System.out.println("收件人：" + currentUser.getId());
        System.out.println("电话：" + currentUser.getPhone());
        System.out.println("地址：" + currentUser.getAddress());
        System.out.print("确认使用以上信息？(y/n)：");
        if (!scanner.nextLine().equalsIgnoreCase("y")) {
            System.out.println("请先到个人中心修改收货信息！");
            return;
        }
        // 发票选项
        System.out.print("是否需要发票？(y/n)：");
        boolean needInvoice = scanner.nextLine().equalsIgnoreCase("y");
        // 创建订单
        String orderId = "O" + System.currentTimeMillis();
        double total = cart.stream().mapToDouble(Product::getPrice).sum();
        Order order = new Order(orderId, currentUser, cart, total, needInvoice,new Date());
        try {
            // 更新订单数据
            List<Order> allOrders = FileUtil.readList(ORDER_FILE);
            allOrders.add(order);
            FileUtil.saveList(allOrders, ORDER_FILE);
            // 更新用户订单
            currentUser.getOrders().add(order);
            // 清空购物车
            currentUser.getCart().clear();
            // 更新商品库存
            List<Product> products = FileUtil.readList(PRODUCT_FILE);
            cart.forEach(p -> {
                Product prod = products.stream().filter(pr -> pr.getId().equals(p.getId())).findFirst().orElse(null);
                if (prod != null) {
                    prod.setStock(prod.getStock() - 1);
                }
            });
            FileUtil.saveList(products, PRODUCT_FILE);
            // 保存用户数据
            updateUserdata();
            System.out.println("\n订单创建成功！");
        } catch (Exception e) {
            System.out.println("结算失败：" + e.getMessage());
        }
    }
    // 我的订单管理
    private void showOrders() {
        List<Order> orders = currentUser.getOrders();
        System.out.println("\n--- 我的订单 ---");
        if (orders.isEmpty()) {
            System.out.println("暂无订单！");
            return;
        }
        // 按状态分类显示
        System.out.println("1. 待付款  2. 待发货  3. 待收货  4. 待评价  5. 已完成  6. 已取消  7. 全部");
        System.out.print("选择状态：");
        int statusChoice = scanner.nextInt();
        scanner.nextLine();
        
        String status = switch (statusChoice) {
            case 1 -> "待付款";
            case 2 -> "待发货";
            case 3 -> "待收货";
            case 4 -> "待评价";
            case 5 -> "已完成";
            case 6 -> "已取消";
            default -> null;
        };
        
        List<Order> filteredOrders = status == null ? orders : 
                orders.stream().filter(o -> o.getStatus().equals(status)).collect(Collectors.toList());
        
        if (filteredOrders.isEmpty()) {
            System.out.println("无对应状态的订单！");
            return;
        }
        
        filteredOrders.forEach(o -> System.out.println(o));
        System.out.print("\n输入要操作的订单ID：");
        String orderId = scanner.nextLine();
        Order selectedOrder = orders.stream().filter(o -> o.getOrderId().equals(orderId)).findFirst().orElse(null);
        
        if (selectedOrder == null) {
            System.out.println("订单不存在！");
            return;
        }
        
        System.out.println("\n【操作】1. 取消订单  2. 支付订单  3. 返回");
        System.out.print("选择操作：");
        int choice = scanner.nextInt();
        scanner.nextLine();
        
        try {
            switch (choice) {
                case 1: 
                    if (selectedOrder.getStatus().equals("待付款") || selectedOrder.getStatus().equals("待发货")) {
                        selectedOrder.setStatus("已取消");
                        updateOrderdata();
                        System.out.println("订单已取消！");
                    } else {
                        System.out.println("该状态无法取消订单！");
                    }
                    break;
                case 2: 
                    if (selectedOrder.getStatus().equals("待付款")) {
                        selectedOrder.setStatus("待发货");
                        selectedOrder.setPayTime(new Date());
                        updateOrderdata();
                        System.out.println("订单支付成功！");
                    } else {
                        System.out.println("该状态无需支付！");
                    }
                    break;
                case 3: return;
                default: System.out.println("无效选项！");
            }
        } catch (Exception e) {
            System.out.println("操作失败：" + e.getMessage());
        }
    }
    // 个人中心
    private void personalCenter() {
        System.out.println("\n--- 个人中心 ---");
        System.out.println("1. 个人信息  2.订单历史  3. 返回");
        System.out.print("选择操作：");
        int choice = scanner.nextInt();
        scanner.nextLine();
        switch (choice) {
            case 1: editUserInfo(); break;
            case 2: showOrders(); break;
            case 3: return;
            default: System.out.println("无效选项！");
        }
    }
    // 编辑个人信息
    private void editUserInfo() {
        System.out.println("\n--- 个人信息 ---");
        System.out.println("ID：" + currentUser.getId());
        System.out.println("姓名：" + currentUser.getName());
        System.out.println("手机号：" + currentUser.getPhone());
        System.out.println("QQ：" + (currentUser.getQQ() == null ? "未绑定" : currentUser.getQQ()));
        
        System.out.println("\n【操作】1. 修改姓名  2. 修改密码  3. 修改手机号 4. 修改收货地址 5. 返回");
        System.out.print("选择操作：");
        int choice = scanner.nextInt();
        scanner.nextLine();
        try {
            if (choice == 1) {
                System.out.print("输入新姓名：");
                currentUser.setId(scanner.nextLine());
                updateUserdata();
                System.out.println("修改成功！");
            } else if (choice == 2) {
                System.out.print("输入原密码：");
                String oldPwd = scanner.nextLine();
                if (!currentUser.getPassword().equals(oldPwd)) {
                    System.out.println("原密码错误！");
                    return;
                }
                System.out.print("输入新密码（4-6位，需包含数字和字母）：");
                String newPwd = scanner.nextLine();
                if (newPwd.matches("^(?=.*[0-9])(?=.*[a-zA-Z]).{4,6}$")) {
                    currentUser.setPassword(newPwd);
                    updateUserdata();
                    System.out.println("密码修改成功！");
                } else {
                    System.out.println("密码格式错误！");
                }
            } else if (choice == 3) {
                System.out.print("输入新手机号：");
                String newPhone = scanner.nextLine();
                if (newPhone.matches("^1[3-9]\\d{9}$")) {
                    currentUser.setPhone(newPhone);
                    updateUserdata();
                    System.out.println("手机号修改成功！");
                } else {
                    System.out.println("手机号格式错误！");
                }
            }else if (choice == 4) {
                System.out.print("输入新收货地址：");
                String newAddress = scanner.nextLine();
                currentUser.setAddress(newAddress);
                System.out.println("收货地址修改成功！");
            }
        } catch (Exception e) {
            System.out.println("操作失败：" + e.getMessage());
        }
    }

    // 更新用户数据
    private void updateUserdata() throws IOException, ClassNotFoundException {
        List<User> users = FileUtil.readList(USER_FILE);
        users.removeIf(u -> u.getId().equals(currentUser.getId()));
        users.add(currentUser);
        FileUtil.saveList(users, USER_FILE);
    }

    // 更新订单数据
    private void updateOrderdata() throws IOException, ClassNotFoundException {
        List<Order> allOrders = FileUtil.readList(ORDER_FILE);
        allOrders.removeIf(o -> o.getOrderId().equals(currentUser.getId()));
        allOrders.addAll(currentUser.getOrders());
        FileUtil.saveList(allOrders, ORDER_FILE);
    } 
}