package com.supermarket.cartservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
	private int prodId;
    private String prodName;
    private double price;
    private int stock;
    
//
//	public int getProdId() {
//		return prodId;
//	}
//	public void setProdId(int prodId) {
//		this.prodId = prodId;
//	}
//	public String getProdName() {
//		return prodName;
//	}
//	public void setProdName(String prodName) {
//		this.prodName = prodName;
//	}
//	public double getPrice() {
//		return price;
//	}
//	public void setPrice(double price) {
//		this.price = price;
//	}
//	public int getStock() {
//		return stock;
//	}
//	public void setStock(int stock) {
//		this.stock = stock;
//	}
	/*public ProductResponse(int prodId, String prodName, double price, int stock) {
		super();
		this.prodId = prodId;
		this.prodName = prodName;
		this.price = price;
		this.stock = stock;
	}*/
    
    
}
