package gcc.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gcc.demo.pojo.User;
import gcc.demo.service.UserService;

@RestController
public class UserController {

    @Autowired
    private UserService userService;

    // http://localhost:8080/demo/findById?id=1
    @RequestMapping("/findById")
    public User findById(Integer id) {
        return userService.findById(id);
    }
}
