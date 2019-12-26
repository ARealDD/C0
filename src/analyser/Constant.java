package analyser;

public class Constant {
    int index;
    char type;
    int intValue;
    double doubleValue;
    String stringValue;
    public Constant(int index,char type,int intValue) {
        this.index = index;
        this.type = type;
        this.intValue = intValue;
    }
    public Constant(int index,char type,double doubleValue) {
        this.index = index;
        this.type = type;
        this.doubleValue = doubleValue;
    }
    public Constant(int index,char type,String stringValue) {
        this.index = index;
        this.type = type;
        this.stringValue = stringValue;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        if (type == 'I')
            return index + " " + type + " " + intValue + "\n";
        else if (type == 'D')
            return index + " " + type + " " + doubleValue + "\n";
        else
            return index + " " + type + " \"" + stringValue + "\"\n";
    }
}
