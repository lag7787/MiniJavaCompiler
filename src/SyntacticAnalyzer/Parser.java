package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.*;

// check if token.getText -- may be bugs 

public class Parser {
	private Scanner _scanner;
	private ErrorReporter _errors;
	private Token _currentToken;
	private boolean trace = false;
	
	public Parser( Scanner scanner, ErrorReporter errors ) {
		this._scanner = scanner;
		this._errors = errors;
		this._currentToken = this._scanner.scan();
	}
	
	class SyntaxError extends Error {
		private static final long serialVersionUID = -6461942006097999362L;
	}
	
	public Package parse() {
		try {
			// The first thing we need to parse is the Program symbol
			return parseProgram();
		} catch( SyntaxError e ) {
			return null;
		}
	}
	
	// TODO: REVISE / REVIEW  SOURCE POSITION
	// Program ::= (ClassDeclaration)* eot
	private Package parseProgram() throws SyntaxError {
		ClassDeclList classDeclList = new ClassDeclList();
		Token startToken = this._currentToken;
		Token endToken = this._currentToken;

		while (this._currentToken.getTokenType() == TokenType.CLASS) {
			// would it be more accurate to have access to the last token in the class Decl?
			classDeclList.add(parseClassDeclaration());
			endToken = this._currentToken;
		}

		accept(TokenType.EOT);	
		return new Package(classDeclList, new SourcePosition(startToken, endToken));
	}
	
// ClassDeclaration ::= class identifier { (FieldDeclaration|MethodDeclaration)* }
/* 
	* ::= class id 
	{ 
		( 
			( public | private )?  (static)?  
				(
					Type id ( ; | L )  
					| void id L 
				)
		)* 
	} 
*/
	// TODO: FIX SOURCE POSITION
	private ClassDecl parseClassDeclaration() throws SyntaxError {
		String className;
		String declName;
		FieldDecl fieldDecl;
		boolean isPrivate;
		boolean isStatic;
		TypeDenoter type;
		MethodDeclList methodDeclList = new MethodDeclList();
		FieldDeclList fieldDeclList = new FieldDeclList();

		accept(TokenType.CLASS);
		className = this._currentToken.getTokenText();
		accept(TokenType.IDENTIFIER);
		accept(TokenType.LBRACKET);
		while (
				this._currentToken.getTokenType() == TokenType.PUBLIC ||
				this._currentToken.getTokenType() == TokenType.PRIVATE ||
				this._currentToken.getTokenType() == TokenType.STATIC ||
				this._currentToken.getTokenType() == TokenType.VOID ||
				this._currentToken.getTokenType() == TokenType.BOOLEAN ||
				this._currentToken.getTokenType() == TokenType.INT ||
				this._currentToken.getTokenType() == TokenType.IDENTIFIER
		) {

			/*
			 * is private will be tru iff the private token is present, otherwise it will be false
			 * regardless of wether or not the token is there this is the precedent will we maintain
			 */
			if (this._currentToken.getTokenType() == TokenType.PUBLIC ||
			 this._currentToken.getTokenType() == TokenType.PRIVATE) {
				if (this._currentToken.getTokenType() == TokenType.PRIVATE) {
					isPrivate = true;
				} else {
					isPrivate = false;
				}
				acceptIt();
			} else {
				isPrivate = false;
			}

			/*
			 * isStatic will be true iff the token is presetn, otherwise it will be false
			 */
			
			if (this._currentToken.getTokenType() == TokenType.STATIC) {
				isStatic = true;
				acceptIt();
			} else {
				isStatic = false;
			}
			
			/*
			 * convention is that tokens will have line and column numbers representing the first character in that token
			 * If a type directly represents a token, it will share this convention 
			 */

			if (this._currentToken.getTokenType() == TokenType.VOID) {
				/*
				 * in this branch, void only appears in method decls, so we know were going to add one to the list 
				 * Its also important to note that every method decl contains a field decl, so we create one before
				 */
				type = new BaseType(TypeKind.VOID, 
				new SourcePosition(this._currentToken.lineNumber, this._currentToken.columnNumber));
				acceptIt();
				declName = this._currentToken.getTokenText();
				accept(TokenType.IDENTIFIER);
				fieldDecl = new FieldDecl(isPrivate, isStatic, type, declName, 
				new SourcePosition(null, null));
				methodDeclList.add(parseL(fieldDecl)); // should return a fresh method decl object
			} else {
				type = parseType();
				declName = this._currentToken.getTokenText();
				accept(TokenType.IDENTIFIER);
				fieldDecl = new FieldDecl(isPrivate, isStatic, type, declName, new SourcePosition(null, this._currentToken));
				if (this._currentToken.getTokenType() == TokenType.SEMICOLON){
					acceptIt();
					fieldDeclList.add(fieldDecl);
				} else {
					methodDeclList.add(parseL(fieldDecl));
				}
			}
		}
			
		accept(TokenType.RBRACKET);
		return new ClassDecl(className, fieldDeclList, methodDeclList, null);
	}

