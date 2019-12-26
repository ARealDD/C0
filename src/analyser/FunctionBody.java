package analyser;

import tokenizer.TokenType;

import java.util.ArrayList;

public class FunctionBody {

    TokenTable tokenTable = new TokenTable();
    ArrayList<Instruction> instructions = new ArrayList<>();
    TokenType returnValue;

    int instructionIndex = 0;

    public FunctionBody(TokenType returnValue) {
        this.returnValue = returnValue;
    }

    public TokenType getReturnValue() {
        return returnValue;
    }

    public int addInstruction(String opcode) {
        this.instructions.add(new Instruction(instructionIndex,opcode));
        instructionIndex++;
        return instructionIndex - 1;
    }
    public int addInstruction(String opcode,int operand1) {
        this.instructions.add(new Instruction(instructionIndex,opcode,operand1));
        instructionIndex++;
        return instructionIndex - 1;
    }
    public int addInstruction(String opcode,int operand1,int operand2) {
        this.instructions.add(new Instruction(instructionIndex,opcode,operand1,operand2));
        instructionIndex++;
        return instructionIndex - 1;
    }

    public void changeJumpOffset(int instructionIndex,int offset) {
        Instruction instruction = instructions.get(instructionIndex);
        instruction.operand1 = offset;
    }

    public TokenTable getTokenTable() {
        return tokenTable;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (Instruction instruction:instructions)
            sb.append(instruction.toString());
        return sb.toString();
    }
}
