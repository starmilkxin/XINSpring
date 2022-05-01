package com.forTest.service;

import com.spring.annotations.Autowired;
import com.spring.annotations.Component;

@Component
public class UserService{
    @Autowired
    private PersonService personService;

    public void ok() {
        System.out.println(this.getClass());
        System.out.println(personService.getClass());
    }
}