	/*
	Statement ::=
	{Statement*}
	| ( int ('' | []) | boolean) id = Expression ;
	| id (
		| id = Expression ;
		| = Expression ;
		| [] id = Expression ;
		| [ Expression ] = Expression ;
		| (ArugmentList?) ;
		| .id (.id)* J
		)
	| this (.id)* J
	| return Expression? ; 
	| if ( Expression ) Statement ( else Statement ) ?
	| while ( Expression ) Statement
	*/

	private Statement parseStatement() throws SyntaxError {
		StatementList statementList = new StatementList();
		ExprList exprList = new ExprList();
		Statement rv;
		TypeDenoter t;
		String name;
		VarDecl vd;
		Expression e;
		Reference ref;
		switch(this._currentToken.getTokenType()) {
		case LBRACKET:
			acceptIt();
			while (
					this._currentToken.getTokenType() == TokenType.LBRACKET ||
					this._currentToken.getTokenType() == TokenType.INT ||
					this._currentToken.getTokenType() == TokenType.BOOLEAN ||
					this._currentToken.getTokenType() == TokenType.THIS ||
					this._currentToken.getTokenType() == TokenType.IDENTIFIER ||
					this._currentToken.getTokenType() == TokenType.IF ||
					this._currentToken.getTokenType() == TokenType.RETURN ||
					this._currentToken.getTokenType() == TokenType.WHILE
					) {
				statementList.add(parseStatement());
			}
			accept(TokenType.RBRACKET);
			return new BlockStmt(statementList, new SourcePosition(null, null));
		case INT: case BOOLEAN:
		//var declstmt
		// can i just make a call to parse type here? 
		//	if (this._currentToken.getTokenType() == TokenType.INT) {
		//		acceptIt();
		//		if (this._currentToken.getTokenType() == TokenType.SQLBRACKET) {
		//			acceptIt();
		//			accept(TokenType.SQRBRACKET);
		//		}
		//	} else acceptIt();
			t = parseType();
			name = this._currentToken.getTokenText();
			accept(TokenType.IDENTIFIER);
			vd = new VarDecl(t, name, new SourcePosition(null, null));
			accept(TokenType.ASSIGNMENT);
			e = parseE();
			accept(TokenType.SEMICOLON);
			return new VarDeclStmt(vd, e, new SourcePosition(null, null));
		case THIS:
			//assignStmt, call stmt, or IxStmt
			ref = new ThisRef(new SourcePosition(null, null));
			acceptIt();
			while (this._currentToken.getTokenType() == TokenType.DOT) {
				acceptIt();
				ref = new QualRef(ref, new Identifier(this._currentToken),new SourcePosition(null, null));
				accept(TokenType.IDENTIFIER);			
			}
			rv = parseJ(ref);
			accept(TokenType.SEMICOLON);
			return rv;
		case IDENTIFIER:
			// could be class type
			Token Idtoken = this._currentToken;
			acceptIt();
			switch(this._currentToken.getTokenType()) {
			case IDENTIFIER:
			//var dcl
				if (Idtoken.getTokenText().equals("String")) {
					t = new BaseType(TypeKind.UNSUPPORTED, new SourcePosition(null, null));
				} else {
					t = new ClassType(new Identifier(Idtoken), new SourcePosition(null, null));
				}
				name = this._currentToken.getTokenText();
				acceptIt();
				vd = new VarDecl(t, name, new SourcePosition(null, null));
				accept(TokenType.ASSIGNMENT);
				e = parseE();
				accept(TokenType.SEMICOLON);
				rv = new VarDeclStmt(vd, e, new SourcePosition(_currentToken, _currentToken));
				break;
			case ASSIGNMENT:
				ref = new IdRef(new Identifier(Idtoken), new SourcePosition(null, null));
				acceptIt();
				e = parseE();
				accept(TokenType.SEMICOLON);
				rv = new AssignStmt(ref, e, new SourcePosition(null, null));
				break;
			case SQLBRACKET:
				acceptIt();
				if (this._currentToken.getTokenType() == TokenType.SQRBRACKET) {
					if (Idtoken.getTokenText().equals("String")) {
						t = new BaseType(TypeKind.UNSUPPORTED, new SourcePosition(null, null));
					} else {
						t = new ClassType(new Identifier(Idtoken), new SourcePosition(null, null));
					}
					t = new ArrayType(t, new SourcePosition(Idtoken, Idtoken));
					acceptIt();
					name = this._currentToken.getTokenText();
					accept(TokenType.IDENTIFIER);
					vd = new VarDecl(t, name, new SourcePosition(null, null));
					accept(TokenType.ASSIGNMENT);
					e = parseE();
					accept(TokenType.SEMICOLON);
					rv = new VarDeclStmt(vd, e, new SourcePosition(null, null));
					break;
				} else {
					// but it might not be Ix Assignment statement
					ref = new IdRef(new Identifier(Idtoken), new SourcePosition(null, null));
					e = parseE();
					accept(TokenType.SQRBRACKET);
					accept(TokenType.ASSIGNMENT);
					rv = new IxAssignStmt(ref, e, parseE(), new SourcePosition(null, null));
					accept(TokenType.SEMICOLON);
					break;
				}
			case LPAREN:
				acceptIt();
				// call stmt
				if (
					this._currentToken.getTokenType() == TokenType.NUMBER ||
					this._currentToken.getTokenType() == TokenType.FALSE ||
					this._currentToken.getTokenType() == TokenType.TRUE ||
					this._currentToken.getTokenType() == TokenType.NEW ||
					this._currentToken.getTokenType() == TokenType.LPAREN ||
					this._currentToken.getTokenType() == TokenType.IDENTIFIER ||
					this._currentToken.getTokenType() == TokenType.THIS ||
					this._currentToken.getTokenText() == "!" ||
					this._currentToken.getTokenText() == "-"
				) {
					parseArgumentList(exprList);
				}
				accept(TokenType.RPAREN);
				accept(TokenType.SEMICOLON);
				rv = new CallStmt(new IdRef(new Identifier(Idtoken), new SourcePosition(null, null)), exprList,
				 new SourcePosition(null, null));
				break;
			case DOT:
				//qual ref
				ref = new IdRef(new Identifier(Idtoken), new SourcePosition(null, null));
				acceptIt();
				ref = new QualRef(ref, new Identifier(this._currentToken), new SourcePosition(null, null));
				accept(TokenType.IDENTIFIER);
				while (this._currentToken.getTokenType() == TokenType.DOT) {
					acceptIt();
					ref = new QualRef(ref, new Identifier(this._currentToken), new SourcePosition(null, null));
					accept(TokenType.IDENTIFIER);
				}
				rv = parseJ(ref);
				accept(TokenType.SEMICOLON);
				break;
			default:
				parseError("Invalid Term - found " + this._currentToken.getTokenType());
				return null;
			}
			return rv;
		case RETURN:
			acceptIt();
			// expression starters
			if (
				this._currentToken.getTokenType() == TokenType.NUMBER ||
				this._currentToken.getTokenType() == TokenType.FALSE ||
				this._currentToken.getTokenType() == TokenType.TRUE ||
				this._currentToken.getTokenType() == TokenType.NEW ||
				this._currentToken.getTokenType() == TokenType.LPAREN ||
				this._currentToken.getTokenType() == TokenType.IDENTIFIER ||
				this._currentToken.getTokenType() == TokenType.THIS ||
				this._currentToken.getTokenText() == "!" ||
				this._currentToken.getTokenText() == "-"
			) {
				e = parseE();
			} else {
				/// should never reach this i don't think
				e = null;
			}
			rv = new ReturnStmt(e, new SourcePosition(null, null));
			//accept(TokenType.RPAREN);
			accept(TokenType.SEMICOLON);
			return rv;
		case IF:
			acceptIt();
			accept(TokenType.LPAREN);
			e = parseE();
			accept(TokenType.RPAREN);
			rv = parseStatement();
			if (this._currentToken.getTokenType() == TokenType.ELSE) {
				acceptIt();
				rv = new IfStmt(e, rv, parseStatement(), new SourcePosition(null, null));
			} else {
				rv = new IfStmt(e, rv, new SourcePosition(null,null));
			}
			return rv;
		case WHILE:
			acceptIt();
			accept(TokenType.LPAREN);
			e = parseE();
			accept(TokenType.RPAREN);
			rv = parseStatement();
			rv = new WhileStmt(e, rv, new SourcePosition(null, null));
			return rv;
			
		default:
			parseError("Invalid Term - found " + this._currentToken.getTokenType());
			return null;
		}
	}

