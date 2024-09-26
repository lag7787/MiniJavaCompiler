package miniJava.ContextualAnalysis;
import java.util.Hashtable;
import java.util.Map;
import java.util.Stack;

import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.AbstractSyntaxTrees.Identifier;

public class ScopedIdentification {

    private Stack<Map<String, Declaration>> IDTables;

    public ScopedIdentification() {
        this.IDTables = new Stack<Map<String, Declaration>>();
    }

    public void openScope() {
        this.IDTables.push(new Hashtable<String, Declaration>());
    }

    public Map<String, Declaration> closeScope() {
        return this.IDTables.pop();
    }

    /*
     * str - a String variable representing the fully-formed string to act as a key for the
     * declaration
     * 
     * decl - a declaration for that contextualized string
     */

    public void addDeclaration(String str, Declaration decl) throws IdentificationError {
        // It should throw an IdentificationError if that name already exists at that level or level 2+. 
        /// hopw do i know if i'm in level two plus
        // level is size - 1
        Stack<Map<String, Declaration>> poppedTables = new Stack<Map<String, Declaration>>();
        int level = this.IDTables.size() - 1;

        if (level < 2) {
            // checking only current level if level is 0 or 1
            if (IDTables.peek().containsKey(str)){
                throw new IdentificationError();
            }
        } else {
            // checking all levels greater than or equal to 2
            while (level >= 2) {
                poppedTables.push(this.IDTables.pop());
                if (poppedTables.peek().containsKey(str)) {
                    throw new IdentificationError();
                }
                level--;
            }
            // refilling IDTables
            while (poppedTables.size() != 0) {
                this.IDTables.push(poppedTables.pop());
            }
        }

        // if no IdentificaitonError was thrown, this will add a new decl to the topmost hashtable
        this.IDTables.peek().put(str, decl);
    }

    /*
     * identifier - an Identifier type that we use to lookup the delcaration in the IDTable
     * 
     * context - an optional parameter used to qualify the context of a lookup (usually a classname)
     */
    public Declaration findDeclaration(String identifier, String context, boolean explicitFieldLookup, boolean explicitClassLookup) {
        // look throug the whole table? 
        Stack<Map<String, Declaration>> poppedTables = new Stack<Map<String, Declaration>>();
        StringBuilder lookupBuilder = new StringBuilder();
        String contextualizedLookupString;
        int level = this.IDTables.size() - 1;
        Declaration rv = null;
        boolean found;
        String key;
        
        // should we check the identfier first to see if it has a decl?
        // also where are we assigning the decl to it. 
        // i think it should be done in here

        // form lookup string
        if (context != null) {
            lookupBuilder.append(context + ".");
        }
        lookupBuilder.append(identifier);
        contextualizedLookupString = lookupBuilder.toString();


        while (level >= 0) {
            poppedTables.push(this.IDTables.pop());

            if (explicitFieldLookup){
                found = poppedTables.peek().containsKey(contextualizedLookupString);
                key = contextualizedLookupString;

            } else {

                if (level == 1 & !explicitClassLookup) {
                    found = poppedTables.peek().containsKey(contextualizedLookupString);
                    key = contextualizedLookupString;
                } else  {
                    found = poppedTables.peek().containsKey(identifier);
                    key = identifier;
                }

            }

            if (found){
                rv = poppedTables.peek().get(key);
                break;
            }
            level--;
        }

        while (poppedTables.size() != 0) {
            this.IDTables.push(poppedTables.pop());
        }

        return rv;
    }
    
}
