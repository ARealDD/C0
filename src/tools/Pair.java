package tools;

public class Pair<t,n> {
    private t first;
    private n second;

    public Pair (t first,n second) {
        this.first = first;
        this.second = second;
    }

    public t getFirst() {
        return first;
    }
    public n getSecond() {
        return second;
    }

    @Override
    public String toString() {
        return ""+this.first+"  "+this.second;
    }
}