	/* 
	 * Expression ::=
		( Id | this ) ( . id )* (  | [ Expression ] | '('(Expression ( , Expression )*)? ')' )

		| unop Expression
		| ( Expression )
		| num | true | false | new ( id() | … | … )
		(binop Expression)*
	 */

	private Expression parseE() throws SyntaxError {

		// need to encode more precedence here
		Expression rv = parseA();
		while (this._currentToken.getTokenText().equals("||")) {
			Operator o = new Operator(_currentToken);
			acceptIt();
			rv = new BinaryExpr(o, rv, parseA(), new SourcePosition(null,null));
		}
	//	while (this._currentToken.getTokenType() == TokenType.OPERATOR && !this._currentToken.getTokenText().equals("!")) {
	//		Operator o = new Operator(_currentToken);
	//		acceptIt();
	//		rv = new BinaryExpr(o, rv ,parseT(), new SourcePosition(null, null));
	//	}
		return rv;

	}

	private Expression parseA() {
		Expression rv = parseB();
		while (this._currentToken.getTokenText().equals("&&")) {
			Operator o = new Operator(_currentToken);
			acceptIt();
			rv = new BinaryExpr(o, rv, parseB(), new SourcePosition(null,null));
		}
		return rv;
	}

	private Expression parseB() {
		Expression rv = parseC();
		while (this._currentToken.getTokenText().equals("==") || this._currentToken.getTokenText().equals("!=")) {
			Operator o = new Operator(_currentToken);
			acceptIt();
			rv = new BinaryExpr(o, rv, parseC(), new SourcePosition(null,null));
		}
		return rv;
	}

