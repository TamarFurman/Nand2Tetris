import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
/**
 * Generates assembly code from the parsed VM command
 */
public class CodeWriter {
    private String fileName;
    private FileWriter fileWriter;
    private Integer counter = 0;
    private Map<String, String> segments;

    /**
     * C'tor
     * @param filePath - of the file the asm code will be written into,
     * set segments pointer that will be used.
     */
    public CodeWriter(String filePath) {
        this.fileName = filePath;
        segments = new HashMap<>(6);
        segments.put("local", "LCL");
        segments.put("argument", "ARG");
        segments.put("this", "THIS");
        segments.put("that", "THAT");
        segments.put("temp", "5");
        segments.put("pointer", "3");

        try {
            fileWriter = new FileWriter(filePath);

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private String unaryArithmetic(String operation) {
        return "@SP\r\nAM=M-1\r\nM=" + operation + "M\r\n@SP\r\nM=M+1\r\n";
    }

    private String binaryArithmetic1(String operation) {
        return "@SP\r\nAM=M-1\r\nD=M\r\n@SP\r\nAM=M-1\r\nM=M" + operation + "D\r\n@SP\r\nM=M+1\r\n";
    }

    private String binaryArithmetic2(String operation) {
        return "@SP\r\nAM=M-1\r\nD=M\r\n@SP\r\nAM=M-1\r\nM=D" + operation + "M\r\n@SP\r\nM=M+1\r\n";
    }

    private String compareArithmetic(String comp) {
        return "@SP\r\nAM=M-1\r\nD=M\r\n@SP\r\nAM=M-1\r\nD=M-D\r\n@IS_GT_OR_LT_" + counter + "\r\nD;"
                + comp + "\r\n@SP\r\nA=M\r\nM=0\r\n@FINISH_" + counter + "\r\n0;JMP\r\n(IS_GT_OR_LT_" + counter + ")\r\n@SP\r\n" +
                "A=M\r\nM=-1\r\n(FINISH_" + counter++ + ")\r\n@SP\r\nM=M+1\r\n";
    }

    public void writeArithmetic(String command) {
        String commandString;
        switch (command) {
            case "sub":
                commandString = binaryArithmetic1("-");
                break;
            case "add":
                commandString = binaryArithmetic1("+");
                break;
            case "neg":
                commandString = unaryArithmetic("-");
                break;
            case "eq":
                commandString = compareArithmetic("JEQ");
                break;
            case "gt":
                commandString = compareArithmetic("JGT");
                break;
            case "lt":
                commandString = compareArithmetic("JLT");
                break;
            case "and":
                commandString = binaryArithmetic2("&");
                break;
            case "or":
                commandString = binaryArithmetic2("|");
                break;
            case "not":
                commandString = unaryArithmetic("!");
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + command);
        }
        try {
            fileWriter.write(commandString);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * assembly code to be written into the file for push command
     * @param segment - the segment where the value will taken from to push
     * @param index - the index of the segment above
     * @return - the assembly code
     */
    private String push(String segment, String index) {
        String commandString = "@" + index + "\r\nD=A";
        if (segment.equals("static")) commandString = "@" + fileName.replace("/", ".").replace("asm",index) + "\r\nD=M";
        if (!segment.equals("constant") && !segment.equals("static")) {
            commandString += "\r\n@" + segments.get(segment) + "\r\nA=D+";
            commandString += (segment.equals("temp") || segment.equals("pointer")) ? "A\r\nD=M" : "M\r\nD=M";
        }
        commandString += "\r\n@SP\r\nA=M\r\nM=D\r\n@SP\r\nM=M+1\r\n";

        return commandString;
    }
    /**
     * assembly code to be written into the file for pop command
     * @param segment - the segment where the value will be put from the pop
     * @param index - the index of the segment above
     * @return - the assembly code
     */
    private String pop(String segment, String index) {
        String commandString = "@" + index + "\r\nD=A\r\n@" + segments.get(segment) + "\r\nD=D+";
        commandString += (segment.equals("local") || segment.equals("argument") ||
                segment.equals("this") || segment.equals("that")) ? "M" : "A";
        commandString += "\r\n@R13\r\nM=D\r\n@SP\r\nAM=M-1\r\nD=M\r\n@R13\r\nA=M\r\nM=D\r\n";
        commandString = !segment.equals("static") ? commandString :
                "@SP\r\nAM=M-1\r\nD=M\r\n@" + fileName.replace("/", ".").replace("asm",index) + "\r\nM=D\r\n";
        return commandString;
    }

    public void writePushPop(String command, String segment, String index) {
        String commandString;
        if (command.equals("C_PUSH"))
            commandString = push(segment, index);
        else {
            commandString = pop(segment, index);
        }
        try {
            fileWriter.write(commandString);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * closes the file - before closing it adds an infinite loop to the code in the file
     */
    public void close() {
        String endLoop = "(END)\r\n@END\r\n0;JMP\r\n";
        try {
            fileWriter.write(endLoop);
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
