/*
 *
 */

import chisel3._
import chisel3.util._


class FetchUnit extends Module {
    val io = IO(new Bundle {
        val pcIn = Input(UInt(32.W))
        val pcOut = Output(UInt(32.W))
        val IMem = Flipped(new IMemIO())
        val i1 = Output(UInt(64.W))  // Cat (pc1, instruction1)
        val i2 = Output(UInt(64.W))  // Cat (pc2, instruction1)
    })
    val pc = io.pcIn

    // TODO check pc validity
    val pc1 = Wire(UInt(32.W))
    val pc2 = Wire(UInt(32.W))
    pc1 := pc
    pc2 := pc + 1.U

    io.IMem.port1.address := pc1
    io.IMem.port2.address := pc2
    io.pcOut := pc1 + 1.U  // TODO 2.U
    io.i1 := Cat(pc1, io.IMem.port1.value)
    io.i2 := Cat(pc2, io.IMem.port2.value) // TODO io.IMem.port2.value
}