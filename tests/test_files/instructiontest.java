package miniJava.test_files;

import miniJava.CodeGeneration.x64.R;
import miniJava.CodeGeneration.x64.Reg64;
import miniJava.CodeGeneration.x64.ISA.Add;
import miniJava.CodeGeneration.x64.ISA.Cmp;
import miniJava.CodeGeneration.x64.ISA.Mov_ri64;
import miniJava.CodeGeneration.x64.ISA.Mov_rmi;
import miniJava.CodeGeneration.x64.ISA.Mov_rmr;
import miniJava.CodeGeneration.x64.ISA.Mov_rrm;
import miniJava.CodeGeneration.x64.ISA.Pop;
import miniJava.CodeGeneration.x64.ISA.Push;

public class instructiontest {

    public static void main (String[] args) {

        // create simple two register move instruction .... 

        Reg64 rax = Reg64.RAX;
        Reg64 rcx = Reg64.RCX;
        Reg64 rsi = Reg64.RSI;
        Reg64 r12 = Reg64.R12;
        Reg64 rsp = Reg64.RSP;
        //R modrmsib = new R(r12, false);
        //modrmsib.SetRegIdx(rax);
        //modrmsib.SetMult(4);
        //modrmsib.SetDisp(16);
        //R modrmsib = new R(rax, rcx);
        //R modrmsib = new R(16, rax);
        R modrmsib = new R(Reg64.R10, Reg64.R8, 8, 2222, Reg64.R11);
        //modrmsib.SetRegR(rcx);
        //Mov_rmr mov = new Mov_rmr(modrmsib);
        Mov_rrm mov = new Mov_rrm(modrmsib);
        //Mov_ri64 mov = new Mov_ri64(r12, 8446744073709551615L);
        //Pop pop = new Pop(rcx);
        //Push push = new Push(0);
        //Cmp cmp = new Cmp(modrmsib);
        //Add add = new Add(modrmsib);

        byte[] bytearr = mov.getBytes();

        for (byte b : bytearr) {
            String st = String.format("%02X", b);
            System.out.print(st);
        }
        System.out.println();
    }


}

//rex op reg sib disp 
//48  89 0c  85  10  00  00 -> assembler output
//48  89 4C  85  10 -> myoutput
//48 89 4c 84 10