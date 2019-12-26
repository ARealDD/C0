package analyser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import com.sun.org.apache.regexp.internal.RE;
import org.omg.CORBA.COMM_FAILURE;
import run.Main;
import error.CompilationError;
import error.ErrorCode;
import tokenizer.*;
import tools.Pair;

public class Analyser {

    FileWriter fw;
    //词法分析的结果
    ArrayList<Token> tokens;
    //错误出现的位置
    Pair<Integer,Integer> position = new Pair(0,0);
    //当前分析的token在tokens中的偏移
    int index = 0;

    Assembly assembly = new Assembly();
    FunctionBody instructionBody = assembly.getStarts();
    TokenTable globalTable = assembly.getStarts().getTokenTable();
    TokenTable curTable = globalTable;

    // 用于标记一个<variable-declaration>是否是常量声明
    boolean isConst = false;
    // 用于记录上一条语句的offset
    int lastInstuctionIndex = -1;
    // 用于记录当前函数体是否有返回语句
    boolean hasReturn = false;


    public Analyser(ArrayList tokens, File fout) throws IOException {
        this.tokens = tokens;
        this.fw = new FileWriter(fout);
    }

    public void analyse() throws CompilationError,IOException{
        try {
            program();
        }catch (CompilationError err) {
            if (err.getErrorCode() == ErrorCode.ErrNoMoreToken){
                throw new CompilationError(position,ErrorCode.ErrInvalidEnd);
            }
            throw err;
        }
        fw.write(assembly.toString());
        fw.close();
        System.out.println("编译正常完成！");
    }

    /**
     * <C0-program> ::= {<variable-declaration>}{<function-definition>}
     * 变量声明+函数声明
     * @throws CompilationError
     */
    void program() throws CompilationError{

        boolean into = false;
        try {
            // 全局符号表开头添加一个不存在的函数名占位
            globalTable.addVariable(new Token(TokenType.IDENTIFIER,"zymzymzym",new Pair<>(0,0),new Pair<>(0,0)),TokenType.VOID);

            /**
             * {<variable-declaration>}
             */
            while (intoVar()) {
                variableDeclaration();
                into = true;
            }

            /**
             * {<function-definition>}
             */
            while (intoFunc()) {
                functionDefinition();
                into = true;
            }

            if (!into)
                throw new CompilationError(position,ErrorCode.ErrNoBegin);


        }catch (CompilationError err) {
            throw err;
        }
        return;
    }

    /**
     * <variable-declaration> ::= [<const-qualifier>]<type-specifier><init-declarator-list>’;’
     * third = '=' | ',' | ';'
     *
     * <init-declarator-list> ::= <init-declarator>{','<init-declarator>}
     * <type-specifier>         ::= <simple-type-specifier>
     * <simple-type-specifier>  ::= 'void'|'int'|'char'|'double'
     * <const-qualifier>        ::= 'const'
     * @throws CompilationError
     */
    void variableDeclaration() throws CompilationError {

        Token next;


        try {
            /**
             * [<const-qualifier>]
             */
            next = nextToken();
            if (next.getType() == TokenType.CONST) {
                //该变量声明是常量声明
                isConst = true;
            }
            else
                unreadToken();

            /**
             * <type-specifier>
             */
            TokenType variableType = typeSpecifier();
            if (variableType == TokenType.VOID)
                throw new CompilationError(position,ErrorCode.ErrVoidTypeVar);

            /**
             * <init-declarator-list> ::= <init-declarator>{','<init-declarator>}
             */
            initDeclarator(variableType);
            while (true) {
                next = nextToken();
                if(next.getType() == TokenType.COMMA) {
                    initDeclarator(variableType);
                }
                else {
                    unreadToken();
                    break;
                }
            }

            /**
             * ’;’
             */
            next = nextToken();
            if (next.getType() != TokenType.SEMICOLON) {
                unreadToken();
                throw new CompilationError(position, ErrorCode.ErrNoSemicolon);
            }

        }catch (CompilationError err){
            throw err;
        }

        isConst = false;
        return;
    }

    /**
     * <init-declarator> ::= <identifier>[<initializer>]
     * <initializer> ::= '='<expression>
     * @throws CompilationError
     */
    void initDeclarator(TokenType type) throws CompilationError {
        Token next;


        try {
            /**
             * <identifier>
             */
            next = nextToken();
            if (next.getType() != TokenType.IDENTIFIER) {
                unreadToken();
                throw new CompilationError(position,ErrorCode.ErrNeedIdentifier);
            }
            if (curTable.isDeclared(next)) {
                unreadToken();
                throw new CompilationError(position,ErrorCode.ErrDuplicateDeclaration);
            }
            Token token = next;

            /**
             * peek'='
             */
            next = nextToken();
            // 常量声明
            if (isConst) {
                // 没有初值
                if (next.getType() != TokenType.ASSIGNMENT_SIGN) {
                    unreadToken();
                    throw new CompilationError(position,ErrorCode.ErrConstantNeedValue);
                }
                // 有初值
                else {
                    expression();
                    curTable.addConstant(token,type);
                }

            }
            // 变量声明
            else {
                // 有初值
                if (next.getType() == TokenType.ASSIGNMENT_SIGN) {
                    expression();
                    curTable.addVariable(token,type);
                }
                // 没有初值
                else {
                    unreadToken();
                    curTable.addUninitializedVariable(token,type);
                }
            }

        }catch (CompilationError err) {
            throw err;
        }
        return;
    }

