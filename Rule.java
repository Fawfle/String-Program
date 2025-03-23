import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Scanner;

public class Rule {
    private final String line;

    private Scanner inputSc = new Scanner(System.in);

    private final RuleString pattern;
    private final RuleString replacement;

    public boolean ignore = false;

    public final boolean hasPrintFlags;

    //public boolean terminate = false; // end the program on execution

    private static final boolean DEBUG = false;

    public static final String ERROR_NAME = "Rule";
    public static final String RUN_ERROR = "(run)";
    public static final String FLAG_ERROR = "(flag)";
    public static final String LOAD_ERROR = "(load)";

    private String printString;

    public Rule(String _line) {
        line = _line.replaceAll(" ", "");
        if (stringArrayContains(Interpreter.PROCESS_RESERVED_CHARACTERS, line)) throw new RuleException(LOAD_ERROR, "Rule line contains reserved process character(s)");
        if (!line.contains("=")) throw new RuleException(LOAD_ERROR, "Rule does not contain '='");

        String[] l = line.split("=");

        pattern = new RuleString(l[0].length() > 0 ? l[0] : "", RSType.PATTERN);
        replacement = new RuleString(l.length > 1 ? l[1] : "", RSType.REPLACEMENT);

        // Rule flag errors and other flag stuff
        if (replacement.hasFlag(RSFlag.WILDCARD) && !pattern.hasFlag(RSFlag.WILDCARD)) throw new RuleException(FLAG_ERROR, "unmatched '*' in replacement");

        int patternWildcards = pattern.text.length() - pattern.text.replace(".", "").length();
        int replacementWildcards = replacement.text.length() - replacement.text.replace("!", "").length();
        if (replacementWildcards > patternWildcards) throw new RuleException(LOAD_ERROR, "replacement contains more wildcards than pattern");

        hasPrintFlags = replacement.hasPrintFlags;

        updatePrintString();
    }

    public String run(String input) {
        if (ignore) throw new RuleException(RUN_ERROR, "Rule should be ignored");
        String res = null; // return null means Rule didn't execute

        // simple replacement without flags, minor "optimization"
        if (((pattern.flags.size() == 1 && pattern.hasFlag(RSFlag.ONCE)) || pattern.flags.size() == 0) && replacement.flags.size() == 0) {
            res = RuleString.replaceFirst(input, pattern, replacement);
            if (res.equals(input)) return null; 
            if (pattern.hasFlag(RSFlag.ONCE)) ignore = true;
            return res;
        }

        if (pattern.hasFlag(RSFlag.INPUT)) {
            pattern.updateRunText(readUserInput());
        }

        int matchIndex = -1;

        if (pattern.hasFlag(RSFlag.END)) {
            int start = input.length() - pattern.getText().length();
            if (start < 0 || !RuleString.find(input, pattern, start)) return null;
            matchIndex = start;
        }
        else {
            matchIndex = RuleString.getFindIndex(input, pattern);
        }

        if (matchIndex == -1) return null;
        if (pattern.hasFlag(RSFlag.START) && matchIndex != 0) return null;

        // successful match
        if (pattern.hasFlag(RSFlag.ONCE)) ignore = true;
        
        if (replacement.hasDynamicFlags) {
            if (replacement.hasFlag(RSFlag.INPUT)) replacement.updateRunText(readUserInput());
            else replacement.updateRunText(replacement.text);
        }

        String matched = input.substring(matchIndex, matchIndex + pattern.getText().length());

        // update wildcard in replacement string
        if (replacement.hasFlag(RSFlag.WILDCARD)) {
            int startIndex = 0;
            String newText = "";
            char matchChar = (char)0;
            for (int i = 0; i < replacement.getText().length(); i++) {
                char c = replacement.getText().charAt(i);
                if (c == '!') { // update matchchar
                    int wmi = pattern.getText().indexOf(".", startIndex);
                    startIndex = wmi + 1;
                    matchChar = matched.charAt(wmi);
                }
                newText += (c == '!' || c == '@') ? matchChar : c;
            }
            replacement.updateRunText(newText);
        }

        if (replacement.hasFlag(RSFlag.RETURN)) return replacement.getText();
        if (hasPrintFlags) { // print returns res and replacement seperated by !
            res = RuleString.replaceFirst(input, pattern, "");
            return res + "!" + replacement.getText();
        }

        // calculate res
        if (replacement.hasPositionFlags) {
            res = RuleString.replaceFirst(input, pattern, "");
            res = replacement.hasFlag(RSFlag.START) ? replacement.getText() + res : res + replacement.getText();
        } else if (pattern.hasFlag(RSFlag.END)) { // replace last instead of replace first
            res = input.substring(0, input.length() - pattern.getText().length()) + replacement.getText();
        } else { // normal
            res = RuleString.replaceFirst(input, pattern, replacement);
        }

        return res;
    }

    public String readUserInput() {
        System.out.print("Enter input: ");
        String in = inputSc.nextLine();
        if (in.length() == 0) throw new RuleException(RUN_ERROR, "empty input");

        if (in.contains(" ") || stringArrayContains(Interpreter.SYNTAX_RESERVED_CHARACTERS, in) || stringArrayContains(Interpreter.PROCESS_RESERVED_CHARACTERS, in)) throw new RuleException(RUN_ERROR, "invalid input, contains reserved characters or spaces");
        System.out.println();
        return in;
    }

    public boolean hasPatternFlag(RSFlag f) {
        return pattern.hasFlag(f);
    }

    public boolean hasReplacementFlag(RSFlag f) {
        return replacement.hasFlag(f);
    }

    public static boolean stringArrayContains(String[] arr, String text) {
        for (String c : arr) {
            if (text.contains(c)) return true;
        }
        return false;
    }

    public String toString() {
        return printString;
    }

    private void updatePrintString() {
        if (DEBUG) printString = pattern.toString() + " = " + replacement.toString();
        else printString = line;
    }

    private class RuleException extends RuntimeException {
        public RuleException(String location, String description) {
            super(ERROR_NAME + " " + location + ": " + description + " \"" + line + "\"");
        }
    }
}