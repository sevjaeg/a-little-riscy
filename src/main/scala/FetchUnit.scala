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
    /*val memI = Wire(Vec(6, UInt(32.W)))
    memI(0) := "h00000001".U
    memI(1) := "h00000002".U
    memI(2) := "h00000003".U
    memI(3) := "h00000004".U
    memI(4) := "h00000005".U
    memI(5) := "h00000006".U*/

    // TODO check pc validity

    io.addr1 := pc
    io.addr2 := pc + 1.U
    io.pcOut := pc + 2.U
    io.i1 := io.val1
    io.i2 := io.val2
}