    /**
     * <type-specifier>         ::= <simple-type-specifier>
     * <simple-type-specifier>  ::= 'void'|'int'|'char'|'double'
     */
    TokenType typeSpecifier() throws CompilationError {
        Token next;

        try {
            next = nextToken();
            if (isType(next)) {
                return next.getType();
            }
            else {
                unreadToken();
                throw new CompilationError(position,ErrorCode.ErrNeedTypeSpecifier);
            }
        }catch (CompilationError err) {
            throw err;
        }
    }

    /**
     * <function-definition> ::= <type-specifier><identifier><parameter-clause><compound-statement>
     * third = '('
     * <parameter-clause> ::= '(' [<parameter-declaration-list>] ')’
     * 语义：函数调用的标识符，必须是当前作用域中可见的，被声明为函数的标识符
     * @throws CompilationError
     */
    void functionDefinition() throws CompilationError {
        Token next;
        try {
            /**
             * <type-specifier>
             */
            TokenType returnType = typeSpecifier();

            /**
             * <identifier>
             */
            next = nextToken();
            if (next.getType() != TokenType.IDENTIFIER) {
                unreadToken();
                throw new CompilationError(position,ErrorCode.ErrNeedIdentifier);
            }
            Token funcName = next;
            // 函数名写入常量池
            int index = assembly.getFunctionsIndex();
            int nameIndex = assembly.addConstant('S',next.getValue());
            // 重置指令offest
            lastInstuctionIndex = -1;


            /**
             * '('
             */
            next = nextToken();
            if (next.getType() != TokenType.LEFT_BRACKET) {
                unreadToken();
                throw new CompilationError(position, ErrorCode.ErrInvalidFunctionDefinition);
            }
            // 新建函数体，切换至局部表
            instructionBody = assembly.newFunction(returnType);
            curTable = instructionBody.getTokenTable();
            // 函数名写入符号表
            curTable.addVariable(funcName,returnType);

            /**
             * <parameter-clause-list>
             * first = const|int|void|double|char
             */
            int params_size = 0;
            next = nextToken();
            if (next.getType() == TokenType.CONST || isType(next)) {
                unreadToken();
                params_size = parameterClauseList();
            }
            else
                unreadToken();

            /**
             * ')'
             */
            next = nextToken();
            if (next.getType() != TokenType.RIGHT_BRACKET) {
                unreadToken();
                throw new CompilationError(position, ErrorCode.ErrInvalidFunctionDefinition);
            }
            // 添加函数表
            assembly.addFunction(index,nameIndex,params_size,1);

            /**
             * <compound-statement>
             */
            compoundStatement();

        }catch (CompilationError err) {
            throw err;
        }
        return;
    }

    /**
     * <parameter-declaration-list> ::= <parameter-declaration>{','<parameter-declaration>}
     */
    int parameterClauseList() throws CompilationError {
        Token next;
        int cnt = 1;
        try {

            parameterDeclaration();
            while (true) {
                next = nextToken();
                if(next.getType() == TokenType.COMMA) {
                    parameterDeclaration();
                    cnt ++;
                }
                else {
                    unreadToken();
                    break;
                }
            }

        }catch (CompilationError err) {
            throw err;
        }
        return cnt;
    }

    /**
     * <parameter-declaration> ::= [<const-qualifier>]<type-specifier><identifier>
     * @throws CompilationError
     */
    void parameterDeclaration() throws CompilationError {
        Token next;
        try {

            /**
             * [<const-qualifier>]
             */
            next = nextToken();
            if (next.getType() == TokenType.CONST) {
                //该变量声明是常量声明
                isConst = true;
            }
            else
                unreadToken();

            /**
             * <type-specifier>
             */
            TokenType type = typeSpecifier();
            if (type == TokenType.VOID)
                throw new CompilationError(position,ErrorCode.ErrVoidTypeVar);

            /**
             * <identifier>
             */
            next = nextToken();
            if (next.getType() != TokenType.IDENTIFIER) {
                unreadToken();
                throw new CompilationError(position,ErrorCode.ErrNeedIdentifier);
            }
            if (curTable.isDeclared(next)) {
                unreadToken();
                throw new CompilationError(position,ErrorCode.ErrDuplicateDeclaration);
            }
            if (isConst)
                curTable.addConstant(next,type);
            else
                curTable.addParams(next,type);

        }catch (CompilationError err) {
            throw err;
        }
        isConst = false;
        return;
    }

