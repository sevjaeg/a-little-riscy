/*
 *
 */

import Chisel.Cat
import chisel3._

class MemoryIO() extends Bundle {
    val dataIn = Input(UInt(32.W))
    val dataOut = Output(UInt(32.W))
    val address = Input(UInt(8.W))
    val write = Input(Bool())
    val writeMask = Input(Vec(4, Bool()))
}

class Memory extends Module {
    val io = IO(new MemoryIO())
    val mem = Mem(64, Vec(4, UInt(8.W)))

    when(io.write) {
        val data = Wire(Vec(4, UInt(8.W)))
        for (idx <- 0 to 3) {
            data(idx) := io.dataIn((idx+1) * 8 - 1, idx * 8)
        }
        mem.write(io.address, data, io.writeMask)
        io.dataOut := 0.U
    } .otherwise {
        val data = mem.read(io.address)
        io.dataOut := Cat(data(3), data(2), data(1), data(0))
    }
}

object MemoryMain extends App {
    println("I will now generate the Verilog file!")
    chisel3.Driver.execute(Array("--target-dir", "generated"), () => new Memory())
}