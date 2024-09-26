package miniJava.SyntacticAnalyzer;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import miniJava.ErrorReporter;

public class Scanner {
	private InputStream _in;
	private ErrorReporter _errors;
	private StringBuilder _currentText;
	private char _currentChar;
	private boolean eot = false;
	private int rowNumber = 1;
	private int columnNumber = 0;

	private static HashMap<String, TokenType> keywordMap;
	
	static {
		keywordMap = new HashMap<String, TokenType>();
	    keywordMap.put("class", TokenType.CLASS);
	    keywordMap.put("public", TokenType.PUBLIC);
	    keywordMap.put("private", TokenType.PRIVATE);
	    keywordMap.put("void", TokenType.VOID);
	    keywordMap.put("static", TokenType.STATIC);
	    keywordMap.put("int", TokenType.INT);
	    keywordMap.put("boolean", TokenType.BOOLEAN);
	    keywordMap.put("this", TokenType.THIS);
	    keywordMap.put("return", TokenType.RETURN);
	    keywordMap.put("if", TokenType.IF);
	    keywordMap.put("else", TokenType.ELSE);
	    keywordMap.put("while", TokenType.WHILE);
	    keywordMap.put("true", TokenType.TRUE);
	    keywordMap.put("false", TokenType.FALSE);
	    keywordMap.put("new", TokenType.NEW);
	    keywordMap.put("null", TokenType.NULL);
	    //keywordMap.put("=", TokenType.ASSIGNMENT);
		// not sure we need this 
	}

	public Scanner( InputStream in, ErrorReporter errors ) {
		this._in = in;
		this._errors = errors;
		nextChar();
	}
	
	public Token scan() {
		this._currentText = new StringBuilder();
		skipSeparators();
		TokenType kind = scanToken();
		return makeToken(kind);

		// TODO: This function should check the current char to determine what the token could be.
		// must be a token if weve skipped seperators
		
		// TODO: What happens if there are no more tokens?
		// how do we know if there are no more tokens? 
			// eot?  return EOT token? 
		
		// TODO: Determine what the token is. For example, if it is a number
		//  keep calling takeIt() until _currentChar is not a number. Then
		//  create the token via makeToken(TokenType.IntegerLiteral) and return it.
	}
	
	private void takeIt() {
		_currentText.append(_currentChar);
		nextChar();
	}
	
	private void skipIt() {
		nextChar();
	}

	private void skipSeparators() {
		// could check two characters??
		switch (_currentChar) {
		case ' ': case '\t': case '\r': case '\n':
			if (_currentChar == '\n') {
				this.rowNumber++;
				this.columnNumber = 0;
			}
			skipIt();
			skipSeparators();
			break;
		case '/':
			// if next character isnt a forward slash or or * its not a comment, restore old byyes
			_in.mark(1);
			skipIt();
			if (_currentChar == '*') {
				skipIt();
				char previousChar = _currentChar;
				skipIt();
				while (!(eot || (previousChar == '*' && _currentChar == '/'))) {
					if (_currentChar == '\n') {
						this.rowNumber++;
						this.columnNumber = 0;
					}
					previousChar = _currentChar;
					skipIt();
				}
				if (eot) {
					scanError("Comment was never terminated");
				}
				skipIt();
			} else if (_currentChar == '/') {
				skipIt();
				while (!(eot || _currentChar == '\n' || _currentChar == '\r')) {
					skipIt();
				}
				if (_currentChar == '\n') {
					this.rowNumber++;
					this.columnNumber = 0;
				}
				skipIt();
			} else {
				_currentChar = '/';
				this.columnNumber--;
				try {
					_in.reset();
					eot = false;
				} catch (IOException e) {
					scanError(e.toString());
				}
				
				break;
			}
			skipSeparators();
			break;
		default:
			break;
		}
	}	
	
