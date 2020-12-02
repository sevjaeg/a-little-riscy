/*
 *
 *
 * Only supports aligned access
 */

import Chisel.Cat
import chisel3._

class Memory extends Module {
    val io = IO(new MemoryIO())
    val mem = new Array[Mem[UInt]](4)
    for (idx <- 0 to 3) {
        mem(idx) = Mem(256, UInt(8.W))  // 8 bit address, so 2^8 entries
    }

    when(io.write) {
        for (idx <- 0 to 3) {
            when(io.writeMask(idx)) {
                mem(idx).write(io.address, io.dataIn((idx + 1) * 8 - 1, idx * 8))
            }
        }
        io.dataOut := 0.U
    } .otherwise {
        val data = new Array[UInt](4)
        for (idx <- 0 to 3) {
            data(idx) = mem(idx).read(io.address)
        }
        io.dataOut := Cat(data(3), data(2), data(1), data(0))
    }
}

class MemoryIO() extends Bundle {
    val dataIn = Input(UInt(32.W))
    val dataOut = Output(UInt(32.W))
    val address = Input(UInt(8.W))
    val write = Input(Bool())
    val writeMask = Input(Vec(4, Bool()))
}