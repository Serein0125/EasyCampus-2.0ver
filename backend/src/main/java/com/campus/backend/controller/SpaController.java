package com.campus.backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaController {

    @RequestMapping(value = {
            "/",
            "/login",
            "/register",
            "/forgot-password",
            "/products",
            "/products/create",
            "/categories",
            "/messages",
            "/profile",
            "/my-products",
            "/favorites",
            "/address",
            "/settings",
            "/chat/**",
            "/products/**",
            // 社区功能路由
            "/community",
            "/community/**",
            "/boards",
            "/boards/**",
            "/users/**"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
