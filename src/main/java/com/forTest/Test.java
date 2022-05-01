package com.forTest;

import com.spring.XINApplicationContext;
import com.forTest.service.PersonService;
import com.forTest.service.UserService;

public class Test {
    public static void main(String[] args) {
        XINApplicationContext applicationContext = new XINApplicationContext(MyConfig.class);
        UserService userService = (UserService) applicationContext.getBean("userService");
        userService.ok();
        PersonService personService = (PersonService) applicationContext.getBean("personService");
        personService.ok();
    }
}
