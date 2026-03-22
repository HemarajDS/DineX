package com.example.restaurant.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class RouteConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/login");
        registry.addViewController("/login").setViewName("forward:/pages/login.html");
        registry.addViewController("/guest-menu").setViewName("forward:/pages/customer-menu.html");
        registry.addViewController("/admin").setViewName("forward:/pages/dashboard.html");
        registry.addViewController("/staff").setViewName("forward:/pages/users.html");
        registry.addViewController("/waiter").setViewName("forward:/pages/waiter-dashboard.html");
        registry.addViewController("/kitchen").setViewName("forward:/pages/kitchen-dashboard.html");
        registry.addViewController("/cashier").setViewName("forward:/pages/cashier-dashboard.html");
        registry.addViewController("/menu-studio").setViewName("forward:/pages/menu.html");
        registry.addViewController("/tables").setViewName("forward:/pages/tables.html");
        registry.addViewController("/manual-order").setViewName("forward:/pages/order-create.html");
        registry.addViewController("/order-board").setViewName("forward:/pages/orders.html");
    }
}
