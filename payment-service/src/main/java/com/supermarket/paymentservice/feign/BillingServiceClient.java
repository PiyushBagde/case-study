package com.supermarket.paymentservice.feign;

import com.supermarket.paymentservice.dto.OrderDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "billing-service", url = "${billing-service.url}")
public interface BillingServiceClient {

    @GetMapping("/bill/admin-biller/getOrderByOrderId/{orderId}")
    OrderDto getOrderByOrderId(@PathVariable int orderId);
}
    
