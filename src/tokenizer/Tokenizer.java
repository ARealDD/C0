package tokenizer;
import java.io.*;
import java.util.ArrayList;

import error.CompilationError;
import error.ErrorCode;
import tools.*;
import run.Main;

public class Tokenizer {

    /**
     * reader用于文件输入
     * buffer是一个行缓冲区，将换行装换为\n
     */
    boolean initialized = false;
    public static Reader reader;
    public static BufferedReader bf;
    ArrayList<String> buffer = new ArrayList<>();
    Pair<Integer,Integer> position = new Pair(0,0);

    ArrayList<Token> result = new ArrayList<>();

    /**
     * 一般构造方法
     * @param fin
     */
    public Tokenizer(File fin) throws IOException{
        this.reader = new FileReader(fin);
    }

    /**
     * 返回所有token
     * @return
     * @throws CompilationError
     */
    public ArrayList<Token> allToken() throws CompilationError {

        Token t;
        while (true) {
            try {
                t = nextToken();
            }catch (CompilationError err) {
                if (err.getErrorCode() == ErrorCode.ErrEOF)
                    return result;
                else
                    throw err;
            }
            result.add(t);
        }
    }


    /**
     * 检查初始化状态，检查是否到行末尾，检查token合法性
     * @return
     */
    Token nextToken() throws CompilationError {

        Token t;
        if (!initialized)
            readAll();
        if (isEOF())
            throw new CompilationError(new Pair<>(0,0),ErrorCode.ErrEOF);

        try {
            t = next();
            checkToken(t);
        }catch (CompilationError err) {
            throw err;
        }

        return t;
    }

