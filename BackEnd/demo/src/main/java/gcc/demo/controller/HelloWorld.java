package gcc.demo.controller;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConfigurationProperties(prefix = "hello")
public class HelloWorld {

    @RequestMapping("/hello")
    public String hello() {
        return "Hello World";
    }
}
