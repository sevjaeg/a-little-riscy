/*
 *
 */

import chisel3._
import chisel3.util._

class IMemSingleIO() extends Bundle {
    val address = Input(UInt(6.W))
    val value = Output(UInt(32.W))
}

class IMemIO() extends Bundle {
    val port1 = new IMemSingleIO()
    val port2 = new IMemSingleIO()
}

class InstructionMemory extends Module {
    val io = IO(new IMemIO)

    val mem = Reg(Vec(64, UInt(32.W)))  // no reset

    mem(0) := "h0000000F".U
    mem(1) := "h00000005".U
    mem(2) := "h00000007".U
    mem(3) := "h00000006".U
    mem(4) := "h00000003".U
    mem(5) := "h00000001".U
    mem(6) := "h00000006".U

    io.port1.value := mem(io.port1.address)
    io.port2.value := mem(io.port2.address)
}