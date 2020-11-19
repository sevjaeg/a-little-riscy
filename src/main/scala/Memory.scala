/*
 *
 */

import chisel3._

class MemoryIO() extends Bundle {
    val dataIn = Input(UInt(32.W))
    val dataOut = Output(UInt(32.W))
    val address = Input(UInt(8.W))
    val write = Input(Bool())
}

class Memory extends Module {
    val io = IO(new MemoryIO())
    val mem = Mem(64, UInt(32.W))

    when(io.write) {
        mem(io.address) := io.dataIn
        io.dataOut := 0.U
    } .otherwise {
        io.dataOut := mem(io.address)
    }
}

object MemoryMain extends App {
    println("I will now generate the Verilog file!")
    chisel3.Driver.execute(Array("--target-dir", "generated"), () => new Memory())
}