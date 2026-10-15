package com.ims.inventory.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() throws UnknownHostException {
         return InetAddress.getLocalHost().getHostName();
    }
}
