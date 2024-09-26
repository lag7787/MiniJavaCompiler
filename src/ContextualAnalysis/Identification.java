package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ErrorReporter;

import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.TokenType;
import miniJava.SyntacticAnalyzer.Token;

public class Identification implements Visitor<Environment, Declaration> {

    ScopedIdentification scopedIdentification;
    ErrorReporter errorReporter;

    public Identification(ErrorReporter errorReporter) {
        this.scopedIdentification = new ScopedIdentification();
        this.errorReporter = errorReporter;
    }

    public void identifyTree(AST ast){
        ast.visit(this, null);
    }   

    @Override
    public Declaration visitPackage(Package prog, Environment arg) {
        //level 0 class decls
        this.scopedIdentification.openScope();

        ClassDeclList predefiniedNames = addPredefinedNames();

        for (ClassDecl decl : predefiniedNames) {
            try {
                scopedIdentification.addDeclaration(decl.name, decl);
            } catch (IdentificationError error) {
                errorReporter.reportError("Error adding class decl");
            }
        }

        for (ClassDecl decl : prog.classDeclList) {
            try {
                scopedIdentification.addDeclaration(decl.name, decl);
            } catch (IdentificationError error) {
                errorReporter.reportError("Error adding class decl");
            }
        }


        //level 1 member decls
        this.scopedIdentification.openScope();

        for (ClassDecl decl: predefiniedNames) {
            for (FieldDecl fieldDecl : decl.fieldDeclList) {
                try {
                    scopedIdentification.addDeclaration(decl.name + "." + fieldDecl.name,
                    fieldDecl);
                } catch (IdentificationError error) {
                    errorReporter.reportError("Error adding field decl");
                }
            }

            for (MethodDecl methodDecl : decl.methodDeclList) {
                try {
                    scopedIdentification.addDeclaration(decl.name + "." + methodDecl.name,
                    methodDecl);
                } catch (IdentificationError error) {
                    errorReporter.reportError("Error adding method decl");
                }
            }
        }


        for (ClassDecl decl: prog.classDeclList) {
            // might refactor this so that it uses visitFieldDecl method
            for (FieldDecl fieldDecl : decl.fieldDeclList) {
                try {
                    scopedIdentification.addDeclaration(decl.name + "." + fieldDecl.name,
                    fieldDecl);
                } catch (IdentificationError error) {
                    errorReporter.reportError("Error adding field decl");
                }
            }
            for (MethodDecl methodDecl : decl.methodDeclList) {
                try {
                    scopedIdentification.addDeclaration(decl.name + "." + methodDecl.name,
                    methodDecl);
                } catch (IdentificationError error) {
                    errorReporter.reportError("Error adding method decl");
                }
            }
        }

        // visit class Decls

        for (ClassDecl decl: prog.classDeclList) {
            // passing the class name for instance references 
            // could be like an enum for a class instane or a static instance
            decl.visit(this, new Environment(decl.name));
        }

        return null;
    }

    @Override
    public Declaration visitClassDecl(ClassDecl cd, Environment arg) {
        // not doing anything with this type
        for (FieldDecl fieldDecl : cd.fieldDeclList) {
            fieldDecl.visit(this, arg);
        }

        for (MethodDecl methodDecl : cd.methodDeclList) {
            // should be two different kinds of visits here, one with a static indicator and one without 
            methodDecl.visit(this, new Environment(arg.className, methodDecl.isStatic));
        }

        return null;
    }

    @Override
    public Declaration visitFieldDecl(FieldDecl fd, Environment arg) {
        fd.type.visit(this, arg);
        return null;
    }

    @Override
    public Declaration visitMethodDecl(MethodDecl md, Environment arg) {
        scopedIdentification.openScope();
        for (ParameterDecl parameterDecl : md.parameterDeclList) {
            visitParameterDecl(parameterDecl, arg);
        }

        // visit statements
        StatementList sl = md.statementList;
        for (Statement s: sl) {
            if (s instanceof ReturnStmt && md.type.typeKind.equals(TypeKind.VOID)) {
                if (((ReturnStmt) s).returnExpr != null) {
                    errorReporter.reportError("cannot reutrn a value from a void method");
                }
            }
            s.visit(this, arg);
        }

        scopedIdentification.closeScope();
        return null;
    }

