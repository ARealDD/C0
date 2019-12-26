package analyser;

import tokenizer.Token;
import tokenizer.TokenType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Assembly {
    ArrayList<Constant> constants = new ArrayList<>();
    FunctionBody starts = new FunctionBody(TokenType.VOID);
    ArrayList<FunctionBody> functionBodies = new ArrayList<>();
    ArrayList<Function> functions = new ArrayList<>();
    Map funcTypes = new HashMap<String,TokenType>();

    private int constantsIndex = 0;
    private int functionsIndex = 0;
    private int startIndex = 0;

    public int getConstantsIndex() {
        return constantsIndex;
    }

    public int getFunctionsIndex() {
        return functionsIndex;
    }

    public FunctionBody getStarts() {
        return starts;
    }

    public ArrayList<Constant> getConstans() {
        return constants;
    }

    public ArrayList<Function> getFunctions() {
        return functions;
    }

    public int addConstant(char type, String value) {
        constants.add(new Constant(constantsIndex,type,value));
        constantsIndex++;
        return constantsIndex - 1;
    }

    public FunctionBody newFunction(TokenType returnValue) {
        FunctionBody functionBody = new FunctionBody(returnValue);
        functionBodies.add(functionBody);
        funcTypes.put(functionsIndex,returnValue);
        functionsIndex++;
        return functionBody;
    }

    public int findFunc(Token tk) {
        String s = tk.getValue();
        int nameIndex=-1,index=-1;
        for (Constant constant:constants) {
            if (constant.stringValue.equals(s))
                nameIndex = constant.index;
        }
        for (Function function:functions) {
            if (function.getNameIndex() == nameIndex)
                index = function.getIndex();
        }
        return index;
    }

    /**
     * 传入函数名，返回函数返回值类型
     * @param tk
     * @return
     */
    public TokenType findFuncType(Token tk) {
        return (TokenType)funcTypes.get(findFunc(tk));
    }

    public void addFunction(int index, int nameIndex, int paramsSize,int level) {
        functions.add(new Function(index,nameIndex,paramsSize,level));
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(".constants:\n");
        for (Constant constant:constants) {
            sb.append(constant.toString());
        }
        sb.append(".start:\n");
        sb.append(starts.toString());
        sb.append(".functions:\n");
        for (Function function:functions) {
            sb.append(function.toString());
        }
        for (int i=0;i<functionBodies.size();i++) {
            sb.append(".F"+i+":\n");
            sb.append(functionBodies.get(i).toString());
        }
        return sb.toString();
    }
}
