package howToSolveCodingProblems.twoSum;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * # Two Sum - Hash Map Pattern Analysis
 * Given an array of integers nums and an integer target, return indices of the two numbers that add up to target. Each input has exactly one solution. You may not use the same element twice.
 * Example input: nums = [2, 7, 11, 15], target = 9
 * Expected output: [0,1]
 *
 * ## Problem Breakdown
 * You're looking for two elements where `nums[i] + nums[j] = target`. The key insight is that for any number `x`, you need to find its **complement**: `target - x`.
 *
 * ## Core Concepts
 * ### 1. **Complement Thinking**
 * Instead of asking "which two numbers sum to target?", reframe it as: "For each number I see, have I already seen its complement?"
 * ```
 * target = 9, current number = 4
 * complement = 9 - 4 = 5
 * Question: Have I seen 5 before?
 * ```
 *
 * ### 2. **Hash Map for O(1) Lookup**
 * A hash map stores `{value → index}`, allowing instant lookup of whether a complement exists. This transforms the problem from O(n²) brute force to **O(n)** single-pass.
 *
 * ### 3. **Single-Pass Strategy**
 * As you iterate, you simultaneously:
 * - Check if the complement exists in the map
 * - Add the current number to the map for future lookups
 * This elegantly handles the "don't use the same element twice" constraint.
 *
 * ## Algorithm Walkthrough
 * ```
 * nums = [2, 7, 11, 15], target = 9
 * i=0: num=2, complement=7, map={} → 7 not found → add {2:0}
 * i=1: num=7, complement=2, map={2:0} → 2 FOUND at index 0 → return [0,1]
 * ```
 *
 * ## Edge cases
 * 1. Negative numbers:
 * nums = [-3, 4, 3, 90], target = 0
 * // -3 + 3 = 0 → return [0, 2]
 *
 * 2. Zeros in array
 * nums = [0, 4, 3, 0], target = 0
 * // 0 + 0 = 0 → return [0, 3]
 *
 * 3. Duplicate values
 * nums = [3, 3], target = 6
 * // 3 + 3 = 6 → return [0, 1]
 *
 * Works because we check for the complement **before** adding the current element to the map.
 * i=0: num=3, complement=3, map={} → not found → add {3:0}
 * i=1: num=3, complement=3, map={3:0} → FOUND → return [0,1]
 *
 * 4. Same element cannot be used twice
 * nums = [3, 2, 4], target = 6
 * // Cannot use index 0 twice (3+3)
 * // Must find 2+4 → return [1, 2]
 * The check-before-insert pattern inherently prevents this.
 *
 * ## Time & Space Complexity
 * | Approach | Time | Space |
 * |----------|------|-------|
 * | Brute Force (nested loops) | O(n²) | O(1) |
 * | Hash Map (single pass) | O(n) | O(n) |
 *
 * ## Why This Pattern Matters
 * This is a foundational pattern you'll see repeatedly in variations like:
 * - **Three Sum** / **Four Sum** (reduce to Two Sum)
 * - **Subarray Sum Equals K** (prefix sums + hash map)
 * - **Two Sum in BST** (two pointers or hash set)
 *
 * Common Interview Follow-ups
 * "What if array is sorted?"
 * Use two pointers (O(1) space)
*/
public class TwoSum {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer, Integer> seenValues = new HashMap<>();
        
        for(int i=0; i<nums.length; i++) {
            int complement = target - nums[i];
            if(seenValues.containsKey(complement)) {
                return new int[] {seenValues.get(complement), i};
            }
            
            seenValues.put(nums[i], i);
        }
        
        throw new IllegalArgumentException("No two sum solution found");
    }
    
    public static void main(String[] args) {
        TwoSum instance = new TwoSum();
        System.out.println(Arrays.toString(instance.twoSum(new int[] {2,7,11,15}, 9)));
    }
}
