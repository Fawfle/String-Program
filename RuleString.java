import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Scanner;

public class RuleString {
    public final RSType type;
    public final String plainText; // full input text
    public final String text; // text without flags and with internal characters set
    // runtime variables (modifyable)
    private Pattern pattern; // regex with formatted text as pattern (special characters escaped) for dynamic instructions, pattern.pattern will change
    private String runText;

    public final ArrayList<RSFlag> flags = new ArrayList<RSFlag>();
    // variables for flag categories
    public final boolean hasPositionFlags; // start and end
    public final boolean hasDynamicFlags; // wildcard, input
    public final boolean hasPrintFlags; // print, println

    public final boolean isEmpty;

    private String printString;

    public static final boolean DEBUG = false;

    public static final String ERROR_NAME = "RuleString";
    public static final String LOAD_ERROR = "(load)";
    public static final String RUN_ERROR = "(run)";
    public static final String FLAG_ERROR = "(flag)";

    public RuleString(String _text, RSType _type) {
        type = _type;
        plainText = _text;

        // temp text before final assignment
        String tempText = plainText.substring(plainText.indexOf(")") + 1);
        isEmpty = tempText.length() == 0;

        addExplicitFlags(); // flags between '()'s

        if (tempText.contains("*")) {
            flags.add(RSFlag.WILDCARD); // implicit
            if (isType(RSType.PATTERN)) tempText = tempText.replaceAll("\\*", "."); // convert to regex wildcard
            else if (isType(RSType.REPLACEMENT)) tempText = tempText.replaceAll("\\*", "!"); // convert to custom internal character
        }

        if (tempText.contains("@") && (!hasFlag(RSFlag.WILDCARD) || plainText.indexOf("@") < plainText.indexOf("*"))) throw new RuleStringException(LOAD_ERROR, "'@' does not match with previous '*'");

        text = tempText;

        // assign/update flag categories
        hasPositionFlags = hasFlag(RSFlag.START) || hasFlag(RSFlag.END);
        hasDynamicFlags = hasFlag(RSFlag.WILDCARD) || hasFlag(RSFlag.INPUT);
        hasPrintFlags = hasFlag(RSFlag.PRINT) || hasFlag(RSFlag.PRINTLN) || hasFlag(RSFlag.PRINTSTR);

        handleFlagExceptions();

        if (!hasFlag(RSFlag.INPUT)) {
            runText = text;
            if (isType(RSType.PATTERN)) pattern = textToPattern(text);
        }
        
        updatePrintString();        
    }

    // update internals for dynamic instructions. Input Pattern or any Dynamic Replacement.
    public void updateRunText(String t) {
        if (!hasDynamicFlags) throw new RuleStringException(RUN_ERROR, "attempted to update non dynamic instruction");
        if (!hasFlag(RSFlag.INPUT) && isType(RSType.PATTERN)) throw new RuleStringException(RUN_ERROR, "attempted to update pattern without 'input' flag.");

        runText = t;
        if (isType(RSType.PATTERN)) pattern = textToPattern(t);
    }

    private void addExplicitFlags() {
        if (plainText.equals("")) return;
        String[] flagStrings = parseExplicitFlags();
        if (flagStrings.length <= 0) return;
        for (String s : flagStrings) {
            switch(s) {
                case "start":
                flags.add(RSFlag.START);
                break;
                case "end":
                flags.add(RSFlag.END);
                break;
                case "once":
                flags.add(RSFlag.ONCE);
                break;
                case "input":
                flags.add(RSFlag.INPUT);
                break;
                case "return":
                flags.add(RSFlag.RETURN);
                break;
                case "print":
                flags.add(RSFlag.PRINT);
                break;
                case "println":
                flags.add(RSFlag.PRINTLN);
                break;
                case "printstr":
                flags.add(RSFlag.PRINTSTR);
                break;
                default:
                throw new RuleStringException(FLAG_ERROR, "invalid Pattern flag " + "\"" + s + "\"");
            }
        }
    }

