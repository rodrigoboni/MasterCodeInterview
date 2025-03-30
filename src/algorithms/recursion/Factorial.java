package algorithms.recursion;

public class Factorial {
	public int findFatorialIterative(int number) { // It's a O(n) time complexity because it's linear, a loop that repeat for n times
		if(number < 2) {
			return 1;
		}
		
		int result = 1;
		for(int i = 2; i <= number; i++) {
			result *= i;
		}
		
		return result;
	}
	
	public int findFactorialRecursive(int number) { // It's a O(n) time complexity because the function calls itself n times, it's linear
		if(number < 2) { // base case
			return 1;
		}
		
		return number * findFactorialRecursive(number - 1); // recursive case
	}
	
	public static void main(String[] args) {
		Factorial factorial = new Factorial();
		System.out.println(factorial.findFatorialIterative(5));
		System.out.println(factorial.findFactorialRecursive(5));
	}
}
