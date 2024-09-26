package miniJava.SyntacticAnalyzer;

public class SourcePosition {

    private int lineNumber;
    private int columnNumber;
    private Token startToken;
    private Token endToken;

    public SourcePosition(int lineNumber,int columnNumber){
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    public SourcePosition(Token startToken, Token endToken) {
        this.startToken = startToken;
        this.endToken = endToken;
    }

    public int getLineNumber() {
        return this.lineNumber;
    }

    public int getColumnNumber() {
        return this.columnNumber;
    }

    public void setEndToken(Token token) {
        this.endToken = token;
    }



    @Override
    public String toString() {
        if (startToken != null && endToken != null) {
            return String
            .format("The current syntax spans lineNumber: %d columnNumber: %d to lineNumber: %d columnNumber %d. ",
                    startToken.lineNumber,
                    startToken.columnNumber,
                    endToken.lineNumber,
                    endToken.columnNumber
                    );
        } else {
            return String.format("lineNumber: %d column number: %s", this.lineNumber, this.columnNumber);
        }
    }
}
