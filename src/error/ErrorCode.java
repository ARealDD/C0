package error;

public enum ErrorCode {
    /**
     * 错误类型
     */
    Unspecified("未定义错误"),
    ErrNoBegin("没有找到合法的程序开头"),
    ErrStreamError("文件流错误"),
    ErrEOF("已读到文件尾"),
    ErrInvalidInput("不合法的输入"),
    ErrInvalidIdentifier("不合法的标识符"),
    ErrInvalidNumberFormat("不合法的数值格式"),
    ErrIntegerOverflow("整型字面量数值溢出"),
    ErrNeedTypeSpecifier("缺少数据类型"),
    ErrNeedIdentifier("缺少标识符"),
    ErrConstantNeedValue("常量声明缺少数值"),
    ErrVoidTypeVar("变量声明类型不能为void"),
    ErrInvalidVoid("void型变量不能参加运算"),
    ErrNoSemicolon("缺少分号"),
    ErrNoBracket("缺少括号"),
    ErrNoBrace("缺少大括号"),
    ErrNotDeclared("未声明的变量"),
    ErrUndefinedFunction("未声明的函数名"),
    ErrAssignToConstant("无法对常量赋值"),
    ErrDuplicateDeclaration("重复的变量名"),
    ErrNotInitialized("变量未初始化"),
    ErrInvalidAssignment("不合法的赋值语句"),
    ErrNoMoreToken("没有更多token了"),
    ErrInvalidFunctionDefinition("不合法的函数声明"),
    ErrInvalidEnd("不合法的程序结尾"),
    ErrUnexpectedToken("意外的符号"),
    ErrFailedExpression("表达式两侧数据类型不同"),
    ErrInvalidReturnType("返回值与函数定义不符"),
    ErrNeedReturnStatement("函数需要返回语句"),
    ;

    private final String description;

    private ErrorCode(final String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
