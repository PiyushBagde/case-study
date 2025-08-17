package com.supermarket.billingservice.feign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.supermarket.billingservice.dto.CartItemResponse;

@FeignClient(name = "cart-service", url = "${cart-service.url}")
public interface CartServiceClient {

	@GetMapping("/cart/getCartItemsByUserId/{userId}")
	List<CartItemResponse> getCartItemsByUserId(@PathVariable int userId);

	@GetMapping("/cart/getCartIdByUserId/{userId}")
	int getCartIdByUserId(@PathVariable int userId);

}
