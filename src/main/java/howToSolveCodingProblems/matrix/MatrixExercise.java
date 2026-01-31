package howToSolveCodingProblems.matrix;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MatrixExercise {
	
	public static String formatMatrix(List<List<Integer>> matrix) { //O(m*n)
		if(matrix == null || matrix.isEmpty() || matrix.get(0) == null || matrix.get(0).isEmpty()) {
			return null; //O(1)
		}
		
		StringBuilder sb = new StringBuilder();
		for(List<Integer> row : matrix) { //O(m)
			for(Integer item : row) { //O(n)
				sb.append(item).append(" ");
			}
			sb.append("\n");
		}
		
		return sb.toString();
	}
	
	
	// the optimization here becomes from the use of streams, map e joining functions, reducing the number of iterations on elements
	public static String formatMatrixOptimized(List<List<Integer>> matrix) { //O(m*n)
		if(matrix == null || matrix.isEmpty() || matrix.get(0) == null || matrix.get(0).isEmpty()) {
			return null; //O(1)
		}
		
		return matrix.stream()
				.map(row -> row.stream() //O(m)
						.map(Object::toString) //O(n)
						.collect(Collectors.joining(" "))) //O(n)
				.collect(Collectors.joining("\n")); //O(m)
	}
	
	// the optimization here becomes from the maximum level of nested loops with 2 (one for horizontal and one for vertical)
	// the use of while loop to mark matched elements also result in improvement, since only the needed items are considered
	public static List<List<Integer>> match3(List<List<Integer>> matrix) { // O(m*n)
		if (matrix == null || matrix.isEmpty() || matrix.get(0) == null || matrix.get(0).isEmpty()) {
			return null; // O(1)
		}
		
		int rows = matrix.size();
		int cols = matrix.get(0).size();
		
		boolean[][] toReplace = new boolean[rows][cols];
		
		// O(m*n)
		// Check horizontal matches
		for (int i = 0; i < rows; i++) { // traverse rows
			for (int j = 0; j < cols - 2; j++) { // traverse each column, not considering the last 2 columns (where there is no chance to start a 3 match)
				if (matrix.get(i).get(j).equals(matrix.get(i).get(j + 1)) &&
						matrix.get(i).get(j).equals(matrix.get(i).get(j + 2))) { // check if the next two columns have the same value from the current column
					int k = j;
					while (k < cols && matrix.get(i).get(j).equals(matrix.get(i).get(k))) { // mark the columns to be replaced with 0 until the same number repeats
						toReplace[i][k] = true;
						k++;
					}
					j = k - 1; // increment the next column with the column after the 3 match
				}
			}
		}
		
		// O(m*n)
		// Check vertical matches (same logic from columns)
		for (int j = 0; j < cols; j++) {
			for (int i = 0; i < rows - 2; i++) {
				if (matrix.get(i).get(j).equals(matrix.get(i + 1).get(j)) &&
						matrix.get(i).get(j).equals(matrix.get(i + 2).get(j))) {
					int k = i;
					while (k < rows && matrix.get(i).get(j).equals(matrix.get(k).get(j))) {
						toReplace[k][j] = true;
						k++;
					}
					i = k - 1;
				}
			}
		}
		
		// O(m*n)
		// Replace marked elements with 0
		List<List<Integer>> resultMatrix = new ArrayList<>();
		for (int i = 0; i < rows; i++) {
			List<Integer> row = new ArrayList<>();
			for (int j = 0; j < cols; j++) {
				if (toReplace[i][j]) {
					row.add(0);
				} else {
					row.add(matrix.get(i).get(j));
				}
			}
			resultMatrix.add(row);
		}
		
		return resultMatrix;
	}
	
	public static void main(String[] args) {
		// given a matrix of numbers M x N (with numbers 1-9 only)
		// 1   2   3   4   5
		// 1   1   1   3   6
		// 1   3   4   5   7
		// 2   6   4   5   8
		// 3   2   7   5   9
		// 8   8   8   6   7
		List<List<Integer>> matrix = new ArrayList<>();
		matrix.add(List.of(1,2,3,4,5));
		matrix.add(List.of(1,1,1,3,6));
		matrix.add(List.of(1,3,4,5,7));
		matrix.add(List.of(2,6,4,5,8));
		matrix.add(List.of(3,2,7,5,9));
		matrix.add(List.of(8,8,8,6,7));
		
		// format the matrix as a string with line break for each line and a space between each item
		// 1 2 3 4 5\n1 1 1 3 6\n...
		System.out.println(formatMatrix(matrix));
		System.out.println(formatMatrixOptimized(matrix));
		
		// play the match 3 game and build a output matrix with 0 value where there is a 3 match, considering only vertical and horizontal (not diagonal)
		// 0   2   3   4   5
		// 0   0   0   3   6
		// 0   3   0   0   7
		// 2   6   0   0   8
		// 3   2   7   0   9
		// 0   0   0   6   7
		System.out.println(match3(matrix));
	}
}