    /**
     * <compound-statement> ::= '{' {<variable-declaration>} <statement-seq> '}'
     * @throws CompilationError
     */
    void compoundStatement() throws CompilationError {
        Token next;

        try {
            /**
             * '{'
             */
            next = nextToken();
            if (next.getType() != TokenType.LEFT_BRACE) {
                unreadToken();
                throw new CompilationError(position,ErrorCode.ErrInvalidFunctionDefinition);
            }


            /**
             * {<variable-declaration>}
             */
            while (intoVar()) {
                variableDeclaration();
            }

            /**
             * <statement-seq>
             */
            statementSeq();

            // 判断是否有返回语句
            if (!hasReturn)
                throw new CompilationError(position,ErrorCode.ErrNeedReturnStatement);

            /**
             * '}'
             */
            next = nextToken();
            if (next.getType() != TokenType.RIGHT_BRACE) {
                unreadToken();
                throw new CompilationError(position,ErrorCode.ErrInvalidFunctionDefinition);
            }

        }catch (CompilationError err) {
            throw err;
        }

        return;
    }

    /**
     * <statement-seq> ::= {<statement>}
     * @throws CompilationError
     */
    void statementSeq() throws CompilationError{
        try {
            while (intoStatement()) {
                statement();
            }
        }catch (CompilationError err) {
            throw err;
        }
        return;
    }

    /**
     * <statement> ::=
     *      '{' <statement-seq> '}'
     *      |<condition-statement>  'if'
     *      |<loop-statement>       'while'
     *      |<jump-statement>       'return'
     *      |<print-statement>      'print'
     *      |<scan-statement>       'scan'
     *      |<assignment-expression>‘;’     'id'
     *      |<function-call>‘;’             'id'
     *      |';'
     * @throws CompilationError
     */
    void statement() throws CompilationError {
        Token next;
        try {
            next = nextToken();
            switch (next.getType()) {
                case LEFT_BRACE: {
                    statementSeq();
                    next = nextToken();
                    if (next.getType() != TokenType.RIGHT_BRACE) {
                        unreadToken();
                        throw new CompilationError(position,ErrorCode.ErrNoBrace);
                    }
                    break;
                }
                case IF: {
                    unreadToken();
                    conditionStatement();
                    break;
                }
                case WHILE: {
                    unreadToken();
                    loopStatement();
                    break;
                }
                case RETURN: {
                    unreadToken();
                    jumpStatement();
                    break;
                }
                case PRINT: {
                    unreadToken();
                    printStatement();
                    break;
                }
                case SCAN: {
                    unreadToken();
                    scanStatement();
                    break;
                }
                case IDENTIFIER: {
                    next = nextToken();
                    if (next.getType() == TokenType.ASSIGNMENT_SIGN) {
                        unreadToken();
                        unreadToken();
                        assignmentStatement();
                        /**
                         * ';'
                         */
                        next = nextToken();
                        if (next.getType() != TokenType.SEMICOLON) {
                            unreadToken();
                            throw new CompilationError(position,ErrorCode.ErrNoSemicolon);
                        }
                        break;
                    }
                    else if (next.getType() == TokenType.LEFT_BRACKET) {
                        unreadToken();
                        unreadToken();
                        functionCall();
                        /**
                         * ';'
                         */
                        next = nextToken();
                        if (next.getType() != TokenType.SEMICOLON) {
                            unreadToken();
                            throw new CompilationError(position,ErrorCode.ErrNoSemicolon);
                        }
                        break;
                    }
                    else {
                        unreadToken();
                        throw new CompilationError(position,ErrorCode.ErrUnexpectedToken);
                    }
                }
                case SEMICOLON: {
                    break;
                }
            }
        }catch (CompilationError err) {
            throw err;
        }
        return;
    }

    /**
     * <condition-statement> ::= 'if' '(' <condition> ')' <statement> ['else' <statement>]
     * @throws CompilationError
     */
    void conditionStatement() throws CompilationError {
        try {
            Token next;

            /**
             * 'if' '(' <condition> ')' <statement>
             */
            next = nextToken();
            if(next.getType() != TokenType.IF) {
                unreadToken();
                throw new CompilationError(position,ErrorCode.ErrUnexpectedToken);
            }
            next = nextToken();
            if (next.getType() != TokenType.LEFT_BRACKET) {
                unreadToken();
                throw new CompilationError(position,ErrorCode.ErrNoBracket);
            }

            String instruction = condition();

            next = nextToken();
            if (next.getType() != TokenType.RIGHT_BRACKET) {
                unreadToken();
                throw new CompilationError(position,ErrorCode.ErrNoBracket);
            }

            // 插入jmp指令
            lastInstuctionIndex = instructionBody.addInstruction(instruction,0);
            // 保存jump语句的index，留待以后修改
            int jumpIndex = lastInstuctionIndex;

            statement();

            // if执行完成后，跳转到else之后的第一条语句
            lastInstuctionIndex = instructionBody.addInstruction("jmp",0);
            // nop占位，上一个if跳转到这里
            lastInstuctionIndex = instructionBody.addInstruction("nop");
            // 修改上一个jmp指令
            instructionBody.changeJumpOffset(jumpIndex,lastInstuctionIndex);
            // 保存nop之前的jmp指令的offset
            jumpIndex = lastInstuctionIndex - 1;

            /**
             * ['else' <statement>]
             */
            next = nextToken();
            if (next.getType() != TokenType.ELSE)
                unreadToken();
            // 如果存在else
            else {
                statement();
            }

            lastInstuctionIndex = instructionBody.addInstruction("nop");
            instructionBody.changeJumpOffset(jumpIndex,lastInstuctionIndex);

        }catch (CompilationError err) {
            throw err;
        }
        return;
    }

