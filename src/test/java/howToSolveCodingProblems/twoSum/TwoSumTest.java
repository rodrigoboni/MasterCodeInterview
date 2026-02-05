package howToSolveCodingProblems.twoSum;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TwoSumTest {
    private final TwoSum instance = new TwoSum();
    
    @Test
    void basicCase() {
        assertArrayEquals(new int[]{0,1}, instance.twoSum(new int[]{2,7,11,15}, 9));
    }
    
    @Test
    void negativeNumbers() {
        assertArrayEquals(new int[]{0,2}, instance.twoSum(new int[]{-3, 4, 3, 90}, 0));
    }
    
    @Test
    void zeros() {
        assertArrayEquals(new int[]{0, 3}, instance.twoSum(new int[]{0, 4, 3, 0}, 0));
    }
    
    @Test
    void duplicateValues() {
        assertArrayEquals(new int[]{0, 1}, instance.twoSum(new int[]{3, 3}, 6));
    }
    
    @Test
    void cannotReuseSameElement() {
        assertArrayEquals(new int[]{1, 2}, instance.twoSum(new int[]{3, 2, 4}, 6));
    }
    
    @Test
    void minimumSize() {
        assertArrayEquals(new int[]{0, 1}, instance.twoSum(new int[]{1, 2}, 3));
    }
    
    @Test
    void solutionAtEnd() {
        assertArrayEquals(new int[]{3, 4}, instance.twoSum(new int[]{1, 2, 3, 4, 5}, 9));
    }
    
    @Test
    void noSolutionThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.twoSum(new int[]{1, 2, 3}, 100));
    }
}
