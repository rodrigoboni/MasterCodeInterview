# MasterCodeInterview
Big O Notation + Data Structures + Algorithms exercises with Java

(All content here refers to the course **"Master the Coding Interview: Data Structures + Algorithms"** by Andrei Neagoie - https://www.udemy.com/share/1013ja3@Ahg-_VoZoRNnhroJCNpiaHHTGeM2xkzu7Zh7lqng98FGhNabwGjP9nefcmM4azc=/)

(The following content is a mix of course resume and some copy and paste of checklists, guides, step-by-step instructions etc - I wrote it as a way to fix better new knowledge and have a reference to read when needed)

## Big O complexity chart
![bigocomplexitychart.jpg](bigocomplexitychart.jpg)

## Course Mind maps
- https://coggle.it/diagram/W5E5tqYlrXvFJPsq/t/master-the-interview-click-here-for-course-link
- https://coggle.it/diagram/W5u8QkZs6r4sZM3J/t/master-the-interview

## Big O Cheatsheets
- https://zerotomastery.io/cheatsheets/big-o-cheat-sheet/
- https://zerotomastery.io/cheatsheets/data-structures-and-algorithms-cheat-sheet/
- https://www.bigocheatsheet.com/

## Big O's (For time and space complexity)
- **O(1)** Constant- no loops
- **O(log N)** Logarithmic - usually searching algorithms have log n if they are sorted (Binary Search)
- **O(n)** Linear - for loops, while loops through n items
- **O(n log(n))** Log Linear - usually sorting operations
- **O(n^2)** Quadratic - every element in a collection needs to be compared to ever other element. Two nested loops
- **O(2^n)** Exponential - recursive algorithms that solves a problem of size N
- **O(n!)** Factorial - you are adding a loop for every element - it's not usual to find or apply this complexity, because it's so expensive

## Big O rules
- Worst case
  - Consider only the worst case to define the complexity of an algorithm 
    - ex: an algorithm with **O(1)** and **O(n)** operations will be **O(n)**
- Remove constants
  - Don't consider any complexity defined with a constant
    - ex: an algorithm with **O(1 + n/2 + 100)** will be **O(n)**
    - The main idea is to evaluate how the number of operations scale with the number of elements, instead of care about the number of operations to be performed
- Different terms for inputs
  - Pay attention on which terms / elements a loop or any kind of operation applies to, in order to not assign a wrong complexity notation of **O(n)**
  - Two separate collections (two separated inputs) have different variables in big O
    - When executed in order (same level): O(a + b)
    - When executed nested: O(a * b), which results in **O(n^2)** - quadratic time
- Drop non dominants
  - Consider only the most significant part
  - ex: an algorithm with **O(n^2 + 3n + 100 + n/2)** will be **O(n^2)**, because the quadratic function will always scale more than other parts
  - The best algorithm is which scale elements with the minimum increment of operations
  - Iterating through half a collection **O(n/2)** is still **O(n)**

## What can cause time in a function ?
- Operations (+, -, *, /)
- Comparisons (<, >, ==)
- Looping (for, while)
- Outside Function call (function())

## What causes Space complexity ?
- Variables
- Data Structures
- Function Call
- Allocations

## What's a good code ?
- Readable
  - Clean code
  - Maintainable
- Scalable
  - Speed: 
    - Time complexity (Big O)
    - Efficient (fewer operations as inputs grows, resulting in shorter time to run)
  - Memory
    - Space complexity (Big O)
    - Efficient (lower memory usage to complete the algorithm)

## Trade-off for good code:
- Fast algorithms usually require more memory
- Low memory algorithms usually require more time

## What skills interviewer is looking for:
- Analytic skills - How can you think through problems and analyze things ?
- Coding skills - Do you code well, by writing clean, simple, organized, readable code ?
- Technical knowledge - Do you know the fundamentals of the job you're applying for ?
- Communications skills - Does your personality match the companie's culture ?

## Step by step through a problem:
1. When the interviewer says the question, write down the key points at the top. 
   - Make sure you have all the details. 
   - Show how organized you are
2. Make sure you double check
   - What are the inputs ? 
   - What are the outputs ?
3. What is the most important value of the problem ? 
   - Do you have time, space, memory etc ? 
   - What is the main goal ?
4. Don't be annoying and ask too many questions to the interviewer
5. Start with the naive/bute force approach. 
   - First thing that comes into mind. 
   - It shows that you're able to think well and critically (you don't need to write this code, just speak about it).
