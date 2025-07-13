package dataStructures.hashtables.SecondMostExpensiveProduct;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * secondMostExpensive exercise
 * Considering a product with name and price
 * Then set a list of products
 * Build an algorithm to extract the second most expensive product(s) from the provided list
 * Product -> name, price
 * Example:
 * “Table”, 400
 * “Chair”, 200
 * “Tablet”, 300
 * “TV”, 400
 * “Phone”, 300
 * “keyboard,” 100
 **/

public class ProductService {
	public static final ProductService INSTANCE = new ProductService();
	
	private ProductService() {
	}
	
	public List<Product> getSecondMostExpensive(final List<Product> products) {
//		final Map<Integer, List<Product>> productMap = new HashMap<>();
//		for(Product product : products) {
//			// this approach doesn't works because the add method of list interface returns a boolean, while the put method expects for a List<Product> type
//			// productMap.put(product.getPrice(), productMap.getOrDefault(product.getPrice(), new ArrayList<>()).add(product));
//
//			// Get the list for the current product's price.
//			// If no list exists for this price, create a new ArrayList,
//			// put it in the map, and return it.
//			// Then, add the current product to the (either new or existing) list.
//			// productMap.computeIfAbsent(product.getPrice(), value -> new ArrayList<>()).add(product);
//
//			// a more classic way to implement this
//			var productsAtPrice = productMap.get(product.getPrice());
//			if(productsAtPrice == null) {
//				productsAtPrice = new ArrayList<>();
//				productMap.put(product.getPrice(), productsAtPrice);
//			}
//
//			productsAtPrice.add(product);
//		}
		
		// this approach achieves the same result with just one line using streams api
		final Map<Integer, List<Product>> productMap = products.stream().collect(Collectors.groupingBy(Product::getPrice));
		
		// based on the keys of the map (with prices) get an ordered set, from highest to lowest price
		final var orderedPrices = productMap.keySet().stream().sorted(Comparator.reverseOrder()).toList();
		
		// handle edge cases (when there is no second price tier)
		if(orderedPrices.size() < 2) {
			return Collections.emptyList();
		}
		
		// get and return the second most expensive products
		return productMap.get(orderedPrices.get(1));
	}
}
