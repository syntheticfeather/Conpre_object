package gcc.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
// 如果要导入其他配置类，可以用@Import注解
// 或者配置类很多，导入selector类，然后在selector类中导入其他配置类
// 可以直接放在自定义的注解中
// @Import(MyConfig.class)
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
