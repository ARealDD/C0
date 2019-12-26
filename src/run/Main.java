package run;

import analyser.*;
import error.*;
import tokenizer.*;
import java.io.*;

import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {

        File fin;
        File fout = new File("out");
        int argv = args.length;
        if (argv == 0)
            System.out.println("Usage:\n" +
                    "  cc0 [options] input [-o file]\n" +
                    "or \n" +
                    "  cc0 [-h]\n" +
                    "Options:\n" +
                    "  -s        将输入的 c0 源代码翻译为文本汇编文件\n" +
                    "  -c        将输入的 c0 源代码翻译为二进制目标文件\n" +
                    "  -h        显示关于编译器使用的帮助\n" +
                    "  -o file   输出到指定的文件 file");
        if (args[0] == "-h")
            System.out.println("Usage:\n" +
                    "  cc0 [options] input [-o file]\n" +
                    "or \n" +
                    "  cc0 [-h]\n" +
                    "Options:\n" +
                    "  -s        将输入的 c0 源代码翻译为文本汇编文件\n" +
                    "  -c        将输入的 c0 源代码翻译为二进制目标文件\n" +
                    "  -h        显示关于编译器使用的帮助\n" +
                    "  -o file   输出到指定的文件 file");
        else if (args[0] == "-s") {
            fin = new File(args[1]);
            if (argv > 2) {
                if (args[2] == "-o")
                    fout = new File(args[3]);
            }
            ArrayList tokens;
            try {
                tokens = tokenize(fin);
                analyser(tokens,fout);
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        else if (args[0] == "-c") {
            fin = new File(args[1]);
            if (argv > 2) {
                if (args[2] == "-o")
                    fout = new File(args[3]);
            }
            ArrayList tokens;
            try {
                tokens = tokenize(fin);
                analyser(tokens,fout);
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        //File fin = new File("src/input.txt");
        //File fout = new File("src/output.txt");

    }
    private static void analyser(ArrayList tokens,File output) throws IOException {
        Analyser analyser;
        try {
            analyser = new Analyser(tokens,output);
        }catch (IOException e){
            throw e;
        }
        try {
            analyser.analyse();
        }catch (CompilationError err){
            System.out.println(err.getMessage());
            System.exit(1);
        }
    }

    /**
     * 用于测试tokenizer的方法
     * @param input
     * @param output
     * @throws IOException
     */
    private static void printTokenize(File input,File output) throws IOException {

        ArrayList<Token> tokens = tokenize(input);
        FileWriter fw = new FileWriter(output);
        for(Token token:tokens) {
            fw.write(token.toString() + "\n");
        }
        fw.close();
    }

    private static ArrayList tokenize(File input) throws IOException{

        Tokenizer tokenizer;
        try {
            tokenizer = new Tokenizer(input);
        }catch (IOException e) {
            throw e;
        }

        ArrayList result = new ArrayList();
        try {
            result = tokenizer.allToken();
        }catch (CompilationError err){
            System.out.println(err.getMessage());
            System.exit(1);
        }
        return result;
    }

    /**
     * 出错退出
     * @param condition
     */
    public static void DieAndPrint(String condition) {
        System.out.print("Exception:"+condition+"\n");
        System.out.print("The program should not reach here.\n");
        System.out.print("Please check your program carefully.\n");
        System.out.print("If you believe it's not your fault, please report this to TAs.\n");
        System.exit(2);
    }
}
