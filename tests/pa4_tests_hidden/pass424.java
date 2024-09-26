/**
 * COMP 520
 * deallocation of VarDecl
 */
class MainClass {
    public static void main (String [] args) {
        int tstvar = 24;
        
        int i = 0;
        while (i < 30720000) {
            int j = i;
            i = i + 1;
        }
        
        System.out.println(tstvar + 48 - 22);
		System.out.println(tstvar + 48 - 20);
    }
}


