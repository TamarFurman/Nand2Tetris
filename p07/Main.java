
/**
 * drives the process (VMTranslator)
 * read the file line by kine and send it to translate.
 */

public class Main {

    public static void main(String[] args) {
        String filePath = args[0];
        Parser parser = new Parser(filePath);
        String newFile = filePath.replace("vm","asm");
        CodeWriter codeWriter = new CodeWriter(newFile);
        while (parser.hasMoreCommands()){
            parser.advance();
            if(parser.commandType().equals("C_ARITHMETIC")){
                codeWriter.writeArithmetic(parser.arg1());
            }
            else {
                codeWriter.writePushPop(parser.commandType(),parser.arg1(),parser.arg2());
            }
        }
        codeWriter.close();

    }
}
