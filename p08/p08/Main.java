import java.io.File;
import java.util.ArrayList;

public class Main {
    private static CodeWriter codeWriter;
    private static Parser parser;
    private static Boolean hasSys = false;

    public static ArrayList<String> getVMFiles(String file) {
        File dir = new File(file);
        File[] files = dir.listFiles();
        String outputFile;
        ArrayList<String> vmFiles = new ArrayList<String>();
        if (dir.isFile()) {
            String path = dir.getAbsolutePath();
            if (!path.endsWith(".vm")) {
                throw new IllegalArgumentException(".vm file is required!");
            }
            vmFiles.add(file);
            outputFile = dir.getAbsolutePath().substring(0, dir.getAbsolutePath().lastIndexOf(".")) + ".asm";
            codeWriter = new CodeWriter(outputFile);
        } else if (dir.isDirectory()) {
            for (File f : files) {
                if(f.getName().equals("Sys.vm")) hasSys = true;
                if (f.getName().endsWith(".vm")) {
                    vmFiles.add(file + "/"+f.getName());
                }
            }
            if (vmFiles.size() == 0) {
                throw new IllegalArgumentException("No vm file in this directory");
            }
            outputFile = file+"/"+ file.substring(file.lastIndexOf("/")+1)+".asm";
            codeWriter = new CodeWriter(outputFile);
        }
        return vmFiles;
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage:java Main [filename|directory]");
            return;
        }
        ArrayList<String> vmFiles = getVMFiles(args[0].replace('\\','/'));
        if(hasSys)
            codeWriter.writeInit();
        for(String file:vmFiles) {
            codeWriter.setFileName(file);
            parser = new Parser(file);
            while (parser.hasMoreCommands()) {
                parser.advance();
                switch (parser.commandType()) {
                    case "C_ARITHMETIC":
                        codeWriter.WriteArithmetic(parser.arg1());
                        break;
                    case "C_PUSH":
                    case "C_POP":
                        codeWriter.WritePushPop(parser.commandType(), parser.arg1(), parser.arg2());
                        break;
                    case "C_IF":
                        codeWriter.writeIf(parser.arg1());
                        break;
                    case "C_GOTO":
                        codeWriter.writeGoto(parser.arg1());
                        break;
                    case "C_LABEL":
                        codeWriter.writeLabel(parser.arg1());
                        break;
                    case "C_FUNCTION":
                        codeWriter.writeFunction(parser.arg1(), parser.arg2());
                        break;
                    case "C_CALL":
                        codeWriter.writeCall(parser.arg1(), parser.arg2());
                        break;
                    case "C_RETURN":
                        codeWriter.writeReturn();
                        break;
                    default:
                        break;
                }
            }
        }
        codeWriter.close();
    }
}