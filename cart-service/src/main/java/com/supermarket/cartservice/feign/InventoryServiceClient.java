package com.supermarket.cartservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

import com.supermarket.cartservice.dto.ProductResponse;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "inventory-service", url = "${inventory-service.url}")
public interface InventoryServiceClient {

	@GetMapping("/invent/biller-customer/getProductById/{id}")
	ProductResponse getProductById(@PathVariable int id) ;
	
	@PutMapping("/invent/reduceStock/{productId}/{quantity}")
	public void reduceStock(@PathVariable int productId, @PathVariable int quantity);

	@GetMapping("/invent/getProductByProdName")
	ProductResponse getProductByProdName(@RequestParam String prodName);
}
