package howToSolveCodingProblems.googleInterview;

import java.util.HashSet;
import java.util.Set;

public class GoogleInterviewExercise {

	// naive solution
	// O(n^2) - quadratic time
	public static boolean hasPairWithSum(int[] arr, int sum) {
		for(int i=0; i<arr.length; i++) {
			for(int j=i+1; j<arr.length; j++) {
				if(arr[i] + arr[j] == sum) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	// better solution
	// O(n) - linear time
	public static boolean hasPairWithSum2(int[] arr, int sum) {
		final Set<Integer> numbersMap = new HashSet<>();
		
		for(int i=0; i<arr.length; i++) {
			if(numbersMap.contains(arr[i])) {
				return true;
			} else {
				numbersMap.add(sum - arr[i]);
			}
		}
		
		return false;
	}
	
	public static void main(String[] args) {
		final int[] arr = new int[]{1,2,3,9};
		final int[] arr2 = new int[]{1,2,4,4};
		System.out.println(hasPairWithSum(arr, 8));
		System.out.println(hasPairWithSum(arr2, 8));
		
		System.out.println(hasPairWithSum2(arr, 8));
		System.out.println(hasPairWithSum2(arr2, 8));
	}
}
