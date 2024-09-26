package miniJava.ContextualAnalysis;

import java.lang.reflect.Array;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenType;;

public class TypeChecker implements Visitor<Object, TypeDenoter>{

    ErrorReporter errorReporter;

    public TypeChecker(ErrorReporter errorReporter) {
        this.errorReporter = errorReporter;
    }

    public void typeCheckTree(AST ast){
        ast.visit(this, null);
    }   

    @Override
    public TypeDenoter visitPackage(Package prog, Object arg) {
        for (ClassDecl classDecl : prog.classDeclList) {
            classDecl.visit(this, null);
        }
        return null;
    }

    @Override
    public TypeDenoter visitClassDecl(ClassDecl cd, Object arg) {
        for (FieldDecl fieldDecl : cd.fieldDeclList) {
            fieldDecl.visit(this, null);
        }
        for (MethodDecl methodDecl: cd.methodDeclList) {
            methodDecl.visit(this, null);
        }
        return null;
    }

    @Override
    public TypeDenoter visitFieldDecl(FieldDecl fd, Object arg) {
        fd.type.visit(this,null); // what are we doing when visiting a type? 
        return null;
    }

    @Override
    public TypeDenoter visitMethodDecl(MethodDecl md, Object arg) {
        ParameterDeclList pdl = md.parameterDeclList;
        for (ParameterDecl pd: pdl) {
            pd.visit(this, null);
        }

        StatementList sl = md.statementList;
        for (Statement s: sl) {
            s.visit(this, null);
        }
        return null;
    }

    @Override
    public TypeDenoter visitParameterDecl(ParameterDecl pd, Object arg) {
        return pd.type.visit(this, null); // might have to revist this one too
    }

    @Override public TypeDenoter visitVarDecl(VarDecl decl, Object arg) {
        // should we visit the type and return it from there?  might save a line of code
        // is this in correct....
        decl.type.visit(this, null);
        return decl.type; 
    }

    @Override
    public TypeDenoter visitBaseType(BaseType type, Object arg) {
        return type;

    }

    @Override
    public TypeDenoter visitClassType(ClassType type, Object arg) {
        type.className.visit(this, type);
        return type;
    }

    @Override
    public TypeDenoter visitArrayType(ArrayType type, Object arg) {
        type.eltType.visit(this, null);
        return type;
    }

    @Override
    public TypeDenoter visitBlockStmt(BlockStmt stmt, Object arg) {
        StatementList sl = stmt.sl;
        for (Statement s: sl) {
        	s.visit(this, null);
        }
        return null;
    }

    @Override
    public TypeDenoter visitVardeclStmt(VarDeclStmt stmt, Object arg) {
        // need to make sure that the RHS type and LHS type match 
        /// cannot reference variable currently being declared 
        if (isSelfContained(stmt.varDecl.name, stmt.initExp)) {
            errorReporter.reportError("cannot reference decl withing initalization of decl");
        }
        TypeDenoter lhsType = stmt.varDecl.visit(this, null);	
        TypeDenoter rhsType = stmt.initExp.visit(this, null);
        // if null then its a class type 
        TypeDenoter result = assignment(lhsType, rhsType);
        if (result.typeKind.equals(TypeKind.ERROR)) {
            this.errorReporter.reportError("Incompatible types for var decl assingment statement");
        }

        return result;
    }

    @Override
    public TypeDenoter visitAssignStmt(AssignStmt stmt, Object arg) {
        TypeDenoter lhsType = stmt.ref.visit(this, null);	
        TypeDenoter rhsType = stmt.val.visit(this, null);
        TypeDenoter result = assignment(lhsType, rhsType);
        if (result.typeKind.equals(TypeKind.ERROR)) {
            this.errorReporter.reportError("Incompatible types for assingment statement");
        }

        return result;
    }

    // this might need some type checking around if the expression inside the brackets is an int
    @Override
    public TypeDenoter visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
        TypeDenoter lhsType = stmt.ref.visit(this, null);	
        lhsType = ((ArrayType) lhsType).eltType;
        TypeDenoter rhsType = stmt.exp.visit(this, null);
        TypeDenoter result = assignment(lhsType, rhsType);
        if (result.typeKind.equals(TypeKind.ERROR)) {
            this.errorReporter.reportError("Incompatible types for Ix assingment statement");
        }

