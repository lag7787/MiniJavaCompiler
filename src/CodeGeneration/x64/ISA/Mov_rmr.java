package miniJava.CodeGeneration.x64.ISA;

import miniJava.CodeGeneration.x64.Instruction;
import miniJava.CodeGeneration.x64.R;
import miniJava.CodeGeneration.x64.x64;

public class Mov_rmr extends Instruction {
	// rm,r variants
	public Mov_rmr(R modrmsib) {
		byte[] modrmsibBytes = modrmsib.getBytes();
		importREX(modrmsib); // so we can assume the rex is handled for us? 
		opcodeBytes.write(0x89);
		x64.writeBytes(immBytes,modrmsibBytes);
	}
}
