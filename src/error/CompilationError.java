package error;
import tools.*;

public class CompilationError extends RuntimeException{

    private Pair<Integer,Integer> position;
    private ErrorCode errorCode;

    /**
     * 无参构造默认unspecified
     */
    public CompilationError() {
        super(ErrorCode.Unspecified.getDescription());
        errorCode = ErrorCode.Unspecified;
    }

    /**
     * 通用构造器
     * @param line
     * @param column
     * @param err
     */
    public CompilationError(int line,int column,ErrorCode err) {
        super("Line: " + line + " " +
                "Column: " + column + " " +
                "Error: " + err.getDescription());
        this.position = new Pair<>(new Integer(line),new Integer(column));
        this.errorCode = err;
    }

    /**
     * 通用构造器
     * @param pos
     * @param err
     */
    public CompilationError(Pair<Integer,Integer> pos,ErrorCode err) {
        super("Line: " + pos.getFirst() + " " +
                "Column: " + pos.getSecond().toString() + " " +
                "Error: " + err.getDescription());
        this.position = pos;
        this.errorCode = err;
    }

    public Pair<Integer, Integer> getPosition() {
        return position;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

}
