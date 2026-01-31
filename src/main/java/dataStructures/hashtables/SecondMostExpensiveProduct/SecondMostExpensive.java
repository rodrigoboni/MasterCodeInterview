package dataStructures.hashtables.SecondMostExpensiveProduct;

import java.util.List;

public class SecondMostExpensive {
	
	public static void main(String[] args) {
		final var products = List.of(
				new Product("Table", 400),
				new Product("Chair", 200),
				new Product("Tablet", 300),
				new Product("TV", 400),
				new Product("Phone", 300),
				new Product("Keyboard", 100)
		);
		
		System.out.println(ProductService.INSTANCE.getSecondMostExpensive(products));
	}
}
