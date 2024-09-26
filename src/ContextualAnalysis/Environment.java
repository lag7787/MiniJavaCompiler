package miniJava.ContextualAnalysis;

public class Environment {

    boolean isStatic;
    String className;

    public Environment(String className, boolean isStatic) {
        this.className = className;
        this.isStatic = isStatic;
    }

    public Environment(String className) {
        this.className = className;
    }
}
