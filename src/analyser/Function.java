package analyser;

public class Function {
    private int index;
    private int nameIndex;
    private int paramsSize;
    private int level;

    public Function(int index,int nameIndex,int paramsSize,int level) {
        this.index = index;
        this.nameIndex = nameIndex;
        this.paramsSize = paramsSize;
        this.level = level;
    }

    public int getIndex() {
        return index;
    }

    public int getNameIndex() {
        return nameIndex;
    }

    public int getParamsSize() {
        return paramsSize;
    }

    public int getLevel() {
        return level;
    }

    @Override
    public String toString() {
        return index + " " + nameIndex + " " + paramsSize + " " + level + "\n";
    }
}
