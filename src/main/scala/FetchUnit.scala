/*
 *
 */

import chisel3._
import chisel3.util._


class FetchUnit extends Module {
    val io = IO(new Bundle {
        val pcIn = Input(UInt(32.W))
        val pcOut = Output(UInt(32.W))
        val IMemPort1 = Flipped(new IMemIO())
        val IMemPort2 = Flipped(new IMemIO())
        val i1 = Output(UInt(32.W))
        val i2 = Output(UInt(32.W))
    })
    val pc = io.pcIn

    // TODO check pc validity

    io.IMemPort1.address := pc
    io.IMemPort2.address := pc + 1.U
    io.pcOut := pc + 1.U  // TODO 2.U
    io.i1 := io.IMemPort1.value
    io.i2 := 0.U // TODO io.IMemPort2.value
}