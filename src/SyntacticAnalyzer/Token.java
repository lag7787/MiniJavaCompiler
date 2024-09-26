package miniJava.SyntacticAnalyzer;

public class Token {
	private TokenType _type;
	private String _text;
	public int lineNumber;
	public int columnNumber;
	private SourcePosition sourcePosition;
	
	public Token(TokenType type, String text, int lineNumber, int columnNumber) {
		this._type = type;
		this._text = text;
		this.sourcePosition = new SourcePosition(lineNumber, columnNumber);
	}
	
	public TokenType getTokenType() {
		return this._type;
	}
	
	public String getTokenText() {
		return this._text;
	}

	public SourcePosition getTokenPosition() {
		return this.sourcePosition;
	}
}
