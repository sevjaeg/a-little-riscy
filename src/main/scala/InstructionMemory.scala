/*
 *
 */

import chisel3._
import chisel3.util._

class InstructionMemory extends Module {
    val io = IO(new IMemIO)
    val mem = Reg(Vec(128, UInt(32.W)))  // no reset

    val fileName = "test_sw/instructions.txt"
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
    val address = Input(UInt(6.W))
    val value = Output(UInt(32.W))
}

class IMemIO() extends Bundle {
    val port1 = new IMemSingleIO()
    val port2 = new IMemSingleIO()
}