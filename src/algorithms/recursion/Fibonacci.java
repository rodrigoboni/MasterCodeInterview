package algorithms.recursion;

import java.util.ArrayList;
import java.util.List;

public class Fibonacci {
	/*
	Given a number N return the index value of the Fibonacci sequence.
	
	Sequence: 0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144 ...
	Index:    0  1  2  3  4  5  6   7   8   9   10  11  12
	(the pattern of the sequence is that each value is the sum of the 2 previous values, that means that for N=5 → 2+3)
	*/
	
	public static int fibonacciInteractive(int n) { // O(n) - linear time - only one loop with N iterations
		List<Integer> arr = new ArrayList<>();
		// add initial items (lower than 2)
		arr.add(0);
		arr.add(1);
		
		// fill the array with calculated numbers until N position
		for(int i=2; i<=n; i++) {
			arr.add(arr.get(i-2) + arr.get(i-1));
		}
		
		return arr.get(n); // return the value at the index n
	}
	
	public static int fibonacciRecursive(int n) { // O(2^n) - exponential time - recursive result in 2 power of N iterations
		if(n < 2) { // 0, 1 - base case
			return n;
		}
		
		return fibonacciRecursive(n-1) + fibonacciRecursive(n-2); // recursive case
	}
	
	public static void main(String[] args) {
		System.out.println(fibonacciInteractive(11)); // 89
		System.out.println(fibonacciRecursive(11)); // 89
	}
	
}
