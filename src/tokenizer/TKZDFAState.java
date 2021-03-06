package tokenizer;

public enum TKZDFAState {
    INITIAL_STATE,

    ZERO_STATE,
    HEX_X_STATE,
    FLOATING_DOT_STATE,
    FLOATING_E_STATE,
    UNSIGNED_INTEGER_STATE,

    PLUS_SIGN_STATE,
    MINUS_SIGN_STATE,
    DIVISION_SIGN_STATE,
    MULTIPLICATION_SIGN_STATE,
    IDENTIFIER_STATE,
    EQUAL_SIGN_STATE,
    SEMICOLON_STATE,
    COMMA_STATE,
    LEFTBRACKET_STATE,
    RIGHTBRACKET_STATE,

    LESSTHAN_STATE,
    GREATERTHAN_STATE,
    LEFTBRACE_STATE,
    RIGHTBRACE_STATE,
    NOTEQUAL_STATE;
}
