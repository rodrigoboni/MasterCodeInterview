package dataStructures.hashtables.SecondMostExpensiveProduct;

public class Product {
	private final String name;
	private final int price;
	
	public Product(String name, int price) {
		this.name = name;
		this.price = price;
	}
	
	public int getPrice() {
		return price;
	}
	
	@Override
	public String toString() {
		return "Product{" +
				"name='" + name + '\'' +
				", price=" + price +
				'}';
	}
}
