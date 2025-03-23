import java.util.ArrayList;

public class Interpreter {
    private ArrayList<String> programText;
    private ArrayList<Rule> rules = new ArrayList<Rule>();

    private int ruleIndex = 0;

    private static final boolean DEBUG = true;

    private int executionCounter = 0;

    // execution limits to avoid infinite loops for debugging
    public static final int DEBUG_EXECUTION_COUNTER_LIMIT = 5999;
    public static final int DEBUG_STRING_LENGTH_LIMIT = 200;

    public static final String[] SYNTAX_RESERVED_CHARACTERS = "*()@".split(""); // programming syntax
    public static final String[] PROCESS_RESERVED_CHARACTERS = ".!".split(""); // characters used for processing
    public static final String[] REGEX_RESERVED_CHARACTERS = "^$+?[]{}|".split(""); // characters used by regular expressions

    public static final String ERROR_NAME = "Interpreter";
    public static final String LOAD_ERROR = "(load)";
    public static final String RUN_ERROR = "(run)";
    public static final String DEBUG_ERROR = "(debug)";


    public Interpreter(ArrayList<String> _programText) {
        if (DEBUG) System.out.print("Loading program... ");

        programText = _programText;
        for (String line : programText) {
            if (line.equals("") || line.charAt(0) == '#') continue;
            if (line.contains("#")) throw new InterpreterException(LOAD_ERROR, "line contains reserved '#'", line);
            rules.add(new Rule(line));
        }

        //System.out.println("Program: " + this.programText);
        if (DEBUG) {
            System.out.println("Program loaded!");
            System.out.println("Rules: " + rules);
        }
    }

    public String runProgram(String input) {
        System.out.println("Running Program...\n");
        // reset program
        ruleIndex = 0;
        for (Rule i : rules) i.ignore = false;
        if (DEBUG) System.out.println(input);
        
        String string = input;

        while (ruleIndex < rules.size()) {
            Rule rule = rules.get(ruleIndex);
            String res = rule.ignore ? null : rule.run(string);
            // res is null if no replacement occured or skipped
            if (res != null) {
                executionCounter++;
                if (DEBUG && executionCounter > DEBUG_EXECUTION_COUNTER_LIMIT) throw new InterpreterException(DEBUG_ERROR, "execution counter exceeded");
                if (DEBUG && res.length() > DEBUG_STRING_LENGTH_LIMIT) throw new InterpreterException(DEBUG_ERROR, "string length exceeded");

                if (rule.hasReplacementFlag(RSFlag.RETURN)) {
                    if (DEBUG) System.out.printf(res + "   ([%s])%n", rule);
                    return res;
                }

                ruleIndex = 0;
                if (rule.hasPrintFlags) {
                    // print instructions return a string of string!result with ! as the delimiter.
                    String[] s = res.split("!");
                    res = s[0];
                    String replacement = s.length <= 1 ? "" : s[1];
                    String print = rule.hasReplacementFlag(RSFlag.PRINTSTR) ? res : replacement;
                    if (rule.hasReplacementFlag(RSFlag.PRINTLN) || rule.hasReplacementFlag(RSFlag.PRINTSTR)) print += "\n";
                    if (DEBUG) print = "\nPRINT: " + print.replace("\n", "\\n") + "\n";
                    System.out.print(print);
                }

                string = res;
                if (DEBUG) System.out.print(string + "   ([" + rule + "])\n");
            }
            else ruleIndex++;
        }

        return string;
    }

    private class InterpreterException extends RuntimeException {
        public InterpreterException(String location, String description) {
            super(ERROR_NAME + " " + location + ": " + description);
        }
        public InterpreterException(String location, String description, String line) {
            super(ERROR_NAME + " " + location + ": " + description + " \"" + line + "\"");
        }
    }
}