    @Override
    public Declaration visitParameterDecl(ParameterDecl pd, Environment arg) {
        // not doing anything with this type
        pd.type.visit(this, arg);
        try {
            scopedIdentification.addDeclaration(pd.name, pd);
        } catch (IdentificationError error) {
            errorReporter.reportError("Error adding parameter decl");
        }
        return null;
    }

    @Override
    public Declaration visitVarDecl(VarDecl decl, Environment arg) {
        decl.type.visit(this, arg);
        try {
            scopedIdentification.addDeclaration(decl.name, decl);
        } catch (IdentificationError error) {
            errorReporter.reportError("Error adding var decl");
        }
        return null;
    }

    @Override
    public Declaration visitBaseType(BaseType type, Environment arg) {
        return null;
    }

    @Override
    public Declaration visitClassType(ClassType type, Environment arg) {
        // explicit visitation of class type 
        //type.className.visit(this, arg);
        Identifier id = type.className;
        Declaration declaration = this.scopedIdentification.findDeclaration(id.spelling, arg.className, false, true);
        if (declaration == null) {
            errorReporter.reportError("no declaration for identifier");
        } else {
            id.decl = declaration;
        }
        return null;
    }

    @Override
    public Declaration visitArrayType(ArrayType type, Environment arg) {
        type.eltType.visit(this, arg);
        return null;
    }

    @Override
    public Declaration visitBlockStmt(BlockStmt stmt, Environment arg) {
        // need a new scope to declare variables
        scopedIdentification.openScope();
        // visit statements
        StatementList sl = stmt.sl;
        for (Statement s: sl) {
            s.visit(this, arg);
        }
        scopedIdentification.closeScope();
        return null;
    }

    @Override
    public Declaration visitVardeclStmt(VarDeclStmt stmt, Environment arg) {
        stmt.varDecl.visit(this, arg);
        Declaration decl = stmt.initExp.visit(this, arg);
        if (decl instanceof ClassDecl)  {
            if (stmt.initExp instanceof RefExpr) {
                RefExpr expr = (RefExpr) stmt.initExp;
                if (!(expr.ref instanceof ThisRef)) {
                    errorReporter.reportError("expression is not a varialbe");
                }
            } else {
                errorReporter.reportError("expression is not a varialbe");
            }
        } else if (decl instanceof MethodDecl) {
            errorReporter.reportError("expression is not a varialbe");
        }
        return null;
    }

    @Override
    public Declaration visitAssignStmt(AssignStmt stmt, Environment arg) {
        stmt.ref.visit(this, arg);
        Declaration decl = stmt.val.visit(this, arg);
        if (decl instanceof ClassDecl)  {
            if (stmt.val instanceof RefExpr) {
                RefExpr expr = (RefExpr) stmt.val;
                if (!(expr.ref instanceof ThisRef)) {
                    errorReporter.reportError("expression is not a varialbe");
                }
            } else {
                errorReporter.reportError("expression is not a varialbe");
            }
        } else if (decl instanceof MethodDecl) {
            errorReporter.reportError("expression is not a varialbe");
        }


        return null;
    }

    @Override
    public Declaration visitIxAssignStmt(IxAssignStmt stmt, Environment arg) {
        stmt.ref.visit(this, arg);
        stmt.ix.visit(this, arg);
        stmt.exp.visit(this, arg);
        return null;
    }

    @Override
    public Declaration visitCallStmt(CallStmt stmt, Environment arg) {
        Declaration decl = stmt.methodRef.visit(this, arg);
        if (!(decl instanceof MethodDecl)) {
            errorReporter.reportError("declaration does not denote a method");
        }
        ExprList al = stmt.argList;
        for (Expression e: al) {
            e.visit(this, arg);
        }
        return null;
    }

    @Override
    public Declaration visitReturnStmt(ReturnStmt stmt, Environment arg) {
        if (stmt.returnExpr != null)
        stmt.returnExpr.visit(this, arg);
        return null;
    }

    @Override
    public Declaration visitIfStmt(IfStmt stmt, Environment arg) {
        stmt.cond.visit(this, arg);
        if (stmt.thenStmt instanceof VarDeclStmt) {
            errorReporter.reportError("solitary variable decl statement not permitted here");
        }
        stmt.thenStmt.visit(this, arg);
        if (stmt.elseStmt != null) {
            if (stmt.elseStmt instanceof VarDeclStmt) {
                errorReporter.reportError("solitary variable decl statement not permitted here");
            }
            stmt.elseStmt.visit(this,arg);
        }
        return null;
    }

