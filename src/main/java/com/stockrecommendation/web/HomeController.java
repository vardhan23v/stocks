package com.stockrecommendation.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Public landing page. */
@Controller
public class HomeController {

    @GetMapping("/")
    public String index() {
        return "index";
    }
}