	private Expression parseC() {
		Expression rv = parseD();
		while (this._currentToken.getTokenText().equals("<=") ||
		this._currentToken.getTokenText().equals(">=") ||
		this._currentToken.getTokenText().equals("<") ||
		this._currentToken.getTokenText().equals(">")) {
			Operator o = new Operator(_currentToken);
			acceptIt();
			rv = new BinaryExpr(o, rv, parseD(), new SourcePosition(null,null));
		}
		return rv;
	}

	private Expression parseD() {
		Expression rv = parseF();
		while (this._currentToken.getTokenText().equals("+") || this._currentToken.getTokenText().equals("-")) {
			Operator o = new Operator(_currentToken);
			acceptIt();
			rv = new BinaryExpr(o, rv, parseF(), new SourcePosition(null,null));
		}
		return rv;
	}

	private Expression parseF() {
		Expression rv = parseG();
		while (this._currentToken.getTokenText().equals("*") || this._currentToken.getTokenText().equals("/")) {
			Operator o = new Operator(_currentToken);
			acceptIt();
			rv = new BinaryExpr(o, rv, parseG(), new SourcePosition(null,null));
		}
		return rv;
	}


	private Expression parseG() {
		Operator op;
		Expression e2;
		if (this._currentToken.getTokenType() == TokenType.OPERATOR &&
		(this._currentToken.getTokenText().equals("!") || this._currentToken.getTokenText().equals("-"))) {
				op = new Operator(this._currentToken);
				acceptIt();
				e2 = parseG(); // this parse expression
				return new UnaryExpr(op, e2, new SourcePosition(null, null));
				// this unary case needs to somehow return a binary expression
		} else {
			return parseH();
		}
	}