    @Override
    public Declaration visitWhileStmt(WhileStmt stmt, Environment arg) {
        stmt.cond.visit(this, arg);
        if (stmt.body instanceof VarDeclStmt) {
            errorReporter.reportError("solitary variable decl statement not permitted here");
        }
        stmt.body.visit(this, arg);
        return null;
    }

    @Override
    public Declaration visitUnaryExpr(UnaryExpr expr, Environment arg) {
        expr.operator.visit(this, arg);
        expr.expr.visit(this, arg);
        return null;
    }

    @Override
    public Declaration visitBinaryExpr(BinaryExpr expr, Environment arg) {
        expr.operator.visit(this, arg);
        expr.left.visit(this, arg);
        expr.right.visit(this,arg);
        return null;
    }

    @Override
    public Declaration visitRefExpr(RefExpr expr, Environment arg) {
        Declaration decl = expr.ref.visit(this, arg);
        return decl;
    }

    @Override
    public Declaration visitIxExpr(IxExpr expr, Environment arg) {
        expr.ref.visit(this, arg);
        expr.ixExpr.visit(this, arg);
        return null;
    }

    @Override
    public Declaration visitCallExpr(CallExpr expr, Environment arg) {
        Declaration decl = expr.functionRef.visit(this, arg);
        if (!(decl instanceof MethodDecl)) {
            errorReporter.reportError("declaration does not denote a method");
        }
        ExprList al = expr.argList;
        for (Expression e: al) {
            e.visit(this, arg);
        }
        return null;
    }

    @Override
    public Declaration visitLiteralExpr(LiteralExpr expr, Environment arg) {
        expr.lit.visit(this, arg);
        return null;
    }

    @Override
    public Declaration visitNewObjectExpr(NewObjectExpr expr, Environment arg) {
        expr.classtype.visit(this, arg);
        return null;
    }

    @Override
    public Declaration visitNewArrayExpr(NewArrayExpr expr, Environment arg) {
        expr.eltType.visit(this, arg);
        expr.sizeExpr.visit(this, arg);
        return null;
    }

    @Override
    // need to think more about what my arg can do... 
    // this might be breaking some things
    public Declaration visitThisRef(ThisRef ref, Environment arg) {
        if (arg.isStatic) {
            errorReporter.reportError("this reference inside of static context");
            return null;
        }
        ClassDecl classDecl = (ClassDecl) this.scopedIdentification.findDeclaration(arg.className, arg.className, false, false);
        ref.cn = new Identifier(new Token(TokenType.CLASS, arg.className, 0, 0));

        //i forget what other test this class decl was for

        return classDecl;
    }

    @Override
    public Declaration visitIdRef(IdRef ref, Environment arg) {
    	return ref.id.visit(this, arg);
    }


    /*
     * ref - the qualified ref that we are visitng
     * 
     * arg - the class name that we are currently visiting
     * 
     * return type : Declaration - a decl representing the context of recursive calls
     */

    @Override
    public Declaration visitQRef(QualRef ref, Environment arg) {

        // thinking about privat and this
        // can we ever have a legal private qualified ref 
        // i think it just means we update the context as we iterate
        // is the context for local decls the method name? 
        // could store, lookup and split  might be cleaner 

        MemberDecl decl;
        Declaration context;
        ClassType type;

        context = ref.ref.visit(this, arg);
        if (context == null) {
            errorReporter.reportError("could not find context");
            return null;
        }
        
        if (context instanceof ClassDecl) {
            decl = (MemberDecl) scopedIdentification.findDeclaration(ref.id.spelling, context.name, true, false);
            if (!(ref.ref instanceof ThisRef)) {
                if (!decl.isStatic) {
                    errorReporter.reportError("Static refernce of non static member");
                    return null;
                }
            }
        } else {
            // need to retrieve the class type then use that to perform lookup of member delc
            // need to make sure context is not a method decl
            if ((!(context instanceof MethodDecl)) && context.type instanceof ClassType) {
                    type = (ClassType) context.type;
                    decl = (MemberDecl) scopedIdentification.findDeclaration(ref.id.spelling, type.className.spelling, true, false);
            } else {
                errorReporter.reportError("Attempt to derefernce something other than a var decl");
                return null;
            }
        }

        if (decl == null) {
            errorReporter.reportError("decl not found");
            return null;
        } else {
            // this doesn't handle staic to my knowledge
            if (decl.isPrivate) {
                boolean invalidAccess = true;
                if (context instanceof FieldDecl || context instanceof VarDecl) {
                    type = (ClassType) context.type;
                    if (type.className.spelling.equals(arg.className)) {
                        invalidAccess = false;
                    }
                } else {
                    ClassDecl classDecl = (ClassDecl) context;
                    if (classDecl.name.equals(arg.className)) {
                        invalidAccess = false;
                    }
                }

                if (invalidAccess){
                    errorReporter.reportError("improper access of private variable");
                    return null;
                }
            }
        }
        ref.id.decl = decl;
        return decl;
    }