	private void nextChar() {
		try {
			int c = _in.read();
			if (c > 128) {
				scanError("NonASCII input!");
				// handling for non ASCII input. not sure if this is correct
			}
			
			// result of (char) -1 ?
			if (c == -1) {
				eot = true;
			}

			columnNumber++;

			_currentChar = (char)c;
			
		} catch( IOException e ) {
			scanError("I/O Exception!");
		}
	}

	public TokenType scanToken() {

		// some inconsistencies with marking EOT when using mark
		if (eot)
			return(TokenType.EOT); 

		// scan Token
		switch (_currentChar) {
		case 'a': case 'b': case 'c': case 'd': case 'e': case 'f': case 'g': case 'h': case 'i':case 'j':
		case 'k': case 'l': case 'm': case 'n': case 'o': case 'p': case 'q': case 'r': case 's': case 't':
		case 'u': case 'v': case 'w': case 'x': case 'y': case 'z':
		case 'A': case 'B': case 'C': case 'D': case 'E': case 'F': case 'G': case 'H': case 'I':case 'J':
		case 'K': case 'L': case 'M': case 'N': case 'O': case 'P': case 'Q': case 'R': case 'S': case 'T':
		case 'U': case 'V': case 'W': case 'X': case 'Y': case 'Z':
			takeIt();
			while (Character.isLetter(_currentChar) || 
					Character.isDigit(_currentChar) || 
					_currentChar == '_') {
						takeIt();
					}
			if (keywordMap.containsKey(this._currentText.toString())) {
				return keywordMap.get(this._currentText.toString());
			} else {
				return TokenType.IDENTIFIER;
			}
		case '+': case '-': case '*': case '/':
	//		if (_currentChar == '/') {
	//			System.out.println(); // was i trying to handle something here? 
	//		}
			takeIt();
			return(TokenType.OPERATOR);
		case '&': case '|':
			char prevChar = _currentChar;
			takeIt();
			if (_currentChar == prevChar) {
				takeIt();
				return (TokenType.OPERATOR);
			} else {
				return (TokenType.ERROR);
			}
		case '>': case '<': case '!':
			takeIt();
			if (_currentChar == '=') {
				takeIt();
				return (TokenType.OPERATOR);
			} else {
				return (TokenType.OPERATOR);
			}
		case '=':
			takeIt();
			if (_currentChar == '=') {
				takeIt();
				return (TokenType.OPERATOR);
			} else {
				return TokenType.ASSIGNMENT;
			}
		case '0': case '1': case '2': case '3': case '4': case '5':
		case '6': case '7': case '8': case  '9':
			takeIt();
			while (Character.isDigit(_currentChar)) takeIt();
			return TokenType.NUMBER;
		case '.':
			takeIt();
			return (TokenType.DOT);
		case '(': 
			takeIt();
			return(TokenType.LPAREN);

		case ')':
			takeIt();
			return(TokenType.RPAREN);
			
		case '{': 
			takeIt();
			return(TokenType.LBRACKET);

		case '}':
			takeIt();
			return(TokenType.RBRACKET);
		case '[' :
			takeIt();
			return(TokenType.SQLBRACKET);
		case ']':
			takeIt();
			return(TokenType.SQRBRACKET);
		case ';':
			takeIt();
			return(TokenType.SEMICOLON);
		case ',':
			takeIt();
			return(TokenType.COMMA);
		
		default:
			scanError("Unrecognized character '" + _currentChar + "' in input");
			return(TokenType.ERROR);
		}
	}
	
	private Token makeToken(TokenType tokenType) {
		// can do a simple mathematical expression to determine wher ethe token starts based on the spelling
		// can do a refactor such that single token characters span of syntax..
		String spelling = this._currentText.toString();
		return new Token(tokenType, spelling, this.rowNumber, this.columnNumber - spelling.length());
	}

	private void scanError(String m) {
		// i need to provde line and colum numbers. how? 
		_errors.reportError(this.rowNumber,this.columnNumber, "Scan Error:  " + m);
	}
}
