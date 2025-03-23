import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.ArrayList;

public class Main
{
    public static final String PROGRAM = "Hangman.txt";

    public static void main(String[] args) throws FileNotFoundException
    {
        Scanner input = new Scanner(System.in);
        ArrayList<String> programText = readFile(new File("./Programs/" + PROGRAM));
        ArrayList<String> words = readFile(new File("./Words.txt"));

        Interpreter interpreter = new Interpreter(programText);

        String prompt = "";
        if (PROGRAM.equals("Hangman.txt")) prompt = words.get((int)Math.floor(Math.random() * words.size())).toLowerCase();

        String text = interpreter.runProgram(prompt);

        System.out.println("\nResult:" + text);
    }

    public static ArrayList<String> readFile(File file) throws FileNotFoundException {
        Scanner sc = new Scanner(file);
        sc.useDelimiter("\n");
        ArrayList<String> res = new ArrayList<String>();
        while (sc.hasNextLine()) res.add(sc.nextLine());

        return res;
    }
}