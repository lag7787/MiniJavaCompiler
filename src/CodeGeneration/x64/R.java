package miniJava.CodeGeneration.x64;

import java.io.ByteArrayOutputStream;

public class R {
	private ByteArrayOutputStream _b;
	private boolean rexW = false;
	private boolean rexR = false;
	private boolean rexX = false;
	private boolean rexB = false;
	
	public boolean getRexW() {
		return rexW;
	}
	
	public boolean getRexR() {
		return rexR;
	}
	
	public boolean getRexX() {
		return rexX;
	}
	
	public boolean getRexB() {
		return rexB;
	}
	
	public byte[] getBytes() {
		_b = new ByteArrayOutputStream();
		// construct
		if( rdisp != null && ridx != null && r != null )
			Make(rdisp,ridx,mult,disp,r);
		else if( ridx != null && r != null )
			Make(ridx,mult,disp,r);
		else if( rdisp != null && r != null )
			Make(rdisp,disp,r);
		else if( rm != null && r != null )
			Make(rm,r);
		else if( r != null )
			Make(disp,r);
		else throw new IllegalArgumentException("Cannot determine ModRMSIB");
		
		return _b.toByteArray();
	}
	
	private Reg64 rdisp = null, ridx = null;
	private Reg rm = null, r = null;
	private int disp = 0, mult = 0;
	
	// [rdisp+ridx*mult+disp],r32/64
	public R(Reg64 rdisp, Reg64 ridx, int mult, int disp, Reg r) {
		SetRegR(r);
		SetRegDisp(rdisp);
		SetRegIdx(ridx);
		SetDisp(disp);
		SetMult(mult);
	}
	
	// r must be set by some mod543 instruction set later
	// [rdisp+ridx*mult+disp]
	public R(Reg64 rdisp, Reg64 ridx, int mult, int disp) {
		SetRegDisp(rdisp);
		SetRegIdx(ridx);
		SetDisp(disp);
		SetMult(mult);
	}
	
	// [rdisp+disp],r
	public R(Reg64 rdisp, int disp, Reg r) {
		SetRegDisp(rdisp);
		SetRegR(r);
		SetDisp(disp);
	}
	
	// r will be set by some instruction to a mod543
	// [rdisp+disp]
	public R(Reg64 rdisp, int disp) {
		SetRegDisp(rdisp);
		SetDisp(disp);
	}
	
	// rm64,r64
	public R(Reg64 rm, Reg r) {
		SetRegRM(rm);
		SetRegR(r);
	}
	
	// rm or r
	public R(Reg64 r_or_rm, boolean isRm) {
		if( isRm )
			SetRegRM(r_or_rm);
		else
			SetRegR(r_or_rm);
	}
	
	public int getRMSize() {
		if( rm == null ) return 0;
		return rm.size();
	}
	
	//public ModRMSIB() {
	//}
	
	public void SetRegRM(Reg rm) {
		if( rm.getIdx() > 7 ) rexB = true;
		rexW = rexW || rm instanceof Reg64;
		this.rm = rm;
	}
	
	public void SetRegR(Reg r) {
		if( r.getIdx() > 7 ) rexR = true;
		rexW = rexW || r instanceof Reg64;
		this.r = r;
	}
	
	public void SetRegDisp(Reg64 rdisp) {
		if( rdisp.getIdx() > 7 ) rexB = true;
		this.rdisp = rdisp;
	}
	
	public void SetRegIdx(Reg64 ridx) {
		if( ridx.getIdx() > 7 ) rexX = true;
		this.ridx = ridx;
	}
	
	public void SetDisp(int disp) {
		this.disp = disp;
	}
	
	public void SetMult(int mult) {
		this.mult = mult;
	}
	
	public boolean IsRegR_R8() {
		return r instanceof Reg8;
	}
	
	public boolean IsRegR_R64() {
		return r instanceof Reg64;
	}
	
	public boolean IsRegRM_R8() {
		return rm instanceof Reg8;
	}
	
	public boolean IsRegRM_R64() {
		return rm instanceof Reg64;
	}
	
	// rm,r
	private void Make(Reg rm, Reg r) {
		int mod = 3;
		
		// this maps properly, these are plain register operands 
		// isn't it 32 bits... how does this make sen
		int regByte = ( mod << 6 ) | ( getIdx(r) << 3 ) | getIdx(rm); // gives two hex digits which are then written to the buffer
		_b.write( regByte ); 
	}
	
