package com.shop.ecommerce.common.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ApiEntryController {

    @GetMapping("/")
    public String openMarketplace() {
        return "forward:/index.html";
    }

    @GetMapping("/docs")
    public String openSwaggerUi() {
        return "redirect:/swagger-ui/index.html";
    }
}
