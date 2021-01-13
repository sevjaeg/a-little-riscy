/*
 *
 */

import chisel3._
import chisel3.util._

// First instruction is always 0!

class InstructionMemory extends Module {
    val I_MEM_SIZE = 128
    val io = IO(new IMemIO)
    val mem = Reg(Vec(I_MEM_SIZE, UInt(32.W)))  // no reset

    // Read instructions from file
    val fileName = "test_sw/instructions_alu.txt"
    val bufferedSource = scala.io.Source.fromFile(fileName)
    var i = 0
    for (lines <- bufferedSource.getLines()) {
        mem(i) := ("h".concat(lines)).U
        i = i + 1
    }
    bufferedSource.close()

    io.port1.value := mem(io.port1.address)
    io.port2.value := mem(io.port2.address)
}

class IMemSingleIO() extends Bundle {
    val address = Input(UInt(7.W))
    val value = Output(UInt(32.W))
}

class IMemIO() extends Bundle {
    val port1 = new IMemSingleIO()
    val port2 = new IMemSingleIO()
}