	// [rdisp+disp],r
	private void Make(Reg64 rdisp, int disp, Reg r) {
		// TODO: construct the byte and write to _b
		// Operands: [rdisp+disp],r

		int mod = makeMod(rdisp, disp);
		int regByte = ( mod << 6 ) | ( getIdx(r) << 3 ) | (getIdx(rdisp));
		_b.write( regByte ); 
		if (rdisp == Reg64.RSP) {
			int sibByte = (0 << 6) | 4 << 3 | 4;
			_b.write(sibByte);
		}
		for (int i = 0 ; i < getByteLength(disp); i++) {
			_b.write(disp); // and I should be writing more based on length right? 
			disp = disp << 8;
		}
	}
	
	// [ridx*mult+disp],r
	// what is the base if not rdisp? is it default rbp? 
	// the base is dependent on the mod bits? 
	// no rdisp requires 0 mod 
	private void Make( Reg64 ridx, int mult, int disp, Reg r ) {
		if( !(mult == 1 || mult == 2 || mult == 4 || mult == 8) )
			throw new IllegalArgumentException("Invalid multiplier value: " + mult);
		if( ridx == Reg64.RSP )
			throw new IllegalArgumentException("Index cannot be rsp");
		
		// TODO: construct the modrm byte and SIB byte
		// Operands: [ridx*mult + disp], r
		int mod = 0;
		int regByte = (mod << 6) | ( getIdx(r) << 3 ) | 4;
		int ss = makeSS(mult);
		int sibByte = (ss << 6) | ( getIdx(ridx) << 3 )| 5;
		//int bitLength = Integer.SIZE - Integer.numberOfLeadingZeros(disp);
		_b.write(regByte);
		_b.write(sibByte);
		writeInt(_b, disp); //always gonna write a 32 bit / 4 byte int 
	}
	// i think there will have to be some testing for hte RBP cases
	
	// [rdisp+ridx*mult+disp],r
	private void Make( Reg64 rdisp, Reg64 ridx, int mult, int disp, Reg r ) {
		if( !(mult == 1 || mult == 2 || mult == 4 || mult == 8) )
			throw new IllegalArgumentException("Invalid multiplier value: " + mult);
		if( ridx == Reg64.RSP )
			throw new IllegalArgumentException("Index cannot be rsp");
		
		// TODO: construct the modrm byte and SIB byte
		// Operands: [rdisp + ridx*mult + disp], r
		int mod = makeMod(rdisp, disp);
		int regByte = (mod << 6) | ( getIdx(r) << 3 ) | 4;
		int ss = makeSS(mult);
		int sibByte = (ss << 6) | ( getIdx(ridx) << 3 )| getIdx(rdisp);
		_b.write(regByte);
		_b.write(sibByte);
		for (int i = 0 ; i < getByteLength(disp); i++) { // i still think theres something wrong with how we right disp
			_b.write(disp); // and I should be writing more based on length right? 
			disp = disp >> 8; // write one byte at a time
		}
	}
	
	// [disp],r
	private void Make( int disp, Reg r ) {
		// why no mod here? 
		_b.write( ( getIdx(r) << 3 ) | 4 );
		_b.write( ( 4 << 3 ) | 5 ); // ss doesn't matter
		writeInt(_b,disp);
	}
	
	private int getIdx(Reg r) {
		return x64.getIdx(r); // returns the index of the register?
	}
	
	// TODO: This is a duplicate declaration from x64.writeInt
	//  You should remove this, but the reason it is here is so that
	//  you can immediately see what it does, and so you know what
	//  is available to you in the x64 class.
	private void writeInt(ByteArrayOutputStream b, int n) {
		for( int i = 0; i < 4; ++i ) {
			b.write( n & 0xFF );
			n >>= 8; // java ints are 4 bytes
		}
	}

	private int makeMod(Reg64 rdisp, int disp) {
		int mod;
		int bitLength = Integer.SIZE - Integer.numberOfLeadingZeros(disp);

		if (bitLength > 8) {
			mod = 2;
		} else if (bitLength > 0) {
			mod = 1;
		} else {
			mod = 0;	
		}
		// if mod is 2 and rdisp is RIP
		if (mod == 2 && rdisp == Reg64.RIP){
			mod = 0;
		}

		return mod;
	}

	private int makeSS(int mult) {
		if (mult == 1) {
			return 0;
		} else if (mult == 2) {
			return 1;
		} else if (mult == 4) {
			return 2;
		} else {
			return 3;
		}

	}

	private int getByteLength(int num) {
		int bitLength = Integer.SIZE - Integer.numberOfLeadingZeros(disp);
		int byteLength = bitLength / 8;
		if (bitLength % 8 != 0) {
			byteLength++;
		}
		return byteLength;
	}
}
