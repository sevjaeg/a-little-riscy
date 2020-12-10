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
        val queue = new DecoupledIO(UInt(128.W))
    })
    val pc = io.pcIn
    val pc1 = Wire(UInt(32.W))
    val pc2 = Wire(UInt(32.W))
    val pcOut = Wire(UInt(32.W))
    val queueReady = io.queue.ready

    when(queueReady === true.B) {
        when(pc > 127.U) { // instruction memory size  TODO update
            pc1 := 0.U
            pc2 := 0.U
            pcOut := pc
        }.otherwise {
            pc1 := pc
            pc2 := pc + 1.U
            pcOut := pc2 + 1.U
        }
    } .otherwise {
        pc1 := pc
        pc2 := pc + 1.U
        pcOut := pc
    }

    io.IMem.port1.address := pc1
    io.IMem.port2.address := pc2
    io.pcOut := pcOut
    io.queue.bits := Cat(pc1, io.IMem.port1.value, pc2, io.IMem.port2.value)
    io.queue.valid := true.B
}