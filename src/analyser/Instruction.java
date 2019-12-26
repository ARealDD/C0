package analyser;

public class Instruction {
    int index;
    String opcode;
    Integer operand1 = null;
    Integer operand2 = null;

    public Instruction(int index,String opcode) {
        this.index = index;
        this.opcode = opcode;
    }
    public Instruction(int index,String opcode,int operand1) {
        this.index = index;
        this.opcode = opcode;
        this.operand1 = operand1;
    }
    public Instruction(int index,String opcode,int operand1,int operand2) {
        this.index = index;
        this.opcode = opcode;
        this.operand1 = operand1;
        this.operand2 = operand2;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(index);
        sb.append(' ');
        sb.append(opcode);
        sb.append(' ');
        if (operand1 != null) {
            sb.append(operand1);
        }
        if (operand2 != null) {
            sb.append(',');
            sb.append(operand2);
            sb.append(' ');
        }
        sb.append('\n');
        return sb.toString();
    }
}