    /**
     * 分析一个token并返回
     * @return
     */
    Token next() throws CompilationError{

        StringBuffer sb = new StringBuffer();
        Pair pos = new Pair(0,0);
        TKZDFAState state = TKZDFAState.INITIAL_STATE;

        while (true) {
            char ch;
            try {
                ch = nextChar();
            }catch (CompilationError err) {
                throw err;
            }
            switch (state) {
                /**
                 * 4 种空白符：空格（0x20, ' '）、水平制表符（0x09, '\t'）、换行符（0x0A, '\n'）、回车符（0x0D, '\r'）
                 * 10种数字：从0到9
                 * 52种英文字母：从a到z，从A到Z
                 * 32种标点字符：_ ( ) [ ] { } < = > . , : ; ! ? + - * / % ^ & | ~ \ " ' ` $ # @
                 */
                case INITIAL_STATE: {
                    boolean invalid = false;
                    if (isSpace(ch))
                        break;
                    else if (!isPrint(ch))
                        invalid = true;
                    else if (ch == '0')
                        state = TKZDFAState.ZERO_STATE;
                    else if (isNoneZeroDigit(ch))
                        state = TKZDFAState.UNSIGNED_INTEGER_STATE;
                    else if (Character.isLetter(ch))
                        state = TKZDFAState.IDENTIFIER_STATE;
                    else {
                        switch (ch) {

                            case '=':
                                state = TKZDFAState.EQUAL_SIGN_STATE;
                                break;
                            case '-':
                                state = TKZDFAState.MINUS_SIGN_STATE;
                                break;
                            case '+':
                                state = TKZDFAState.PLUS_SIGN_STATE;
                                break;
                            case '*':
                                state = TKZDFAState.MULTIPLICATION_SIGN_STATE;
                                break;
                            case '/':
                                state = TKZDFAState.DIVISION_SIGN_STATE;
                                break;
                            case '(':
                                state = TKZDFAState.LEFTBRACKET_STATE;
                                break;
                            case ')':
                                state = TKZDFAState.RIGHTBRACKET_STATE;
                                break;
                            case ';':
                                state = TKZDFAState.SEMICOLON_STATE;
                                break;
                            case ',':
                                state = TKZDFAState.COMMA_STATE;
                                break;
                            case '<':
                                state = TKZDFAState.LESSTHAN_STATE;
                                break;
                            case '>':
                                state = TKZDFAState.GREATERTHAN_STATE;
                                break;
                            case '!':
                                state = TKZDFAState.NOTEQUAL_STATE;
                                break;
                            case '{':
                                state = TKZDFAState.LEFTBRACE_STATE;
                                break;
                            case '}':
                                state = TKZDFAState.RIGHTBRACE_STATE;
                                break;
                            default:
                                invalid = true;
                                break;
                        }
                    }
                    if (state != TKZDFAState.INITIAL_STATE) {
                        pos = previousPos();
                        sb.append(ch);
                    }
                    if (invalid) {
                        pos = previousPos();
                        unreadLast();
                        throw new CompilationError(pos,ErrorCode.ErrInvalidInput);
                    }
                    break;
                }
                /**
                 * 数值
                 */
                case ZERO_STATE: {
                    if (ch == 'x' || ch == 'X') {
                        sb.append(ch);
                        state = TKZDFAState.HEX_X_STATE;
                    }
                    else if (ch == '.') {
                        sb.append(ch);
                        state = TKZDFAState.FLOATING_DOT_STATE;
                    }
                    else if (ch == 'E' || ch == 'e') {
                        sb.append(ch);
                        state = TKZDFAState.FLOATING_E_STATE;
                    }
                    else if (Character.isDigit(ch)) {
                        throw new CompilationError(pos,ErrorCode.ErrInvalidNumberFormat);
                    }
                    else {
                        unreadLast();
                        return new Token(TokenType.UNSIGNED_INTEGER,sb.toString(),pos,currentPos());
                    }
                    break;
                }
                case HEX_X_STATE: {
                    if (isHexDigit(ch))
                        sb.append(ch);
                    else if(Character.isLetter(ch)) {
                        sb.append(ch);
                        state = TKZDFAState.IDENTIFIER_STATE;
                    }
                    else {
                        unreadLast();
                        try {
                            return new Token(TokenType.HEXADECIMAL_INTEGER,sb.toString(),pos,currentPos());
                        }catch (CompilationError err) {
                            if (err.getErrorCode() == ErrorCode.ErrIntegerOverflow)
                                throw new CompilationError(pos,ErrorCode.ErrIntegerOverflow);
                        }
                    }
                    break;
                }
                case FLOATING_E_STATE: {
                    break;
                }
                case FLOATING_DOT_STATE: {
                    if (Character.isDigit(ch))
                        sb.append(ch);
                    else {
                        unreadLast();
                        return new Token(TokenType.FLOATING_VALUE,sb.toString(),pos,currentPos());
                    }
                    break;
                }
                case UNSIGNED_INTEGER_STATE: {
                    if (Character.isDigit(ch))
                        sb.append(ch);
                    else if (Character.isLetter(ch)) {
                        sb.append(ch);
                        state = TKZDFAState.IDENTIFIER_STATE;
                    }
                    else {
                        unreadLast();
                        try {
                            return new Token(TokenType.UNSIGNED_INTEGER,sb.toString(),pos,currentPos());
                        }catch (CompilationError err) {
                            if (err.getErrorCode() == ErrorCode.ErrIntegerOverflow)
                                throw new CompilationError(pos,ErrorCode.ErrIntegerOverflow);
                        }
                    }
                    break;
                }
                /**
                 * 标识符
                 */
                case IDENTIFIER_STATE: {
                    if (Character.isLetterOrDigit(ch))
                        sb.append(ch);
                    else {
                        unreadLast();
                        String str = sb.toString();
                        switch (str) {
                            case "const":
                                return new Token(TokenType.CONST,str,pos,currentPos());
                            case "void":
                                return new Token(TokenType.VOID,str,pos,currentPos());
                            case "int":
                                return new Token(TokenType.INT,str,pos,currentPos());
                            case "char":
                                return new Token(TokenType.CHAR,str,pos,currentPos());
                            case "double":
                                return new Token(TokenType.DOUBLE,str,pos,currentPos());
                            case "struct":
                                return new Token(TokenType.STRUCT,str,pos,currentPos());
                            case "if":
                                return new Token(TokenType.IF,str,pos,currentPos());
                            case "else":
                                return new Token(TokenType.ELSE,str,pos,currentPos());
                            case "switch":
                                return new Token(TokenType.SWITCH,str,pos,currentPos());
                            case "case":
                                return new Token(TokenType.CASE,str,pos,currentPos());
                            case "default":
                                return new Token(TokenType.DEFAULT,str,pos,currentPos());
                            case "while":
                                return new Token(TokenType.WHILE,str,pos,currentPos());
                            case "for":
                                return new Token(TokenType.FOR,str,pos,currentPos());
                            case "do":
                                return new Token(TokenType.DO,str,pos,currentPos());
                            case "return":
                                return new Token(TokenType.RETURN,str,pos,currentPos());
                            case "break":
                                return new Token(TokenType.BREAK,str,pos,currentPos());
                            case "continue":
                                return new Token(TokenType.CONTINUE,str,pos,currentPos());
                            case "print":
                                return new Token(TokenType.PRINT,str,pos,currentPos());
                            case "scan":
                                return new Token(TokenType.SCAN,str,pos,currentPos());
                                default:
                                    return new Token(TokenType.IDENTIFIER,str,pos,currentPos());
                        }

                    }
                    break;
                }
                /**
                 * 符号集合 +|-|*|/|=|<|<=|>|>=|==|{|}
                 */
                case PLUS_SIGN_STATE: {
                    unreadLast();
                    return new Token(TokenType.PLUS_SIGN,sb.toString(),pos,currentPos());
                }
                case MINUS_SIGN_STATE: {
                    unreadLast();
                    return new Token(TokenType.MINUS_SIGN,sb.toString(),pos,currentPos());
                }
                case MULTIPLICATION_SIGN_STATE: {
                    unreadLast();
                    return new Token(TokenType.MULTIPLICATION_SIGN,sb.toString(),pos,currentPos());
                }
                case DIVISION_SIGN_STATE: {
                    unreadLast();
                    return new Token(TokenType.DIVISION_SIGN,sb.toString(),pos,currentPos());
                }
                case LEFTBRACKET_STATE: {
                    unreadLast();
                    return new Token(TokenType.LEFT_BRACKET,sb.toString(),pos,currentPos());
                }
                case RIGHTBRACKET_STATE: {
                    unreadLast();
                    return new Token(TokenType.RIGHT_BRACKET,sb.toString(),pos,currentPos());
                }
                case SEMICOLON_STATE: {
                    unreadLast();
                    return new Token(TokenType.SEMICOLON,sb.toString(),pos,currentPos());
                }
                case COMMA_STATE: {
                    unreadLast();
                    return new Token(TokenType.COMMA,sb.toString(),pos,currentPos());
                }
                case LEFTBRACE_STATE: {
                    unreadLast();
                    return new Token(TokenType.LEFT_BRACE,sb.toString(),pos,currentPos());
                }
                case RIGHTBRACE_STATE: {
                    unreadLast();
                    return new Token(TokenType.RIGHT_BRACE,sb.toString(),pos,currentPos());
                }
                case EQUAL_SIGN_STATE: {
                    if (ch == '=') {
                        sb.append(ch);
                        return new Token(TokenType.EQUAL_SIGN, sb.toString(), pos, currentPos());
                    }
                    else {
                        unreadLast();
                        return new Token(TokenType.ASSIGNMENT_SIGN,sb.toString(),pos,currentPos());
                    }
                }
                case NOTEQUAL_STATE: {
                    if (ch == '=') {
                        sb.append(ch);
                        return new Token(TokenType.NOTEQUAL_SIGN,sb.toString(),pos,currentPos());
                    }
                    else {
                        throw new CompilationError(pos,ErrorCode.ErrUnexpectedToken);
                    }
                }
                case LESSTHAN_STATE: {
                    if (ch == '=') {
                        sb.append(ch);
                        return new Token(TokenType.LESSTHANEQUAL_SIGN, sb.toString(), pos, currentPos());
                    }
                    else {
                        unreadLast();
                        return new Token(TokenType.LESSTHAN_SIGN,sb.toString(),pos,currentPos());
                    }
                }
                case GREATERTHAN_STATE: {
                    if (ch == '=') {
                        sb.append(ch);
                        return new Token(TokenType.GREATERTHANEQUAL_SIGN,sb.toString(),pos,currentPos());
                    }
                    else {
                        unreadLast();
                        return new Token(TokenType.GREATERTHAN_SIGN,sb.toString(),pos,currentPos());
                    }
                }
                default: {
                    Main.DieAndPrint("unhandled state.");
                    break;
                }
            }
        }
    }

