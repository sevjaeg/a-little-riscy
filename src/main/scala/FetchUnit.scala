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
        val i1 = Output(UInt(32.W))
        val i2 = Output(UInt(32.W))
    })
    val pc = io.pcIn

    // TODO check pc validity

    io.IMem.port1.address := pc
    io.IMem.port2.address := pc + 1.U
    io.pcOut := pc + 1.U  // TODO 2.U
    io.i1 := io.IMem.port1.value
    io.i2 := 0.U // TODO io.IMem.port2.value
}