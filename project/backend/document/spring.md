# Spring(spring boot)

## HTTP

URL查询参数格式: http://ip:port/path?key1=value1&key2=value2&key3=value3...  

### HTTP方法

`post`  
`get`  
`delete`
`put`  
`patch`

## MVC

Model-View-Controller（模型-视图-控制器）三层架构

- **模型**负责数据和业务规则 （实体entity/pojo，数据访问mapper/dao，业务逻辑service）
- **视图**负责数据的展示和用户界面（前端）
- **控制器**负责接收用户输入，协调模型和视图（controller）

### 实体类

1. 简化代码：引入**Lombok**依赖
    - @Data
    - @NoArgsConstructor
    - @AllArgsConstructor

2. 字段的**格式校验**
    - @NotNull
    - @NotEmpty
    - @NotBlank
    - @Size
    - @Min  @Max
    - @Pattern(regexp="正则")

### 控制层中的注解

1. 核心注解
    - `@Controller`（类）：
        - 标识这个类是一个 Web 控制器
        - 通常返回视图名称（前后端**不分离**）
    - `@ResponseBody`（类，方法）：表示该控制器方法的返回值（String、User、Map等）直接作为 HTTP 响应体（JSON/xml格式），而不是视图名称，
    - `@RestController`（类）： 是 @Controller 和 @ResponseBody 的**组合**注解，这时 @RespenseBody 作用于整个类的**所有**方法，将方法返回的对象自动序列化为 HTTP 响应体。用于前后端分离

2. 请求映射注解
    - `@RequestMapping`:**类级别**定义控制器**公共路径前缀**。**方法级别**@RequestMapping(value = "URL路径", method = RequestMethod.HTTP方法) 。注解方法时有下面四个用法更简单的组合注解：
        1. `@GetMapping`
        2. `@PostMapping`
        3. `@PutMapping`
        4. `@DeletMapping`
        5. `@PatchMapping`

3. 其他
`@RequestBody`  将请求体中的JSON/Xml转化为java对象
`@PathVariavle` 将路径中的参数绑定到方法参数上
`@Valid` 表示该参数要经受 Bean Validation注解 的校验

### IOC（控制反转）

把主程序的**创建对象**控制权反转给IOC容器，避免在调用时手动new对象，实现解耦

- @Controller
- @Service
- @Repository
- @Component

要生效必须被`@ComponentScan`扫描到，在 Spring Boot 启动类上，使用 `@SpringBootApplication` 注解，它包含了组件扫描的功能,自动扫描**启动类所在包及其子包**

### DI（依赖注入）

1. 基于`@Autowired`依赖注入
    - 属性注入
        - 优：代码简洁，开发快速
        - 缺：隐藏了类之间的依赖关系，可能破坏封装（通过反射赋值）
    - 构造方法注入
        - 优：依赖关系清晰，代码更安全（final修饰属性）
        - 缺：代码繁琐
    - setter方法注入
        - 优：保持封装，依赖关系清晰
        - 缺：代码繁琐

2. 有多个实现类：
    - `@Primary`修饰优先注入的类
    - `@Qualifier`指定
    - `@Resource(name=" ")`指定

### 数据访问层（mybatis）

@Mapper

- 作用：标记**接口**，让` MyBatis `在编译时能够找到这些接口，并为它们自动生成动态代理实现类。不需要再手动编写mapper层接口的实现类
- 使用：
  - 在每个数据访问接口上**单独**使用
  - 在**主应用类上**用`@MapperScan("com.example.demo.mapper")` MyBatis 会自动扫描该包及其子包下的所有接口
- 关联 SQL 语句：
  - 静态 sql：使用**注解**。直接在方法上使用 @Select, @Insert, @Update, @Delete 等语句中的占位符用`#{ }`
  - 动态 sql：**xml配置文件**，接口方法名与 XML 中的 id 对应

- `@Options`：插入记录时设置自增id（配合 @Insert ）

- `@Param`：给Mapper接口的方法参数指定其在SQL/xml配置文件中的名称,要与注解中的SQL的`#{ }`一致

## 异常处理

### 流程

### 全局异常处理器

`@ControllerAdvice`：自动捕获所有 Controller 层抛出的异常，转化成统一的结果响应体

### 错误码枚举

## ResponseEntity<T>

控制HTTP响应

## JWT认证

- 组成 Header.Payload.Signature
    1. Header:头部，一个JSON对象，描述元数据，如签名算法和类型
    2. Payload：一个JSON对象，包含用户标识，过期时间，签发时间
    3. Signature:签名，用于验证消息在传递过程有没有被修改,将编码后的 Header和Payload、一个密钥（Secret）通过 Header 中指定的算法进行签名

- 密钥和期限写在配置文件中（不应该写死，后续应优化），通过`@Value`注入

- 认证流程
    1. 登录
    2. 生成JWT
    3. 返回JWT给客户端，客户端保存
    4. 后续请求携带JWT
    5. 服务器验证是否过期
    6. 从Payload中读取用户信息
    7. 返回相应

- 生成token

``` java
String token=Jwts.builder()
    .setSubject(" ")
    .claim()
    .setIssuedAt()
    .setExpiration()
    .signWith()
    .compact();
```

## 事务操作

### @Transaction 声明式事务管理

## 测试

### 控制器测试

- `@WebMvcTest`
  - 只初始化 Web 层相关的 Bean，不加载 Service
  - 专注于控制器(Controller)、JSON 序列化、过滤器等 Web 层逻辑的测试
  - 自动配置 MockMvc

- `MockMvc`(允许在不启动 HTTP 服务器的情况下测试控制器)

- 使用

``` java
// 测试单个
@WebMvcTest(GreetingController.class) //测试多个用,隔开，不指定即测试所有控制器
class GreetingControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testGreetingEndpoint() throws Exception {
        mockMvc.perform(get("/greeting"))
               .andExpect(status().isOk())
               .andExpect(content().string("Hello World"));
    }
}
```