	private Expression parseH() {

		Expression rv;

		switch (this._currentToken.getTokenType()) {
			// need to make a choice up here first 
		case IDENTIFIER: case THIS:
			Reference ref;
			if (this._currentToken.getTokenType() == TokenType.IDENTIFIER) {
				ref = new IdRef(new Identifier(_currentToken), new SourcePosition(null, null));
			} else {
				ref = new ThisRef(new SourcePosition(null,null));
			}
			acceptIt();
			while (this._currentToken.getTokenType() == TokenType.DOT) {
				acceptIt();
				// does this line work?
				ref = new QualRef(ref, new Identifier(_currentToken), new SourcePosition(null, null));
				accept(TokenType.IDENTIFIER);
			}
			if (this._currentToken.getTokenType() == TokenType.SQLBRACKET) {
				// IxExpr
				Expression tmpExpression;
				acceptIt();
				tmpExpression = parseE();
				accept(TokenType.SQRBRACKET);
				rv = new IxExpr(ref, tmpExpression, new SourcePosition(null, null));
			} else if (this._currentToken.getTokenType() == TokenType.LPAREN) {
				// callExpr
				// why do they want a callExpr here need more consistency on wether or not lists should hold
				// null references
				ExprList exprList = new ExprList();
				acceptIt();
				if (
					this._currentToken.getTokenType() == TokenType.NUMBER ||
					this._currentToken.getTokenType() == TokenType.FALSE ||
					this._currentToken.getTokenType() == TokenType.TRUE ||
					this._currentToken.getTokenType() == TokenType.NEW ||
					this._currentToken.getTokenType() == TokenType.LPAREN ||
					this._currentToken.getTokenType() == TokenType.IDENTIFIER ||
					this._currentToken.getTokenType() == TokenType.THIS ||
					this._currentToken.getTokenText() == "!" ||
					this._currentToken.getTokenText() == "-"
				) {
					parseArgumentList(exprList);
				}
				accept(TokenType.RPAREN);
				rv = new CallExpr(ref, exprList, new SourcePosition(null, null));
			} else {
				rv = new RefExpr(ref, new SourcePosition(null, null));
			}
			break;
		case LPAREN:
			// expression? 
			acceptIt();
			rv = parseE();
			accept(TokenType.RPAREN);
			break;
		case NEW:
			// new object or new array;
			Expression e1;
			acceptIt();
			if (this._currentToken.getTokenType() == TokenType.IDENTIFIER) {
				ClassType ct = new ClassType(new Identifier(_currentToken), new SourcePosition(null, null));
				acceptIt();
				if (this._currentToken.getTokenType() == TokenType.LPAREN) {
					acceptIt();
					accept(TokenType.RPAREN);
					rv = new NewObjectExpr(ct, new SourcePosition(null, null));
				} else {
					// new array Expr
					accept(TokenType.SQLBRACKET);
					e1 = parseE();
					accept(TokenType.SQRBRACKET);
					rv = new NewArrayExpr(ct, e1, new SourcePosition(null, null));
				}
			} else {
				// new Array Expr
				accept(TokenType.INT);
				accept(TokenType.SQLBRACKET);
				e1 = parseE();
				accept(TokenType.SQRBRACKET);
				rv = new NewArrayExpr(new BaseType(TypeKind.INT, new SourcePosition(null, null)), e1,
				 new SourcePosition(null, null));
			}
			break;
		case NUMBER: case TRUE: case FALSE: case NULL:
			Terminal terminal;
			if (this._currentToken.getTokenType() == TokenType.NUMBER) {
				terminal = new IntLiteral(this._currentToken);
			} else if (this._currentToken.getTokenType() == TokenType.NULL) {
				terminal = new NullLiteral(_currentToken);
			} else {
				terminal = new BooleanLiteral(_currentToken);
			}
			acceptIt();
			rv = new LiteralExpr(terminal, new SourcePosition(null, null));
			break;
		default:
			rv = null;
			parseError("Invalid Term - found " + this._currentToken.getTokenType());
			break;
		}
		return rv;
	}

