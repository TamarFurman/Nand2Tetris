import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Parser {

    private Scanner scanner;
    private String[] currentCommand;
    private final String C_PUSH = "C_PUSH";
    private final String C_POP = "C_POP";
    private final String C_ARITHMETIC = "C_ARITHMETIC";
    private final String C_GOTO = "C_GOTO";
    private final String C_LABEL = "C_LABEL";
    private final String C_IF = "C_IF";
    private final String C_RETURN = "C_RETURN";
    private final String C_FUNCTION = "C_FUNCTION";
    private final String C_CALL = "C_CALL";

    public Parser(String filePath) {
        File fileToParse = new File(filePath);
        try {
            this.scanner = new Scanner(fileToParse);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public boolean hasMoreCommands() {
        if (scanner.hasNextLine()) {
            return true;
        }
        scanner.close();
        return false;
    }

    public void advance() {
        if (hasMoreCommands()) {
            String nextLine = scanner.nextLine();
            int indexToSlice = nextLine.indexOf("//");
            if (indexToSlice != -1) {
                if (nextLine.substring(0, indexToSlice).isEmpty() || nextLine.isEmpty()) {
                    advance();
                } else {
                    currentCommand = nextLine.substring(0, indexToSlice).split(" ");
                }
            } else if (nextLine.isEmpty()) {
                advance();
            } else {
                currentCommand = nextLine.split(" ");
            }
        }
    }

    public String commandType() {
        switch (currentCommand[0]) {
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
            case "if-goto":
                return C_IF;
            case "goto":
                return C_GOTO;
            case "label":
                return C_LABEL;
            case "function":
                return C_FUNCTION;
            case "call":
                return C_CALL;
            case "return":
                return C_RETURN;
            default:
                throw new IllegalStateException("Unexpected value: " + currentCommand[0]);
        }
    }

    public String arg1() {
        if (commandType() != C_RETURN) {
            if (commandType() == C_ARITHMETIC) {
                return currentCommand[0];
            } else {
                return currentCommand[1];
            }
        }
        return null;
    }

    public String arg2() {
        String commandType = commandType();
        if (commandType.equals(C_POP) || commandType.equals(C_PUSH) || commandType.equals(C_CALL) || commandType.equals(C_FUNCTION))
            return currentCommand[2];
        return null;
    }

}