    /**
     * <loop-statement> ::= 'while' '(' <condition> ')' <statement>
     * @throws CompilationError
     */
    void loopStatement() throws CompilationError {
        Token next;
        try {

            // 循环开始offset
            int beginWhile = lastInstuctionIndex + 1;

            /**
             * 'while' '(' <condition> ')' <statement>
             */
            next = nextToken();
            if(next.getType() != TokenType.WHILE) {
                unreadToken();
                throw new CompilationError(position,ErrorCode.ErrUnexpectedToken);
            }
            next = nextToken();
            if (next.getType() != TokenType.LEFT_BRACKET) {
                unreadToken();
                throw new CompilationError(position,ErrorCode.ErrNoBracket);
            }

            String instruction = condition();

            next = nextToken();
            if (next.getType() != TokenType.RIGHT_BRACKET) {
                unreadToken();
                throw new CompilationError(position,ErrorCode.ErrNoBracket);
            }

            // 插入jmp指令
            lastInstuctionIndex = instructionBody.addInstruction(instruction,0);
            // 保存jump语句的index，留待以后修改
            int jumpIndex = lastInstuctionIndex;

            statement();

            // 跳转到循环开始
            lastInstuctionIndex = instructionBody.addInstruction("jmp",beginWhile);
            // 循环结束占位符
            lastInstuctionIndex = instructionBody.addInstruction("nop");
            instructionBody.changeJumpOffset(jumpIndex,lastInstuctionIndex);

        }catch (CompilationError err) {
            throw err;
        }

        return;
    }

    /**
     * <jump-statement> ::= <return-statement>
     * <return-statement> ::= 'return' [<expression>] ';'
     * @throws CompilationError
     */
    TokenType jumpStatement() throws CompilationError {
        TokenType returnType;
        Token next;
        try {
            /**
             * 'return' [<expression>] ';'
             */
            next = nextToken();
            if(next.getType() != TokenType.RETURN) {
                unreadToken();
                throw new CompilationError(position,ErrorCode.ErrUnexpectedToken);
            }

            if (intoExpression()) {
                if (instructionBody.returnValue == TokenType.VOID)
                    throw new CompilationError(position,ErrorCode.ErrInvalidReturnType);
                returnType = expression();
                lastInstuctionIndex = instructionBody.addInstruction("iret");
            }
            else {
                if (instructionBody.returnValue != TokenType.VOID)
                    throw new CompilationError(position,ErrorCode.ErrInvalidReturnType);
                lastInstuctionIndex = instructionBody.addInstruction("ret");
                returnType = TokenType.VOID;
            }

            next = nextToken();
            if (next.getType() != TokenType.SEMICOLON) {
                unreadToken();
                throw new CompilationError(position,ErrorCode.ErrNoSemicolon);
            }

            // 标记return语句的出现
            hasReturn = true;

        }catch (CompilationError err) {
            throw err;
        }
        return returnType;
    }

    /**
     * <scan-statement>  ::= 'scan' '(' <identifier> ')' ';'
     * @throws CompilationError
     */
    void scanStatement() throws CompilationError {
        Token next;
        try {
            /**
             * 'scan' '(' <identifier> ')' ';'
             */
            next = nextToken();
            if (next.getType() != TokenType.SCAN) {
                unreadToken();
                throw new CompilationError(position,ErrorCode.ErrUnexpectedToken);
            }
            next = nextToken();
            if (next.getType() != TokenType.LEFT_BRACKET) {
                unreadToken();
                throw new CompilationError(position,ErrorCode.ErrNoBracket);
            }
            next = nextToken();
            if (next.getType() != TokenType.IDENTIFIER) {
                unreadToken();
                throw new CompilationError(position,ErrorCode.ErrNeedIdentifier);
            }
            // 保存一下存入位置
            Token id = next;

            next = nextToken();
            if (next.getType() != TokenType.RIGHT_BRACKET) {
                unreadToken();
                throw new CompilationError(position,ErrorCode.ErrNoBracket);
            }
            next = nextToken();
            if (next.getType() != TokenType.SEMICOLON) {
                unreadToken();
                throw new CompilationError(position,ErrorCode.ErrNoSemicolon);
            }

            // 生成scan指令
            // 标识符为函数体中的变量赋值，将栈顶值存入
            if (curTable.isDeclared(id)) {
                if (curTable.isConstant(id))
                    throw new CompilationError(position,ErrorCode.ErrAssignToConstant);
                else {
                    if (curTable.isUninitializedVariable(id))
                        curTable.moveUninitializedToVars(id);
                    lastInstuctionIndex = instructionBody.addInstruction("loada",0,curTable.getIndex(id));
                    lastInstuctionIndex = instructionBody.addInstruction("iscan");
                    lastInstuctionIndex = instructionBody.addInstruction("istore");
                }
            }
            // 在globalTable中
            else if (globalTable.isDeclared(id)) {
                if (globalTable.isDeclared(id))
                    throw new CompilationError(position,ErrorCode.ErrAssignToConstant);
                else {
                    if (globalTable.isUninitializedVariable(id))
                        globalTable.moveUninitializedToVars(id);
                    lastInstuctionIndex = instructionBody.addInstruction("loada",1,globalTable.getIndex(id));
                    lastInstuctionIndex = instructionBody.addInstruction("iscan");
                    lastInstuctionIndex = instructionBody.addInstruction("istore");
                }
            }
            else
                throw new CompilationError(position,ErrorCode.ErrNotDeclared);


        }catch (CompilationError err) {
            throw err;
        }

        return;
    }

