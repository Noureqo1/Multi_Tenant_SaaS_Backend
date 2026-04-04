package Multi_TenantSaaS.SW452.Project.controller;

import Multi_TenantSaaS.SW452.Project.entity.Order;
import Multi_TenantSaaS.SW452.Project.service.OrderService;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public Order createOrder(
            @RequestParam Long productId,
            @RequestParam int quantity) {

        return orderService.createOrder(productId, quantity);
    }
}
