/*
 *
 */

import chisel3._
import chisel3.util._


class FetchUnit extends Module {
    val io = IO(new Bundle {
        val pcIn = Input(UInt(32.W))
        val pcOut = Output(UInt(32.W))
        val addr1 = Output(UInt(8.W))
        val addr2 = Output(UInt(8.W))
        val val1 = Input(UInt(32.W))
        val val2 = Input(UInt(32.W))
        val i1 = Output(UInt(32.W))
        val i2 = Output(UInt(32.W))
    })
    val pc = io.pcIn

    // TODO check pc validity

    io.addr1 := pc
    io.addr2 := pc + 1.U
    io.pcOut := pc + 2.U
    io.i1 := io.val1
    io.i2 := io.val2
}