    public void handleFlagExceptions() {
        if (hasFlag(RSFlag.START) && hasFlag(RSFlag.END)) throw new RuleStringException(FLAG_ERROR, "Flag conflict (start && end)");
        if (hasFlag(RSFlag.INPUT) && !isEmpty) throw new RuleStringException(FLAG_ERROR, "RuleString with INPUT must be empty");
        if (hasFlag(RSFlag.RETURN) && hasPositionFlags) throw new RuleStringException(FLAG_ERROR, "return is incompatible with position flags (start || end)");
        if (hasFlag(RSFlag.RETURN) && type == RSType.PATTERN) throw new RuleStringException(FLAG_ERROR, "PATTERN cannot contain flag 'return'");
        if (hasFlag(RSFlag.ONCE) && type == RSType.REPLACEMENT) throw new RuleStringException(FLAG_ERROR, "REPLACEMENT cannot contain flag 'once'");
        if (hasPrintFlags && type == RSType.PATTERN) throw new RuleStringException(FLAG_ERROR, "PATTERN cannot contain print flags (print || println || printstr)");
        if (hasFlag(RSFlag.RETURN) && hasPrintFlags) throw new RuleStringException(FLAG_ERROR, "Flag conflict (return && print)");
        if (hasFlag(RSFlag.PRINTSTR) && !isEmpty) throw new RuleStringException(FLAG_ERROR, "RuleString with PRINTSTR must be empty");
        // no longer pretty :(
        int printFlags = 0;
        if (hasFlag(RSFlag.PRINT)) printFlags++;
        if (hasFlag(RSFlag.PRINTLN)) printFlags++;
        if (hasFlag(RSFlag.PRINTSTR)) printFlags++;
        if (printFlags > 1) throw new RuleStringException(FLAG_ERROR, "RuleString cannot contain more than 1 print flag (print || println || printstr)");
    }

    public String[] parseExplicitFlags() {
        // check for double '(' or ')'
        if (plainText.length() - plainText.replace(")", "").length() > 1) throw new RuleStringException(FLAG_ERROR, "duplicate ')'");
        if (plainText.length() - plainText.replace("(", "").length() > 1) throw new RuleStringException(FLAG_ERROR, "duplicate '('");

        int flagTextEndIndex = plainText.indexOf(")");
        if (flagTextEndIndex == -1) {
            if (plainText.charAt(0) == '(') throw new RuleStringException(FLAG_ERROR, "expected ')'");
            return new String[0]; // no flags found
        }

        if (plainText.charAt(0) != '(') throw new RuleStringException(FLAG_ERROR, "expected '(");
        if (flagTextEndIndex == 1) throw new RuleStringException(FLAG_ERROR, "empty '()'");
        
        String[] flagStrings = plainText.substring(1, flagTextEndIndex).split(",");
        if (flagStrings.length == 0) throw new RuleStringException(FLAG_ERROR, "null flags");
        return flagStrings;
    }

    public Pattern textToPattern(String t) {
        for (String s : Interpreter.REGEX_RESERVED_CHARACTERS) {
            t = t.replace(s, "\\" + s);
        }
        t = t.replace(".", "(.)"); // parenthesis make a capturing group
        while(t.contains("@")) {
            int index = t.indexOf("@");
            int group = countChar(t.substring(0, index), ('.')); // get # of previous group
            t = t.substring(0, index).concat("\\" + group).concat(t.substring(index + 1)); 
        }
        return Pattern.compile(t);
    }

    public boolean hasFlag(RSFlag f) {
        return flags.contains(f);
    }

    public boolean isType(RSType t) {
        return type == t;
    }

    public String getText() {
        return runText;
    }

    public Pattern getPattern() {
        if (!isType(RSType.PATTERN)) throw new RuleStringException(RUN_ERROR, "attempted to get pattern of RuleString without type PATTERN");
        return pattern;
    }

    public String toString() {
        return printString;
    }

    private void updatePrintString() {
        if (DEBUG) printString = text + ", " + flags;
        else printString = plainText;
    }

    public static String replaceFirst(String s, RuleString l, RuleString r) {
        return replaceFirst(s, l, r.getText());
    }

    public static String replaceFirst(String s, RuleString e, String r) {
        return e.getPattern().matcher(s).replaceFirst(r);
    }

    public static boolean find(String s, RuleString e) {
        return e.getPattern().matcher(s).find();
    }

    public static boolean find(String s, RuleString e, int startIndex) {
        return e.getPattern().matcher(s).find(startIndex);
    }

    public static int getFindIndex(String s, RuleString e) {
        Matcher m = e.getPattern().matcher(s);

        if (!m.find()) return -1;
        return m.start();
    }

    private class RuleStringException extends RuntimeException {
        public RuleStringException(String location, String description) {
            super(ERROR_NAME + " " + location + ": " + description + " \"" + plainText + "\"");
        }
    }

    public int countChar(String str, char c)
    {
        int count = 0;
        
        for(int i = 0; i < str.length(); i++)
        {    
            if (str.charAt(i) == c) count++;
        }
        
        return count;
    }
}

// list of all flags
enum RSFlag {
    WILDCARD, // string contains wildcard
    RETURN, // replacement: return replacement
    ONCE, // only run once
    START, // only check at start/add to start
    END, // only check at end/add to end
    INPUT, // input for pattern or replacement (pattern/replacement = input)
    PRINT, // replacement: output replacement to console
    PRINTLN, // replacement: print with appended newline
    PRINTSTR, // replacement: print string
}

// allows RuleStrings to know their type for formatting and error handling
enum RSType {
    PATTERN,
    REPLACEMENT
}