	// J:: = ( = Expression | [ Expression ] = Expression | (ArgumetnList?) ))
	private Statement parseJ(Reference ref) throws SyntaxError {
		switch(this._currentToken.getTokenType()) {
		case ASSIGNMENT:
		// assingment Statement
			acceptIt();
			return new AssignStmt(ref, parseE(), new SourcePosition(null, null));
		case SQLBRACKET:
			//Ix assignstmt
			acceptIt();
			Expression i = parseE();
			accept(TokenType.SQRBRACKET);
			accept(TokenType.ASSIGNMENT);
			return new IxAssignStmt(ref, i, parseE(), new SourcePosition(null, null));
		case LPAREN:
			// Call sttmt
			ExprList exprList = new ExprList();
			acceptIt();
			if (
					this._currentToken.getTokenType() == TokenType.NUMBER ||
					this._currentToken.getTokenType() == TokenType.FALSE ||
					this._currentToken.getTokenType() == TokenType.TRUE ||
					this._currentToken.getTokenType() == TokenType.NEW ||
					this._currentToken.getTokenType() == TokenType.LPAREN ||
					this._currentToken.getTokenType() == TokenType.IDENTIFIER ||
					this._currentToken.getTokenType() == TokenType.THIS ||
					this._currentToken.getTokenText() == "!" ||
					this._currentToken.getTokenText() == "-"
			) {
				parseArgumentList(exprList);
			}
			accept(TokenType.RPAREN);
			return new CallStmt(ref, exprList, new SourcePosition(null, null));
		default:
			parseError("Invalid Term - found " + this._currentToken.getTokenType());
			return null; // null thing...
		}
	}

	// L ::= '(' ParameterList? ')' {Statement*}
	private MethodDecl parseL(FieldDecl fieldDecl) throws SyntaxError {

		ParameterDeclList parameterDeclList = new ParameterDeclList();
		StatementList statementList = new StatementList();
		Token startToken = this._currentToken;
		Token endToken;
		
		accept(TokenType.LPAREN);
		if (
				this._currentToken.getTokenType() == TokenType.INT ||
				this._currentToken.getTokenType() == TokenType.BOOLEAN ||
				this._currentToken.getTokenType() == TokenType.IDENTIFIER
				) {
				parameterDeclList = parseParameterList();
			}
		accept(TokenType.RPAREN);
		accept(TokenType.LBRACKET);
		while (
				this._currentToken.getTokenType() == TokenType.LBRACKET ||
				this._currentToken.getTokenType() == TokenType.INT ||
				this._currentToken.getTokenType() == TokenType.BOOLEAN ||
				this._currentToken.getTokenType() == TokenType.THIS ||
				this._currentToken.getTokenType() == TokenType.IDENTIFIER ||
				this._currentToken.getTokenType() == TokenType.IF ||
				this._currentToken.getTokenType() == TokenType.RETURN ||
				this._currentToken.getTokenType() == TokenType.WHILE
				) {
			statementList.add(parseStatement());
		}
		accept(TokenType.RBRACKET);
		endToken = this._currentToken;
		return new MethodDecl(fieldDecl, parameterDeclList, statementList, new SourcePosition(startToken, endToken));
	}