        return result;


    }

    @Override
    public TypeDenoter visitCallStmt(CallStmt stmt, Object arg) {
        // parameters should match arguments? 
        // methodRef should have a decl attached to it.
        // either with an id ref, or a this ref

        if (stmt.methodRef instanceof IdRef || stmt.methodRef instanceof QualRef) {
            // need to check type compatibility of the method ref and the arg list 
            // will the id ref always be a method decl?
            MethodDecl decl;

            if (stmt.methodRef instanceof IdRef) {
                decl = (MethodDecl)((IdRef) stmt.methodRef).id.decl; 
            } else {
                decl = (MethodDecl)((QualRef) stmt.methodRef).id.decl;
            }

            for (int i = 0; i < decl.parameterDeclList.size(); i++) {
                // might have to do with how i do qual ref
                TypeDenoter tmpLHS = decl.parameterDeclList.get(i).visit(this, null);
                TypeDenoter tmpRHS = stmt.argList.get(i).visit(this, null);
                if (!tmpLHS.typeKind.equals(tmpRHS.typeKind)) {
                    errorReporter.reportError("Arguments do not match method declaration");
                    return new BaseType(TypeKind.ERROR, new SourcePosition(null, null));
                }
            }

            return decl.type;

        } else {
            return new BaseType(TypeKind.ERROR, new SourcePosition(null, null));
        }

    }

    @Override
    public TypeDenoter visitReturnStmt(ReturnStmt stmt, Object arg){
        // i think there would be a difference between returning the null expr and there just not being anything there
        // from a compilation point of view, the end result would be the same 
        // is that the same as a null literal? 

        TypeDenoter rv;

         if (stmt.returnExpr != null) {
            rv = stmt.returnExpr.visit(this, null);
         } else {
            rv = null;
         }

         return rv;
    }


    @Override
    public TypeDenoter visitIfStmt(IfStmt stmt, Object arg) {
        TypeDenoter typeResult = stmt.cond.visit(this, null);
        if (!typeResult.typeKind.equals(TypeKind.BOOLEAN)) {
            errorReporter.reportError("non boolean if condition");
        }

        stmt.thenStmt.visit(this, null);
        if (stmt.elseStmt != null)
            stmt.elseStmt.visit(this, null);
        return null;
    }

    @Override
    public TypeDenoter visitWhileStmt(WhileStmt stmt, Object arg) {
        stmt.cond.visit(this, null);
        stmt.body.visit(this, null);
        return null;
    }

    @Override
    public TypeDenoter visitUnaryExpr(UnaryExpr expr, Object arg) {
        expr.operator.visit(this, null);
        TypeDenoter type = expr.expr.visit(this, null);
        TypeDenoter returnType =  checkUnaryTable(type, expr.operator);
        if (returnType.typeKind.equals(TypeKind.ERROR)) {
            errorReporter.reportError("Invalid type used in unary expressoin");
        }
        return returnType;
    }

    @Override
    public TypeDenoter visitBinaryExpr(BinaryExpr expr, Object arg) {
        expr.operator.visit(this, null);
        TypeDenoter lhs = expr.left.visit(this, null);
        TypeDenoter rhs = expr.right.visit(this, null);
        TypeDenoter returnType = checkBinaryTable(lhs, rhs, expr.operator);
        if (returnType.typeKind.equals(TypeKind.ERROR)) {
            errorReporter.reportError("Invalid type used in binary expressoin");
        }
        return returnType;
    }

    @Override
    public TypeDenoter visitRefExpr(RefExpr expr, Object arg) {
        // i don't think there needs to be more checkign
        return expr.ref.visit(this, null);
    }

    @Override
    public TypeDenoter visitIxExpr(IxExpr expr, Object arg) {
        // need to chefck that ref is of Array Type
        TypeDenoter presumedArrayType = expr.ref.visit(this, null);
        TypeDenoter tmpExpr = expr.ixExpr.visit(this, null);
        TypeDenoter rv;

        if (presumedArrayType instanceof ArrayType) {
            rv = ((ArrayType) presumedArrayType).eltType;
        } else {
            errorReporter.reportError("attemped derefernce of non array type");
            rv =  new BaseType(TypeKind.ERROR, null);
        }

        if (!tmpExpr.typeKind.equals(TypeKind.INT)) {
            errorReporter.reportError("index expression does not have type int");
        }

        return rv;
    }

    @Override
    public TypeDenoter visitCallExpr(CallExpr expr, Object arg) {
        expr.functionRef.visit(this, null);
        if (expr.functionRef instanceof IdRef || expr.functionRef instanceof QualRef) {
            MethodDecl decl;

            if (expr.functionRef instanceof IdRef) {
                decl = (MethodDecl)((IdRef) expr.functionRef).id.decl; 
            } else {
                decl = (MethodDecl)((QualRef) expr.functionRef).id.decl;
            }

            for (int i = 0; i < decl.parameterDeclList.size(); i++) {
                TypeDenoter tmpLHS = decl.parameterDeclList.get(i).visit(this, null);
                TypeDenoter tmpRHS = expr.argList.get(i).visit(this, null);
                if (!tmpLHS.typeKind.equals(tmpRHS.typeKind)) {
                    errorReporter.reportError("Arguments do not match function declaration");
                    return new BaseType(TypeKind.ERROR, new SourcePosition(null, null));
                }
            }

            return decl.type;

        } else {
            return new BaseType(TypeKind.ERROR, new SourcePosition(null, null));
        }
    }

    @Override
    public TypeDenoter visitLiteralExpr(LiteralExpr expr, Object arg) {
        return expr.lit.visit(this, null);
    }

    @Override
    public TypeDenoter visitNewObjectExpr(NewObjectExpr expr, Object arg) {
        // when do we visit this new object expr
        return expr.classtype;
    }

    @Override
    public TypeDenoter visitNewArrayExpr(NewArrayExpr expr, Object arg) {
        expr.eltType.visit(this, null);
        expr.sizeExpr.visit(this, null);
        return new ArrayType(expr.eltType, new SourcePosition(null, null));
    }

    @Override
    public TypeDenoter visitThisRef(ThisRef ref, Object arg) {

        return new ClassType(ref.cn, null);
    }

    @Override
    public TypeDenoter visitIdRef(IdRef ref, Object arg) {
        return ref.id.visit(this, null);

    }

    @Override
    public TypeDenoter visitQRef(QualRef ref, Object arg) {
        ref.ref.visit(this, null);
        return ref.id.visit(this, null);
    }

    @Override
    public TypeDenoter visitIdentifier(Identifier id, Object arg) {
        return id.decl.type;
    }

    @Override
    public TypeDenoter visitOperator(Operator op, Object arg) {
        return null;
    }

    @Override
    public TypeDenoter visitIntLiteral(IntLiteral num, Object arg) {
        return new BaseType(TypeKind.INT, new SourcePosition(null, null));
            
    }

    @Override
    public TypeDenoter visitBooleanLiteral(BooleanLiteral bool, Object arg) {
        return new BaseType(TypeKind.BOOLEAN, new SourcePosition(null, null));
    }

    @Override
    public TypeDenoter visitNullLiteral(NullLiteral nl, Object arg) {
        // should the null type kind be added? 
        return new BaseType(TypeKind.NULL, new SourcePosition(null, null));
    }

    private TypeDenoter checkBinaryTable(TypeDenoter lhs, TypeDenoter rhs, Operator op) {
        // needs null check, but we can do that later

        if (lhs.typeKind.equals(TypeKind.BOOLEAN)) {
            if (rhs.typeKind.equals(TypeKind.BOOLEAN)) {
                switch (op.spelling) {
                    case "&&": case "||": case "==": case "!=": case "=":
                        return new BaseType(TypeKind.BOOLEAN,new SourcePosition(null, null));
                    default:
                        return new BaseType(TypeKind.ERROR, new SourcePosition(null, null));
                }

            } else {
                return new BaseType(TypeKind.ERROR, new SourcePosition(null, null));
            }
        } else if (lhs.typeKind.equals(TypeKind.INT)){
            if (rhs.typeKind.equals(TypeKind.INT)) {
                switch(op.spelling) {
                    case ">": case ">=": case "<": case "<=": case "==": case "!=":
                        return new BaseType(TypeKind.BOOLEAN,new SourcePosition(null, null));
                    case "+": case "-": case "*": case "/": case "=":
                        return new BaseType(TypeKind.INT,new SourcePosition(null, null));
                    default:
                        return new BaseType(TypeKind.ERROR, new SourcePosition(null, null));
                }
            }
            else {
                return new BaseType(TypeKind.ERROR, new SourcePosition(null, null));
            }
        } else if (lhs.typeKind.equals(TypeKind.NULL)) {
            if (rhs.typeKind.equals(TypeKind.CLASS)) {
                switch(op.spelling) {
                    case "==": case "!=":
                        return new BaseType(TypeKind.BOOLEAN,new SourcePosition(null, null));
                    default:
                        return new BaseType(TypeKind.ERROR, new SourcePosition(null, null));
                    }
                } else {
                    return new BaseType(TypeKind.ERROR, new SourcePosition(null, null));
                }
        } else if (rhs.typeKind.equals(TypeKind.NULL)) {
            if (lhs.typeKind.equals(TypeKind.CLASS)) {
                switch(op.spelling) {
                    case "==": case "!=":
                        return new BaseType(TypeKind.BOOLEAN,new SourcePosition(null, null));
                    default:
                        return new BaseType(TypeKind.ERROR, new SourcePosition(null, null));
                    }
                } else {
                    return new BaseType(TypeKind.ERROR, new SourcePosition(null, null));
                }
        } else {
            // reporting multiple errors is not going to hurt me 
            return new BaseType(TypeKind.ERROR, new SourcePosition(null, null));
        }
    }

    private TypeDenoter checkUnaryTable(TypeDenoter type, Operator op) {
        // unary can only be - or !
        if (op.spelling.equals("-")) {
            if (!type.typeKind.equals(TypeKind.INT)) {
                return new BaseType(TypeKind.ERROR, new SourcePosition(null, null));
            }
        } else {
            // op is !
            if (!type.typeKind.equals(TypeKind.BOOLEAN)) {
                return new BaseType(TypeKind.ERROR, new SourcePosition(null, null));
            }
        }

        return new BaseType(type.typeKind, new SourcePosition(null, null));
    }

    private boolean compareArrayTypes(ArrayType arrayType1, ArrayType arrayType2) {

        boolean result;

        if (arrayType1.eltType.typeKind.equals(arrayType2.eltType.typeKind)) {
            if (arrayType1.eltType.typeKind.equals(TypeKind.ARRAY)) {
                result = compareArrayTypes((ArrayType)arrayType1.eltType, (ArrayType)arrayType2.eltType);
            } else {
                result = true;
            }
        } else {
            result = false;
        }
        return result;
    }

    private TypeDenoter assignment(TypeDenoter lhsType, TypeDenoter rhsType) {

        TypeDenoter result;

        if (lhsType.typeKind.equals(rhsType.typeKind)) {
            if (lhsType.typeKind.equals(TypeKind.CLASS)) {
                String lhsClassname = ((ClassType) lhsType).className.spelling;
                String rhsClassName = ((ClassType) rhsType).className.spelling;
                if (!lhsClassname.equals(rhsClassName)) {
                    result = new BaseType(TypeKind.ERROR, new SourcePosition(null, null));
                } else {
                    result = lhsType;
                }
            } else if (lhsType.typeKind.equals(TypeKind.ARRAY)){
                if (compareArrayTypes((ArrayType) lhsType, (ArrayType) rhsType)) {
                    result = lhsType;
                } else {
                    result = new BaseType(TypeKind.ERROR, new SourcePosition(null, null));
                }

            } else {
                result = lhsType;
            }
        } else {
            // this might break some stuff
            if (lhsType.typeKind.equals(TypeKind.CLASS) && rhsType.typeKind.equals(TypeKind.NULL)) {
                result = lhsType;
            } else {
                result = new BaseType(TypeKind.ERROR, new SourcePosition(null, null));
            }
        }

        return result;
    }

    private boolean isSelfContained(String declName, Expression expr) {

        RefExpr refExpr;
        IxExpr ixExpr;
        NewArrayExpr newArrayExpr;
        BinaryExpr binaryExpr;
        UnaryExpr unaryExpr;
        IdRef idRef;
        boolean selfContained = false;
        // any refernces inside of hte expression cannot be the same
        // how do i get all references?
        // check which expr it is and go from there?
        if (expr instanceof RefExpr) {
            refExpr = (RefExpr) expr;
            if (refExpr.ref instanceof IdRef){
                idRef = (IdRef) refExpr.ref;
                if (idRef.id.spelling.equals(declName)) {
                    selfContained = true;
                }
            }
        } else if (expr instanceof IxExpr) {
            ixExpr = (IxExpr) expr;
            if (ixExpr.ref instanceof IdRef){
                idRef = (IdRef) ixExpr.ref;
                if (idRef.id.spelling.equals(declName)) {
                    selfContained = true;
                }
            }
        } else if (expr instanceof UnaryExpr) {
            unaryExpr = (UnaryExpr) expr;
            selfContained = isSelfContained(declName, unaryExpr.expr);
        } else if (expr instanceof BinaryExpr){
            binaryExpr = (BinaryExpr) expr;
            selfContained = isSelfContained(declName, binaryExpr.left) || isSelfContained(declName, binaryExpr.right);
        } else if (expr instanceof NewArrayExpr) {
            newArrayExpr = (NewArrayExpr) expr;
            selfContained = isSelfContained(declName, newArrayExpr.sizeExpr);
        }

        return selfContained;
    }
}
