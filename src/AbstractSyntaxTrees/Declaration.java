/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.CodeGeneration.x64.Reg64;
import miniJava.SyntacticAnalyzer.SourcePosition;

public abstract class Declaration extends AST {

	public Reg64 ref;
	public int offset; // in bytes. this is a negatie offset as well
	
	public Declaration(String name, TypeDenoter type, SourcePosition posn) {
		super(posn);
		this.name = name;
		this.type = type;
	}
	
	public String name;
	public TypeDenoter type;
}
