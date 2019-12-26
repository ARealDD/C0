package tokenizer;

import error.CompilationError;
import error.ErrorCode;
import tools.Pair;

public class Token {

    private TokenType type;
    private String value;
    private int intValue;
    private double doubleValue;
    private Pair<Integer,Integer> startPos;
    private Pair<Integer,Integer> endPos;

    public Token(TokenType type,String value,int startLine,int startColumn,int endLine,int endColumn) {
        this.type = type;
        this.value = value;
        this.startPos = new Pair<Integer, Integer>(new Integer(startLine),new Integer(startColumn));
        this.endPos = new Pair<Integer, Integer>(new Integer(endLine),new Integer(endColumn));
    }
    public Token(TokenType type,String value,Pair<Integer,Integer> startPos,Pair<Integer,Integer> endPos) throws CompilationError {
        this.type = type;
        this.value = value;
        this.startPos = startPos;
        this.endPos = endPos;
        try {
            if (this.type == TokenType.UNSIGNED_INTEGER)
                intValue = Integer.parseInt(value);
            else if (this.type == TokenType.FLOATING_VALUE)
                doubleValue = Double.parseDouble(value);
            else if (this.type == TokenType.HEXADECIMAL_INTEGER) {
                String s = this.value.substring(2);
                intValue = Integer.valueOf(s, 16);
            }
        }catch (NumberFormatException e) {
            throw new CompilationError(new Pair<>(0,0), ErrorCode.ErrIntegerOverflow);
        }
    }

    public TokenType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public int getIntValue() {
        return intValue;
    }

    public double getDoubleValue() {
        return doubleValue;
    }

    public Pair<Integer, Integer> getStartPos() {
        return startPos;
    }

    public Pair<Integer, Integer> getEndPos() {
        return endPos;
    }

    @Override
    public String toString() {
        String s = "Line: " + startPos.getFirst().toString() + " " +
                "Column: " + startPos.getSecond().toString() + " " +
                "Type: " + type.getDescription() + " " +
                "Value: " + value;
        if (type == TokenType.UNSIGNED_INTEGER || type == TokenType.HEXADECIMAL_INTEGER)
            s = s + "数值：" +intValue;
        else if (type == TokenType.FLOATING_VALUE)
            s = s + "数值：" + doubleValue;
        return s;
    }
}
