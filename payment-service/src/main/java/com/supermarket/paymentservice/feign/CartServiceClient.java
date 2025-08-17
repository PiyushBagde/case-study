package com.supermarket.paymentservice.feign;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "cart-service", url = "${cart-service.url}")
public interface CartServiceClient {
    @DeleteMapping("/cart/clearCartAndReduceStock/{userId}")
    String clearCartAndReduceStockForUser(@PathVariable int userId);

}