    /**
     * <print-statement> ::= 'print' '(' [<printable-list>] ')' ';'
     * <printable-list>  ::= <printable> {',' <printable>}
     * <printable> ::= <expression>
     * @throws CompilationError
     */
    void printStatement() throws CompilationError {
        Token next;
        int expressions = 0;
        try {
            /**
             * 'print' '('
             */
            next = nextToken();
            if (next.getType() != TokenType.PRINT) {
                unreadToken();
                throw new CompilationError(position,ErrorCode.ErrUnexpectedToken);
            }
            next = nextToken();
            if (next.getType() != TokenType.LEFT_BRACKET) {
                unreadToken();
                throw new CompilationError(position,ErrorCode.ErrNoBracket);
            }

            if (intoExpression()) {
                expression();
                expressions ++;
            }

            while (true) {
                next = nextToken();
                if (next.getType() != TokenType.COMMA) {
                    unreadToken();
                    break;
                }
                expressions ++;
                expression();
            }

            next = nextToken();
            if (next.getType() != TokenType.RIGHT_BRACKET) {
                unreadToken();
                throw new CompilationError(position,ErrorCode.ErrNoBracket);
            }
            next = nextToken();
            if (next.getType() != TokenType.SEMICOLON) {
                unreadToken();
                throw new CompilationError(position,ErrorCode.ErrNoSemicolon);
            }

            for (int i=0;i<expressions;i++) {
                lastInstuctionIndex = instructionBody.addInstruction("iprint");
            }

        }catch (CompilationError err) {
            throw err;
        }
        return;
    }

    /**
     * <assignment-expression> ::= <identifier><assignment-operator><expression>
     * <assignment-operator>   ::= '='
     * @throws CompilationError
     */
    void assignmentStatement() throws CompilationError {
        TokenType valueType;
        Token next;
        try {
            /**
             * <assignment-expression> ::= <identifier><assignment-operator><expression>
             */
            next = nextToken();
            if (next.getType() != TokenType.IDENTIFIER) {
                unreadToken();
                throw new CompilationError(position,ErrorCode.ErrUnexpectedToken);
            }
            Token id = next;
            // 在curTable中查找
            if (curTable.isDeclared(id)) {
                if (curTable.isConstant(id))
                    throw new CompilationError(position,ErrorCode.ErrAssignToConstant);
                else if (curTable.getType(id) == TokenType.VOID)
                    throw new CompilationError(position,ErrorCode.ErrInvalidVoid);
                else
                    // 加载地址
                    lastInstuctionIndex = instructionBody.addInstruction("loada",0,curTable.getIndex(id));
            }
            // 在globalTable中查找
            else if (globalTable.isDeclared(id)) {
                if (globalTable.isConstant(id))
                    throw new CompilationError(position,ErrorCode.ErrAssignToConstant);
                else if (globalTable.getType(id) == TokenType.VOID)
                    throw new CompilationError(position,ErrorCode.ErrInvalidVoid);
                else
                    // 加载地址
                    lastInstuctionIndex = instructionBody.addInstruction("loada",1,globalTable.getIndex(id));
            }
            else
                throw new CompilationError(position,ErrorCode.ErrNotDeclared);

            next = nextToken();
            if (next.getType() != TokenType.ASSIGNMENT_SIGN) {
                throw new CompilationError(position,ErrorCode.ErrInvalidAssignment);
            }
            // 入栈一个值
            valueType = expression();

            // 为函数体中的变量赋值，将栈顶值存入
            if (curTable.isDeclared(id)) {
                if (curTable.isUninitializedVariable(id))
                    curTable.moveUninitializedToVars(id);
                lastInstuctionIndex = instructionBody.addInstruction("istore");
                if (curTable.getType(id) != valueType)
                    throw new CompilationError(position,ErrorCode.ErrFailedExpression);
            }
            // 在globalTable中
            else if (globalTable.isDeclared(id)) {
                if (globalTable.isUninitializedVariable(id))
                    globalTable.moveUninitializedToVars(id);
                lastInstuctionIndex = instructionBody.addInstruction("istore");
                if (globalTable.getType(id) != valueType)
                    throw new CompilationError(position,ErrorCode.ErrFailedExpression);
            }

        }catch (CompilationError err) {
            throw err;
        }
        return;
    }

