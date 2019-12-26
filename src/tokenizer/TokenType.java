package tokenizer;

public enum TokenType {

    NULL_TOKEN("NullToken"),
    /**
     * 变量
     */
    UNSIGNED_INTEGER("UnsignedInteger"),
    HEXADECIMAL_INTEGER("HexadecimalInteger"),
    FLOATING_VALUE("FloatingValue"),
    /**
     * 标识符
     */
    IDENTIFIER("Identifier"),
    CONST("Const"),
    VOID("Void"),
    INT("Int"),
    CHAR("Char"),
    DOUBLE("Double"),
    STRUCT("Struct"),
    IF("If"),
    ELSE("Else"),
    SWITCH("Switch"),
    CASE("Case"),
    DEFAULT("Default"),
    WHILE("While"),
    FOR("For"),
    DO("Do"),
    RETURN("Return"),
    BREAK("Break"),
    CONTINUE("Continue"),
    PRINT("Print"),
    SCAN("Scan"),
    /**
     * 符号
     */
    PLUS_SIGN("PlusSign"),
    MINUS_SIGN("MinusSign"),
    MULTIPLICATION_SIGN("MultiplicationSign"),
    DIVISION_SIGN("DivisionSign"),
    ASSIGNMENT_SIGN("AssignmentSign"),
    COMMA("Comma"),
    SEMICOLON("Semicolon"),
    LEFT_BRACKET("LeftBracket"),
    RIGHT_BRACKET("RightBracket"),
    LEFT_BRACE("LeftBrace"),
    RIGHT_BRACE("RightBrace"),
    /**
     * 条件符号
     */
    LESSTHAN_SIGN("Less-thanSign"),
    GREATERTHAN_SIGN("Greater-thanSign"),
    LESSTHANEQUAL_SIGN("Less-than-equalSign"),
    GREATERTHANEQUAL_SIGN("Greater-than-equalSign"),
    EQUAL_SIGN("EqualSign"),
    NOTEQUAL_SIGN("NotEqualSign"),
    ;

    private final String description;

    private TokenType(final String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
