package com.inventory.inventorymanagementsystem.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        log.info("API Request: GET /hello - Health check endpoint accessed");
        String message = "Inventory Management System Running";
        log.info("API Response: {}", message);
        return message;
    }
}