    /**
     * <function-call>   ::= <identifier> '(' [<expression-list>] ')’
     * <expression-list> ::= <expression>{','<expression>}
     * 语义：函数调用的传参数量以及每一个参数的数据类型（不考虑const），都必须和函数声明中的完全一致
     * @throws CompilationError
     */
    TokenType functionCall() throws CompilationError {
        TokenType valueType;
        Token next;
        try {
            /**
             * <identifier> '('
             */
            next = nextToken();
            if (next.getType() != TokenType.IDENTIFIER) {
                unreadToken();
                throw new CompilationError(position,ErrorCode.ErrUnexpectedToken);
            }
            Token funcName = next;
            valueType = assembly.findFuncType(funcName);


            next = nextToken();
            if (next.getType() != TokenType.LEFT_BRACKET) {
                unreadToken();
                throw new CompilationError(position,ErrorCode.ErrNoBracket);
            }

            /**
             * [<expression-list>]
             * <expression-list> ::= <expression>{','<expression>}
             */
            if (intoExpression())
                expression();
            while (true) {
                next = nextToken();
                if (next.getType() != TokenType.COMMA) {
                    unreadToken();
                    break;
                }
                expression();
            }

            /**
             * ')’
             */
            next = nextToken();
            if (next.getType() != TokenType.RIGHT_BRACKET) {
                unreadToken();
                throw new CompilationError(position,ErrorCode.ErrNoBracket);
            }

            //生成call指令
            int index = assembly.findFunc(funcName);
            if ( index == -1)
                throw new CompilationError(position,ErrorCode.ErrUndefinedFunction);
            else {
                lastInstuctionIndex = instructionBody.addInstruction("call",index);
            }

        }catch (CompilationError err) {
            throw err;
        }
        return valueType;
    }

    /**
     * <condition> ::= <expression>[<relational-operator><expression>]
     * <relational-operator>   ::= '<' | '<=' | '>' | '>=' | '!=' | '=='
     * @throws CompilationError
     */
    String condition() throws CompilationError {
        TokenType valueType;
        String instruction = "jne";
        Token next;
        try {
            valueType = expression();
            next = nextToken();
            // 如果没有
            if (!isRelational(next)) {
                unreadToken();
                if (valueType != TokenType.VOID) {
                    instruction = "jne";
                }
            }
            else {
                TokenType cmp = next.getType();
                if (valueType != expression())
                    throw new CompilationError(position,ErrorCode.ErrFailedExpression);
                // 此处指令全部取非
                switch (cmp) {
                    case LESSTHAN_SIGN: {
                        instruction = "jge";
                        lastInstuctionIndex = instructionBody.addInstruction("icmp");
                        break;
                    }
                    case LESSTHANEQUAL_SIGN: {
                        instruction = "jg";
                        lastInstuctionIndex = instructionBody.addInstruction("icmp");
                        break;
                    }
                    case GREATERTHAN_SIGN: {
                        instruction = "jle";
                        lastInstuctionIndex = instructionBody.addInstruction("icmp");
                        break;
                    }
                    case GREATERTHANEQUAL_SIGN: {
                        instruction = "jl";
                        lastInstuctionIndex = instructionBody.addInstruction("icmp");
                        break;
                    }
                    case EQUAL_SIGN: {
                        instruction = "jne";
                        lastInstuctionIndex = instructionBody.addInstruction("icmp");
                        break;
                    }
                    case NOTEQUAL_SIGN: {
                        instruction = "je";
                        lastInstuctionIndex = instructionBody.addInstruction("icmp");
                        break;
                    }
                }
            }
        }catch (CompilationError err) {
            throw err;
        }
        return instruction;
    }

    /**
     * <expression> ::= <additive-expression>
     * <additive-expression> ::= <multiplicative-expression>{<additive-operator><multiplicative-expression>}
     * <additive-operator>       ::= '+' | '-'
     * first = '+' | '-' | '(' | id | unint | hex | floating
     * @throws CompilationError
     */
    TokenType expression() throws CompilationError {
        TokenType valueType;
        Token next;
        try {
            /**
             * <multiplicative-expression>
             */
            valueType = multiplicativeExpression();
            /**
             * {<additive-operator><multiplicative-expression>}
             */
            while (true) {
                next = nextToken();
                TokenType op = next.getType();
                if (op != TokenType.PLUS_SIGN && op != TokenType.MINUS_SIGN) {
                    unreadToken();
                    break;
                }
                if (valueType != multiplicativeExpression())
                    throw new CompilationError(position,ErrorCode.ErrFailedExpression);

                if (op == TokenType.PLUS_SIGN)
                    lastInstuctionIndex = instructionBody.addInstruction("iadd");
                else if (op == TokenType.MINUS_SIGN)
                    lastInstuctionIndex = instructionBody.addInstruction("isub");
            }
        }catch (CompilationError err) {
            throw err;
        }
        return valueType;
    }