    /**
     * 是否是十六进制数
     * @param ch
     * @return
     */
    boolean isHexDigit(char ch) {
        return Character.isDigit(ch) || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <='F');
    }
    /**
     * 非0数
     * @param ch
     * @return
     */
    boolean isNoneZeroDigit(char ch) {
        return ch <= '9' && ch >= '1';
    }
    /**
     * 空字符
     * @param ch
     * @return
     */
    boolean isSpace(char ch) {
        return ch == 0x20 || ch == 0x09 || ch == 0x0A || ch == 0x0D;
    }

    /**
     * 不可接受的字符
     * @param ch
     * @return
     */
    boolean isPrint(char ch) {
        int ascii = ch;
        if (ascii >= 32 && ascii <= 126)
            return true;
        return false;
    }

    /**
     * 读入所有数据
     */
    void readAll() {

        try {
            bf = new BufferedReader(reader);
            String str;
            while ((str = bf.readLine()) != null) {
                str = str + '\n';
                buffer.add(str);
            }
            bf.close();
            reader.close();
            initialized = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 检查token合法性
     * @param token
     * @throws CompilationError
     */
    void checkToken(Token token) throws CompilationError {

        switch (token.getType()) {
            case IDENTIFIER: {
                String value = token.getValue();
                if(Character.isDigit(value.charAt(0))) {
                    throw new CompilationError(token.getStartPos(), ErrorCode.ErrInvalidIdentifier);
                }
            }
            default:
                break;
        }
    }

    /**
     * 获取下一个字符位置
     * @return
     */
    Pair nextPos() {
        if (position.getFirst() >= buffer.size())
            Main.DieAndPrint("advance after EOF");
        if (position.getSecond() == buffer.get(position.getFirst()).length() - 1)
            return new Pair(position.getFirst() + 1,0);
        else
            return new Pair(position.getFirst(),position.getSecond() + 1);
    }

    /**
     * 返回当前位置
     * @return
     */
    Pair currentPos() {
        return position;
    }

    /**
     * 返回上一个位置
     * @return
     */
    Pair previousPos() {
        if (position.getFirst() == 0 && position.getSecond() ==0)
            Main.DieAndPrint("previous position from beginning");
        if (position.getSecond() == 0)
            return new Pair(position.getFirst() - 1,buffer.get(position.getFirst()-1).length()-1);
        else
            return new Pair(position.getFirst(),position.getSecond() - 1);
    }

    /**
     * 读取下一个字符
     * @return
     * @throws CompilationError
     */
    char nextChar() throws CompilationError{
        if (isEOF())
            throw new CompilationError(position,ErrorCode.ErrEOF);
        char result = buffer.get(position.getFirst()).charAt(position.getSecond());
        position = nextPos();
        return result;
    }

    /**
     * 判断是否读到了文件尾
     * @return
     */
    boolean isEOF() {
        return position.getFirst() >= buffer.size();
    }

    /**
     * 退回一个字符
     */
    void unreadLast() {
        position = previousPos();
    }

}
