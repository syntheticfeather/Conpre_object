package study.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorld {

    @GetMapping("/hello")
    public String hello() {
        return "Hello, World!";
    }

    // 带参数的GET接口
    @GetMapping("/greet")
    public String greetUser(
            //value是参数名，defaultValue是默认值
            @RequestParam(value = "name", defaultValue = "respected_Guest") String name, int time) {
        return "Hello, " + name + "! Welcome to our service.\nIt has been " + time + " hours since you last logged in.";
    }

    // 返回JSON数据的接口
    @GetMapping("/user")
    public User getUser() {
        return new User(1, "John Doe", "john@example.com");
    }

    // 简单的POJO类
    static class User {

        private int id;
        private String name;
        private String email;

        public User(int id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }

        // Getters
        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }
    }

}
