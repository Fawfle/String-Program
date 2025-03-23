TODO:
add (print), only in replacement.
- think it will print whatevers in res, but idk.
- possibly add to stack?
- need way to "hide" information

Program is a Markov Algorithm
Lots of inspiration from the puzzle game A=B

---- Language Overview ----

Program is made up of a sequence of rules such as "a=b"
rules all process and modify string
rules are made of expressions
the two types of expressions are Patterns and Replacements.
if a Pattern is detected in the process string, it is replaced with the Replacement.

rules are written in the form:
    
    pattern = replacement

For example, the rule

    a = b

replaces every 'a' in a string with a 'b'.

---- Execution ----

Starts at line 0, evaluating each rule
if an rule executes because a Pattern is found, go back to line 0
otherwise, move onto the next rule
When the final line of the program is reached, the program ends.

---- rules ----

"=" - Assignment
replaces first instance of Pattern with Replacement

---- Flags ----

(start)/(end) - start/end
Pattern: check if pattern is at start/end
Replacement: put replacement at start/end

(return)
Replacement only: return replacement

(once)
Pattern only: only execute a rule once

(input) - accepts string
Dynamically read input, will read every execution.
input expression must otherwise be empty
Pattern: pattern is set to input
Replacement: replacement is set to input

(print) - prints string to console
Replacement only: print will output replacement to console.
Pattern will be removed from string, but replacement will
NOT be added.

(println) - print new line
Identical to print but adds a newline character at the end

(printstr) - print string
Identical to print but prints the current string followed by a newline

---- Other Syntax ----

Flags - "(flag)..."
Flags are entered before a Pattern or Replacement between parenthesis
Multiple flags can be seperated by commas "(f1, f2, f3)"

"*" - Wildcard
Matches with any character
When used in replacement, the first "*" corresponds to the first
character "*" matched with in the pattern, the second "*" with
the second, and so on. # of Wildcards in Replacement cannot exceed
# in Patetrn.

EX: [*=] - removes 1st letter of string [ab->b]

"@" - Previous Wildcard
matches as if it was the previous wildcard.

EX: [*@=] - removes adjacent pairs. [aab->b]

"#" - Comment
Begin line with # to make it a comment

Reserved Characters (syntax or used internally):
"=", "*", "@", "#", "(" and ")", "<", ">" "!"

Whitespace is ignored