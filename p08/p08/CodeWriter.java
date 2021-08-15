import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates assembly code from the parsed VM command
 */
public class CodeWriter {
    private String fileName;
    private String funcName;
    private FileWriter fileWriter;
    private Integer counter = 0;
    private Map<String, String> segments;

    /**
     * C'tor
     * @param filePath - of the file the asm code will be written into
     */
    public CodeWriter(String filePath) {
        this.funcName = null;
        segments = new HashMap<>(6);
        segments.put("local", "LCL");
        segments.put("argument", "ARG");
        segments.put("this", "THIS");
        segments.put("that", "THAT");
        segments.put("temp", "5");
        segments.put("pointer", "3");

        setFileName(filePath);
        try {
            fileWriter = new FileWriter(filePath);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * Writes the commandString to the output file.
     * @param commandString
     */
    private void writeTofile(String commandString){
        try {
            fileWriter.write(commandString);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Informs the codeWriter that the translation of a new file has been started.
     * @param fileName
     */
    public void setFileName(String fileName) {
        String tempFilePath=fileName.replace("\\","/");
        String[] fileNameTemp = tempFilePath.split("/");
        fileNameTemp[fileNameTemp.length - 1] = fileNameTemp[fileNameTemp.length - 1].replace(" ", "_");
        this.fileName=fileNameTemp[fileNameTemp.length - 1];
    }

    private String unaryArithmetic(String operation) {
        return "@SP\r\nA=M-1\r\nM=" + operation + "M\r\n";
    }

    private String binaryArithmetic1(String operation) {
        return "@SP\r\nAM=M-1\r\nD=M\r\n@SP\r\nA=M-1\r\nM=M" + operation + "D\r\n";
    }

    private String binaryArithmetic2(String operation) {
        return "@SP\r\nAM=M-1\r\nD=M\r\n@SP\r\nA=M-1\r\nM=D" + operation + "M\r\n";
    }

    private String compareArithmetic(String comp) {
        return "@SP\r\nAM=M-1\r\nD=M\r\n@SP\r\nAM=M-1\r\nD=M-D\r\n@IS_GT_OR_LT_" + counter + "\r\nD;"
                + comp + "\r\n@SP\r\nA=M\r\nM=0\r\n@FINISH_" + counter + "\r\n0;JMP\r\n(IS_GT_OR_LT_" + counter + ")\r\n@SP\r\n" +
                "A=M\r\nM=-1\r\n(FINISH_" + counter++ + ")\r\n@SP\r\nM=M+1\r\n";
    }

    public void WriteArithmetic(String command) {
        String commandString;
        switch (command) {
            case "sub":
                commandString = binaryArithmetic1("-");
                break;
            case "add":
                commandString = binaryArithmetic2("+");
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
        writeTofile(commandString);
    }

    /**
     * assembly code to be written into the file for push
     * @param segment - the segment where the value will taken from to push
     * @param index - the index of the segment above
     * @return - the assembly code
     */
    private String push(String segment, String index) {
        String commandString = "@" + index + "\r\nD=A";
        if (segment.equals("static"))
            commandString = "@" + fileName.replace("vm", index) + "\r\nD=M";
        if (!segment.equals("constant") && !segment.equals("static")) {
            commandString += "\r\n@" + segments.get(segment) + "\r\nA=D+";
            commandString += (segment.equals("temp") || segment.equals("pointer")) ? "A\r\nD=M" : "M\r\nD=M";
        }
        commandString += "\r\n@SP\r\nA=M\r\nM=D\r\n@SP\r\nM=M+1\r\n";
        return commandString;
    }

    /**
     * assembly code to be written into the file for pop
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
                "@SP\r\nAM=M-1\r\nD=M\r\n@" + fileName.replace("vm", index) + "\r\nM=D\r\n";
        return commandString;
    }

    public void WritePushPop(String command, String segment, String index) {
        String commandString;
        if (command.equals("C_PUSH"))
            commandString = push(segment, index);
        else {
            commandString = pop(segment, index);
        }
        writeTofile(commandString);
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

    /**
     * Writes assembly code that affects the label command.
     * @param label - the label ot be written in the outPut file.
     */
    public void writeLabel(String label) {
        String func = funcName != null ? fileName.replace("vm", label) + "." + funcName + "$" : "";
        writeTofile("(" + (func + label).toUpperCase() + ")\n");
    }

    /**
     * Write the assembly code that effects the bootstrap code that initializes the vm.
     */
    public void writeInit() {
        String commandString = "@256\r\nD=A\r\n@SP\r\nM=D\r\n";
        writeTofile(commandString);
        writeCall("Sys.init", "0");
    }

    /**
     * Writes assembly code that affects the goto command.
     * @param label - the label to goto.
     */
    public void writeGoto(String label) {
        String func = funcName != null ? fileName.replace("vm", label) + "." + funcName + "$" : "";
        String commandString = "@" + (func + label).toUpperCase() + "\r\n0;JMP\r\n";
        writeTofile(commandString);
    }

    /**
     * Writes assembly code that affects the if-goto command.
     * @param label - the label to goto.
     */
    public void writeIf(String label) {
        String func = funcName != null ? fileName.replace("vm", label) + "." + funcName + "$" : "";
        String commandString = "@SP\r\nAM=M-1\r\nD=M\r\n@" + (func + label).toUpperCase() + "\r\nD;JNE\r\n";
        writeTofile(commandString);
    }

    /**
     * Writes assembly code that affects the function command.
     * @param functionName - the name of th function.
     * @param numVars - of the function
     */
    public void writeFunction(String functionName, String numVars) {
        writeLabel(functionName);
        String commandString = "";
        int vars = Integer.parseInt(numVars);
        for (int i = 0; i < vars; i++) {
            commandString +=push("constant","0");
        }
        writeTofile(commandString);
    }

    /**
     * Writes assembly code that affects the call command.
     * @param functionName - the function being called
     * @param numArgs - which have been pushed pushed onto the stack
     */
    public void writeCall(String functionName, String numArgs) {
        String returnLabel = functionName+"$ret."+counter++;
        String string = "//call\n@"+returnLabel.toUpperCase()+"\r\nD=A\r\n@SP\r\nA=M\r\nM=D\r\n@SP\r\nM=M+1\r\n";
        string += "@LCL\r\nD=M\n@SP\nA=M\nM=D\n@SP\r\nM=M+1\r\n";
        string += "@ARG\nD=M\n@SP\nA=M\nM=D\n@SP\r\nM=M+1\r\n";
        string += "@THIS\nD=M\n@SP\nA=M\nM=D\n@SP\r\nM=M+1\r\n";
        string += "@THAT\nD=M\n@SP\nA=M\nM=D\n@SP\r\nM=M+1\r\n";
        string +="@SP\r\nD=M\r\n@5\r\nD=D-A\r\n@"+numArgs+"\r\nD=D-A\r\n@ARG\r\nM=D\r\n";
        string += "@SP\r\nD=M\r\n@LCL\r\nM=D\r\n";
        writeTofile(string);
        functionName = functionName.replace(".vm",String.valueOf(counter++));
        writeTofile("//goto\n");
        writeGoto(functionName);
        writeLabel(returnLabel);
    }

    /**
     * Assembly code to restore the pointer segments.
     * @param index - of the current pointer which is being restored.
     * @param segment - the current segment being restored
     * @return The generated assembly code.
     */
    private String restoreSegmentPointers(int index, String segment){
        return "@"+index+"\nD=A\n@END_FRAME\r\nA=M-D\nD=M\r\n@"+segment+"\r\nM=D\r\n";
    }

    /**
     * Writes assembly code that affects the return command.
     */
    public void writeReturn(){
        String string = "//return\n@LCL\r\nD=M\r\n@END_FRAME\r\nM=D\r\n";
        string += "@5\r\nA=D-A\r\nD=M\n@RET\r\nM=D\r\n";
        string += "@SP\r\nAM=M-1\r\nD=M\r\n@ARG\r\nA=M\r\nM=D\r\n";
        string += "@ARG\r\nD=M\r\n@SP\r\nM=D+1\r\n";
        string += restoreSegmentPointers(1,"THAT");
        string += restoreSegmentPointers(2,"THIS");
        string += restoreSegmentPointers(3,"ARG");
        string += restoreSegmentPointers(4,"LCL");
        string += "@RET\r\nA=M\r\n0;JMP\r\n";
        writeTofile(string);
    }
}