    @Override
    public Declaration visitIdentifier(Identifier id, Environment arg) {
        // we should know wether its a field or a local var right away? 

        if (id.decl == null) {
            // this presupposes that a class name won't share an identifier name 
            // the environment arg is always going to be present and have somethign in it 
            Declaration declaration = this.scopedIdentification.findDeclaration(id.spelling, arg.className, false, false);
            if (declaration == null) {
                errorReporter.reportError("no declaration for identifier");
            } else {
                id.decl = declaration;
            }
        }

        if (id.decl instanceof MemberDecl) {
            MemberDecl tmpDecl = (MemberDecl) id.decl;
            if (arg.isStatic) {
                if (!tmpDecl.isStatic) {
                    errorReporter.reportError("non static reference in a static member");
                    return null;
                }
            }
        }
        
        return id.decl;
    }

    @Override
    public Declaration visitOperator(Operator op, Environment arg) {
        return null;
    }

    @Override
    public Declaration visitIntLiteral(IntLiteral num, Environment arg) {
        return null;
    }

    @Override
    public Declaration visitBooleanLiteral(BooleanLiteral bool, Environment arg) {
        return null;
    }

    @Override
    public Declaration visitNullLiteral(NullLiteral nl, Environment arg) {
        return null;
    }

    private ClassDeclList addPredefinedNames() {

        // cannot be custom class for these. i think this is fullfilled automatically 


        Token t;
        Identifier id;
        TypeDenoter typeDenoter;
        FieldDecl fieldDecl;
        ParameterDecl pd;
        ParameterDeclList pdl = new ParameterDeclList();
        StatementList sl = new StatementList();
        MethodDecl methodDecl;
        FieldDeclList fieldDeclList = new FieldDeclList();
        MethodDeclList methodDeclList = new MethodDeclList();
        ClassDecl classDecl;
        ClassDeclList classDeclList = new ClassDeclList();

        //class System { public static _PrintStream out; }

        t = new Token(TokenType.IDENTIFIER, "_PrintStream", 0,0);
        id = new Identifier(t);
        typeDenoter = new ClassType(id, null);
        fieldDecl = new FieldDecl(false, true, typeDenoter, "out", new SourcePosition(0,0));
        fieldDeclList.add(fieldDecl);
        classDecl = new ClassDecl("System", fieldDeclList, methodDeclList, new SourcePosition(0, 0));
        classDeclList.add(classDecl);

        // class _PrintStream { public void println( int n ){} }
        methodDeclList = new MethodDeclList();
        fieldDeclList = new FieldDeclList();

        typeDenoter = new BaseType(TypeKind.VOID, null);
        fieldDecl = new FieldDecl(false, false, typeDenoter, "println", new SourcePosition(0,0));
        typeDenoter = new BaseType(TypeKind.INT, null);
        pd = new ParameterDecl(typeDenoter, "n", null);
        pdl.add(pd);
        methodDecl = new MethodDecl(fieldDecl, pdl, sl,null);
        methodDeclList.add(methodDecl);
        classDecl = new ClassDecl("_PrintStream", fieldDeclList, methodDeclList, new SourcePosition(0, 0));
        classDeclList.add(classDecl);

        // class String { }

        methodDeclList = new MethodDeclList();
        fieldDeclList = new FieldDeclList();
        classDecl = new ClassDecl("String", fieldDeclList, methodDeclList, null);
        classDeclList.add(classDecl);

        return classDeclList;
    }
}