    /**
     * <multiplicative-expression> ::= <unary-expression>{<multiplicative-operator><unary-expression>}
     * <multiplicative-operator> ::= '*' | '/'
     * @throws CompilationError
     */
    TokenType multiplicativeExpression() throws CompilationError {
        TokenType valueType;
        Token next;
        try {
            /**
             * <unary-expression>
             */
            valueType = unaryExpression();

            /**
             * {<multiplicative-operator><unary-expression>}
             */
            while (true) {
                next = nextToken();
                TokenType op = next.getType();
                if (op != TokenType.MULTIPLICATION_SIGN && op != TokenType.DIVISION_SIGN) {
                    unreadToken();
                    break;
                }
                if (valueType != unaryExpression())
                    throw new CompilationError(position,ErrorCode.ErrFailedExpression);

                if (op == TokenType.MULTIPLICATION_SIGN)
                    lastInstuctionIndex = instructionBody.addInstruction("imul");
                else if (op == TokenType.DIVISION_SIGN)
                    lastInstuctionIndex = instructionBody.addInstruction("idiv");
            }

        }catch (CompilationError err) {
            throw err;
        }
        return valueType;
    }

    /**
     * <unary-expression> ::= [<unary-operator>]<primary-expression>
     * <unary-operator>   ::= '+' | '-'
     * <primary-expression> ::= '('<expression>')'  |<identifier> |<integer-literal> |<function-call>
     * @throws CompilationError
     */
    TokenType unaryExpression() throws CompilationError {
        TokenType valueType = TokenType.VOID;
        Token next;
        int prefix = 1;
        try {
            /**
             * [<unary-operator>]
             */
            next = nextToken();
            if (next.getType() == TokenType.PLUS_SIGN)
                prefix = 1;
            else if (next.getType() == TokenType.MINUS_SIGN)
                prefix = -1;
            else
                unreadToken();

            /**
             * '('<expression>')'  |<integer-literal> |<identifier> |<function-call>
             */
            next = nextToken();
            switch (next.getType()) {
                case LEFT_BRACKET: {
                    valueType = expression();
                    next = nextToken();
                    if (next.getType() != TokenType.RIGHT_BRACKET) {
                        unreadToken();
                        throw new CompilationError(position,ErrorCode.ErrNoBracket);
                    }
                    break;
                }
                case IDENTIFIER: {
                    // 预读 '('
                    next = nextToken();
                    // 回溯预读的'('和id
                    unreadToken();
                    unreadToken();
                    if (next.getType() == TokenType.LEFT_BRACKET) {
                        valueType = functionCall();
                        break;
                    }
                    // id只能为变量
                    next = nextToken();
                    // 在curTable中查找
                    if (curTable.isDeclared(next)) {
                        if (curTable.isUninitializedVariable(next))
                            throw new CompilationError(position,ErrorCode.ErrNotInitialized);
                        //else if (curTable.isParam(next));
                        else {
                            //System.out.println("curTable变量入栈");
                            lastInstuctionIndex = instructionBody.addInstruction("loada",0,curTable.getIndex(next));
                            lastInstuctionIndex = instructionBody.addInstruction("iload");
                        }
                        valueType = curTable.getType(next);
                    }// 在globalTable中查找
                    else if (globalTable.isDeclared(next)) {
                        if (globalTable.isUninitializedVariable(next))
                            throw new CompilationError(position,ErrorCode.ErrNotInitialized);
                        //else if (globalTable.isParam(next));
                        else {
                            //System.out.println("globalTable变量入栈");
                            lastInstuctionIndex = instructionBody.addInstruction("loada",1,globalTable.getIndex(next));
                            lastInstuctionIndex = instructionBody.addInstruction("iload");
                        }
                        valueType = globalTable.getType(next);
                    }
                    else
                        throw new CompilationError(position,ErrorCode.ErrNotDeclared);
                    break;
                }
                case UNSIGNED_INTEGER: {
                    //System.out.println("整型字面量入栈");
                    lastInstuctionIndex = instructionBody.addInstruction("ipush",next.getIntValue());
                    valueType = TokenType.INT;
                    break;
                }
                case HEXADECIMAL_INTEGER: {
                    //System.out.println("整型字面量入栈");
                    lastInstuctionIndex = instructionBody.addInstruction("ipush",next.getIntValue());
                    valueType = TokenType.INT;
                    break;
                }
                case FLOATING_VALUE: {
                    break;
                }
                default: {
                    throw new CompilationError(position,ErrorCode.ErrUnexpectedToken);
                }
            }

            if (prefix == -1)
                lastInstuctionIndex = instructionBody.addInstruction("ineg");

        }catch (CompilationError err) {
            throw err;
        }
        return valueType;
    }

    /**
     * token属于int|char|double|void
     * @param token
     * @return
     */
    boolean isType(Token token) {
        TokenType first = token.getType();
        return first == TokenType.INT
                || first == TokenType.VOID
                || first == TokenType.DOUBLE
                || first == TokenType.CHAR;
    }

    /**
     * token属于'<' | '<=' | '>' | '>=' | '!=' | '=='
     * @param token
     * @return
     */
    boolean isRelational(Token token) {
        TokenType first = token.getType();
        return first == TokenType.LESSTHAN_SIGN
                || first == TokenType.LESSTHANEQUAL_SIGN
                || first == TokenType.GREATERTHAN_SIGN
                || first == TokenType.GREATERTHANEQUAL_SIGN
                || first == TokenType.EQUAL_SIGN
                || first == TokenType.NOTEQUAL_SIGN;
    }

