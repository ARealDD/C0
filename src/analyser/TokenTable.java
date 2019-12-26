package analyser;

import tokenizer.Token;
import tokenizer.TokenType;

import java.util.HashMap;
import java.util.Map;
import run.Main;

public class TokenTable {
    int nextTokenIndex = 0;
    Map vars = new HashMap<String, Integer>();
    Map consts = new HashMap<String,Integer>();
    Map uninitializedVars = new HashMap<String,Integer>();
    Map params = new HashMap<String,Integer>();
    Map types = new HashMap<String,TokenType>();


    void add(Token token,Map mp,TokenType type) {
        if (token.getType() != TokenType.IDENTIFIER)
            Main.DieAndPrint("only identifier can be added to the table.");
        mp.put(token.getValue(),nextTokenIndex);
        types.put(token.getValue(),type);
        nextTokenIndex++;
    }

    public void addVariable(Token tk,TokenType type) {
        add(tk, vars,type);
    }

    public void addConstant(Token tk,TokenType type) {
        add(tk, consts,type);
    }

    public void addUninitializedVariable(Token tk,TokenType type) {
        add(tk, uninitializedVars,type);
    }

    public void addParams(Token tk,TokenType type) { add(tk,params,type); }

    public boolean isDeclared(Token s) {
        return isConstant(s) || isUninitializedVariable(s) || isInitializedVariable(s) || isParam(s) ;
    }

    public boolean isUninitializedVariable(Token tk) {
        String s = tk.getValue();
        return uninitializedVars.containsKey(s);
    }

    public boolean isInitializedVariable(Token tk) {
        String s = tk.getValue();
        return vars.containsKey(s);
    }

    public boolean isConstant(Token tk) {
        String s = tk.getValue();
        return consts.containsKey(s);
    }

    public boolean isParam(Token tk) {
        String s = tk.getValue();
        return params.containsKey(s);
    }

    public TokenType getType(Token tk) {
        String s = tk.getValue();
        return (TokenType) types.get(s);
    }

    public void moveUninitializedToVars(Token tk) {
        String s = tk.getValue();
        vars.put(s,uninitializedVars.get(s));
        uninitializedVars.remove(s);
    }

    public int getIndex(Token tk) {
        String s = tk.getValue();
        if (uninitializedVars.containsKey(s))
            return (int)uninitializedVars.get(s) - 1;
        else if (vars.containsKey(s))
            return (int)vars.get(s) - 1;
        else if (consts.containsKey(s))
            return (int)consts.get(s) - 1;
        else
            return (int)params.get(s) - 1;
    }
}
