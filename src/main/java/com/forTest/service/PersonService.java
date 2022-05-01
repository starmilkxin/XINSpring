package com.forTest.service;

import com.spring.annotations.Autowired;
import com.spring.annotations.Component;

@Component
public class PersonService {
    @Autowired
    private UserService userService;

    public void ok() {
        System.out.println(this.getClass());
        System.out.println(userService.getClass());
    }
}