	//ArgumentList ::= Expression ( , Expression )*
	private void parseArgumentList(ExprList exprList) throws SyntaxError {
		exprList.add(parseE());
		while (this._currentToken.getTokenType() == TokenType.COMMA) {
			acceptIt();
			exprList.add(parseE());
		}
	}

	//ParameterList ::= Type id ( , Type id )* 
	private ParameterDeclList parseParameterList() throws SyntaxError {
		
		ParameterDeclList parameterDeclList = new ParameterDeclList();
		TypeDenoter t;
		String typeName;
		t = parseType();
		typeName = this._currentToken.getTokenText();
		accept(TokenType.IDENTIFIER);
		parameterDeclList.add(new ParameterDecl(t, typeName, new SourcePosition(null, null)));
		while(this._currentToken.getTokenType() == TokenType.COMMA) {
			acceptIt();
			t = parseType();
			typeName = this._currentToken.getTokenText();
			accept(TokenType.IDENTIFIER);
			parameterDeclList.add(new ParameterDecl(t, typeName, new SourcePosition(null, null)));
		}

		return parameterDeclList;
	}

	//Type ::= int (  | [] ) | id (  | [] ) | boolean
	private TypeDenoter parseType() throws SyntaxError {
		TypeDenoter t;
		switch(this._currentToken.getTokenType()) {
		case INT: case IDENTIFIER:
			if(this._currentToken.getTokenType() == TokenType.INT) {
				t = new BaseType(TypeKind.INT, new SourcePosition(null, null));
			} else {
				if (this._currentToken.getTokenText().equals("String")) {
					t = new BaseType(TypeKind.UNSUPPORTED, new SourcePosition(null, null));
				} else {
					t =  new ClassType(new Identifier(this._currentToken), new SourcePosition(null, null));
				}
			}
			acceptIt();
			if (this._currentToken.getTokenType() == TokenType.SQLBRACKET) {
				acceptIt();
				accept(TokenType.SQRBRACKET);
				return new ArrayType(t, new SourcePosition(null, null));
			}
			return t;
		case BOOLEAN:
			t = new BaseType(TypeKind.BOOLEAN, new SourcePosition(null, null));
			acceptIt();
			return t;
		default:
			parseError("Invalid Term - found " + this._currentToken.getTokenType());
			return null;
		} 
	}
	
	// This method will accept the token and retrieve the next token.
	//  Can be useful if you want to error check and accept all-in-one.
	private void accept(TokenType expectedType) throws SyntaxError {
		if( _currentToken.getTokenType() == expectedType ) {
			if (trace) {
				pTrace();
			}
			_currentToken = _scanner.scan();
			return;
		} else {
			parseError("expecting '" + expectedType +
					"' but found '" + this._currentToken.getTokenType() + "'");
		}
	}

	private void acceptIt() throws SyntaxError {
		accept(this._currentToken.getTokenType());
	}


	private void parseError(String e) throws SyntaxError {
		_errors.reportError(this._currentToken.getTokenPosition().getLineNumber(),
		this._currentToken.getTokenPosition().getColumnNumber(),"Parse error: " + e);
		throw new SyntaxError();
	}
	
	private void pTrace() {
		StackTraceElement [] stl = Thread.currentThread().getStackTrace();
		for (int i = stl.length - 1; i > 0 ; i--) {
			if(stl[i].toString().contains("parse"))
				System.out.println(stl[i]);
		}
		System.out.println("accepting: " + this._currentToken.getTokenType() + " (\"" + this._currentToken.getTokenText() + "\")");
		System.out.println();
	}
}
