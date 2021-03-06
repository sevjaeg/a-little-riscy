/*
 *
 */

import chisel3._
import chisel3.util._

class InstructionMemory extends Module {
    val I_MEM_SIZE = 128
    val io = IO(new IMemIO)
    val mem = Reg(Vec(I_MEM_SIZE, UInt(32.W)))  // no reset

    // Read instructions from file
    val fileName = "test_sw/instructions_alu.txt"
    val bufferedSource = scala.io.Source.fromFile(fileName)
    var i = 0
    for (lineRaw <- bufferedSource.getLines()) {
        val line : String = lineRaw.replaceAll(" ", "")
        if (line.length() > 14) {
            if (line(5) == ':' && line.substring(0, 5) != "XXXXX") {
                val instruction: String = "h".concat(line.substring(6, 14))
                mem(i) := instruction.U
                i = i + 1
                // println(instruction)
            }
        }
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