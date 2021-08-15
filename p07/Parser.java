import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * Handles the parsing of a single .vm file
 * Reads a VM command, parses the command into its lexical components,
 * and provides convenient access to these components
 */
public class Parser {

    private Scanner scanner;
    String[] splitCommand;
    private final String C_PUSH = "C_PUSH";
    private final String C_POP = "C_POP";
    private final String C_ARITHMETIC = "C_ARITHMETIC";
    private final String C_RETURN = "C_RETURN";

    /**
     * C,tor - initializes the file to be read
     * @param filepath - of the file to be read(must be FULL path)
     */
    public Parser(String filepath) {
        try {
            scanner = new Scanner( new File(filepath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    /**
     * checks if there are more commands in the file being read.
     * @return ture if has more commands, else false
     */
    public boolean hasMoreCommands() {
        return scanner.hasNextLine();
    }

    /**
     * advances the scanner in the file to the next command,
     * if its an empty line it continues to the next,and if there is a comment in the line it gets erased.
     */
    public void advance() {
        if (hasMoreCommands()) {
            String line = scanner.nextLine();
            Integer index = line.indexOf("//");
            if (line.isEmpty() || index == 0) {
                advance();
            } else if (index != -1) {
                line = line.substring(0, index);
                splitCommand = line.split(" ");
            } else splitCommand = line.split(" ");
        }
        return;
    }
    
    /**
     * @return the command type of the current command.
     */
    public String commandType() {
        switch (splitCommand[0]) {
            case "push":
                return C_PUSH;
            case "pop":
                return C_POP;
            case "sub":
            case "add":
            case "neg":
            case "eq":
            case "gt":
            case "lt":
            case "and":
            case "or":
            case "not":
                return C_ARITHMETIC;
            default:
                throw new IllegalStateException("Unexpected value: " + splitCommand[0]);
        }
    }

    public String arg1() {
        if (commandType() != C_RETURN) {
            if (commandType().equals(C_ARITHMETIC)) return splitCommand[0];
            return splitCommand[1];
        }
        return null;
    }

    public String arg2() {
        if (commandType().equals(C_PUSH) || commandType().equals(C_POP)) {
            return splitCommand[2];
        }
        return null;
    }

}