    /**
     * 判断是否是变量声明
     * @return
     * @throws CompilationError
     */
    boolean intoVar() throws CompilationError {
        Token next;
        try {
            if (index != tokens.size()) {
                /**
                 * const
                 */
                next = nextToken();
                if (next.getType() == TokenType.CONST) {
                    unreadToken();
                    return true;
                }
                /**
                 * type
                 */
                else if (isType(next)) {
                    next = nextToken();
                    /**
                     * identifier
                     */
                    if (next.getType() != TokenType.IDENTIFIER) {
                        unreadToken();
                        unreadToken();
                        return false;
                    } else {
                        /**
                         *  '=' | ',' | ';'
                         */
                        next = nextToken();
                        TokenType type = next.getType();
                        if (type != TokenType.ASSIGNMENT_SIGN && type != TokenType.COMMA && type != TokenType.SEMICOLON) {
                            unreadToken();
                            unreadToken();
                            unreadToken();
                            return false;
                        } else {
                            unreadToken();
                            unreadToken();
                            unreadToken();
                            return true;
                        }
                    }
                } else {
                    unreadToken();
                    return false;
                }
            }
            else
                return false;

        }catch (CompilationError err) {
            if (err.getErrorCode() == ErrorCode.ErrNoMoreToken)
                throw new CompilationError(position,ErrorCode.ErrInvalidEnd);
            throw err;
        }
    }

    /**
     * 判断是否是函数声明
     * @return
     * @throws CompilationError
     */
    boolean intoFunc() throws CompilationError {
        Token next;
        try {
            if (index != tokens.size()) {
                /**
                 * type
                 */
                next = nextToken();
                if (isType(next)) {
                    /**
                     * identifier
                     */
                    next = nextToken();
                    if (next.getType() != TokenType.IDENTIFIER) {
                        unreadToken();
                        unreadToken();
                        return false;
                    }
                    else {
                        /**
                         *  '('
                         */
                        next = nextToken();
                        if (next.getType() != TokenType.LEFT_BRACKET) {
                            unreadToken();
                            unreadToken();
                            unreadToken();
                            return false;
                        }
                        else {
                            unreadToken();
                            unreadToken();
                            unreadToken();
                            return true;
                        }
                    }
                }
                else {
                    unreadToken();
                    return false;
                }
            }
            else
                return false;
        }catch (CompilationError err) {
            if (err.getErrorCode() == ErrorCode.ErrNoMoreToken)
                throw new CompilationError(position,ErrorCode.ErrInvalidEnd);
            throw err;
        }
    }

    /**
     * 用于判断是否为statement
     * @return
     * @throws CompilationError
     */
    boolean intoStatement() throws CompilationError {
        Token next;
        try {
            if (index != tokens.size()) {
                next = nextToken();
                unreadToken();
                TokenType type = next.getType();
                if (
                        type == TokenType.LEFT_BRACE
                                || type == TokenType.IF
                                || type == TokenType.WHILE
                                || type == TokenType.RETURN
                                || type == TokenType.PRINT
                                || type == TokenType.SCAN
                                || type == TokenType.IDENTIFIER
                )
                    return true;
                else
                    return false;
            }
            else
                return false;

        }catch (CompilationError err) {
            if (err.getErrorCode() == ErrorCode.ErrNoMoreToken)
                throw new CompilationError(position,ErrorCode.ErrInvalidEnd);
            throw err;
        }
    }

    /**
     * first = '+' | '-' | '(' | id | unint | hex | floating
     * @return
     * @throws CompilationError
     */
    boolean intoExpression() throws CompilationError {
        Token next;
        try {
            if (index != tokens.size()) {
                next = nextToken();
                unreadToken();
                TokenType type = next.getType();
                if (
                        type == TokenType.PLUS_SIGN
                                || type == TokenType.MINUS_SIGN
                                || type == TokenType.LEFT_BRACKET
                                || type == TokenType.IDENTIFIER
                                || type == TokenType.UNSIGNED_INTEGER
                                || type == TokenType.HEXADECIMAL_INTEGER
                                || type == TokenType.FLOATING_VALUE
                )
                    return true;
                else
                    return false;
            }
            else
                return false;

        }catch (CompilationError err) {
            if (err.getErrorCode() == ErrorCode.ErrNoMoreToken)
                throw new CompilationError(position,ErrorCode.ErrInvalidEnd);
            throw err;
        }
    }

    /**
     * 返回下一个token
     * @return
     * @throws CompilationError
     */
    Token nextToken() throws CompilationError {
        if (index == tokens.size())
            throw new CompilationError(position, ErrorCode.ErrNoMoreToken);
        /**
         * 考虑到 tokens[0...offset-1] 已经被分析过了
         * 所以我们选择 tokens[0...offset-1] 的 EndPos 作为当前位置
         */
        position = tokens.get(index).getEndPos();
        return tokens.get(index++);
    }

    /**
     * 回溯一个token
     */
    void unreadToken() {
        if (index == 0)
            Main.DieAndPrint("analyser unreads token from the begining.");
        position = tokens.get(index-1).getEndPos();
        index--;
    }

}

