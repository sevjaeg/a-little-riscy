/*
 *
 */

import chisel3._
import chisel3.util._

class InstructionMemory extends Module {
    val io = IO(new IMemIO)

    val mem = Reg(Vec(64, UInt(32.W)))  // no reset

    mem(0) := 0.U
    mem(1) := "h00578793".U   //add 5 to r15
    mem(2) := "h00770713".U   //add 7 to r14
    mem(4) := "hE787B3".U   // add r14 + r15 to r 15


    io.port1.value := mem(io.port1.address)
    io.port2.value := mem(io.port2.address)
}

class IMemSingleIO() extends Bundle {
    val address = Input(UInt(6.W))
    val value = Output(UInt(32.W))
}

class IMemIO() extends Bundle {
    val port1 = new IMemSingleIO()
    val port2 = new IMemSingleIO()
}