6. Tell them why this approach is not the best (i.e. O(n^2)) or higher, not readable etc...
7. Walk through your approach, comment things and see where you may be able to break things. 
   - Any repetition, bottlenecks like O(n^2), or unnecessary work ? 
   - Did you use all the information the interviewer gave you? 
   - Bottleneck is the part of the code with biggest Big O. Focus on that. Sometimes this occurs with repeated work as well.
8. Before you start coding, walk through your code and write down the steps you are going to follow
9. Modularize your code from the very beginning. 
   - Break up your code into beautiful small pieces and add just comments if you need to.
10. Start actually writing your code now. 
    - Keep in mind that the more you prepare and understand what you need to code, the better the whiteboard will go.
    - So never start a whiteboard interview not being sure of how things are going to work out. That is a recipe for disaster.
    - Keep in mind - A lot of interviews ask questions that you won't be able to fully answer on time.
    - So think: what can I show in order to show that I can do this and I am better the other coders.
    - Break things up in functions, methods etc (if you can't remember a method, just make up a function and you will at least have it there. Write something, and start with the easy part)
11. Think about error checks and how you can break this code.
    - Never make assumptions about the input 
    - Assume people are trying to break your code and that Darth Vader is using your function.
    - How will you safeguard it ?
    - Always check for false inputs that yo don't want.
    - Here is a trick: 
      - comment in the code, the checks that you want to do... 
      - write the function, 
      - then tell the interviewer that you would write tests now to make your function fail (but you won't need to actually write the tests)
12. Don't use bad/confusing names like i and j for variables, instances etc - write code that reads well
13. Test your code: Check for no params, 0 undefined, null, massive arrays, async code etc... 
    - Ask the interviewer if we can make assumption about the code
    - Can you make the answer return an error ?
    - Poke holes into your solution. Are you repeating yourself ?
14. Finally talk to the interviewer where you would improve the code. 
    - Does it work ?
    - Are there different approaches ?
    - Is it readable ?
    - What would you google to improve ?
    - How can performance be improved ?
    - Possibly: Ask the interviewer what was the most interesting solution you have seen to this problem
15. If your interviewer is happy with the solution, the interview usually ends here.
    - It is also common that the interviewer asks you extension questions, such as how you would handle the problem if the whole input is too large to fit into memory
    - or if the input arrives as a stream
    - This is a common follow-up question at Google, where they care a lot about scale.
    - The answer is usually a divide-and-conquer approach - perfom distributed processing of the data and only read certain chunks of the input from disk into memory, write the ouput back to disk and combine them later.

## Good code checklist:
- [✅]It works
- [✅]Good use of data structures
- [✅]Code Re-use/ Do Not Repeat Yourself
- [✅]Modular - makes code more readable, maintainable and testable
- [✅]Less than O(N^2). We want to avoid nested loops if we can since they are expensive. Two separate loops are better than 2 nested loops
- [✅]Low Space Complexity --> Recursion can cause stack overflow, copying of large arrays may exceed memory of machine

## Heurestics to ace the question:
- [✅]Hash Maps are usually the answer to improve Time Complexity
- [✅]If it's a sorted array, use Binary tree to achieve O(log N). Divide and Conquer - Divide a data set into smaller chunks and then repeating a process with a subset of data. Binary search is a great
example of this
- [✅]Try Sorting your input
- [✅]Hash tables and precomputed information (i.e. sorted) are some of the best ways to optimize your code
- [✅]Look at the Time vs Space tradeoff. Sometimes storing extra state in memory can help the time. (Runtime)
- [✅]If the interviewer is giving you advice/tips/hints. Follow them
- [✅]Space time tradeoffs: Hastables usually solve this a lot of the times. You use more space, but you can get a time optimization to the process. In programming, you often times can use up a little bit more space to get faster time
