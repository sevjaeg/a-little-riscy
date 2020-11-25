/*
 *
 */

import chisel3._
import chisel3.util._

class RegisterSingleReadIO() extends Bundle {
    val address = Input(UInt(32.W))
    val value = Output(UInt(5.W))
}

class RegisterReadIO() extends Bundle {
    val r1 = new RegisterSingleReadIO()
    val r2 = new RegisterSingleReadIO()
}

class RegisterPortIO() extends Bundle {
    val read = new RegisterReadIO()
    val write = Flipped(new RegisterSingleReadIO())
}

// TODO deal with instructions using less registers (is it fine to just use address 0?)

class Registers extends Module {
    val io = IO(new Bundle {
        val portAlu = new RegisterPortIO()
        val portLoadStore = new RegisterPortIO()
        val portDebug = new RegisterSingleReadIO()
        val pc = Output(UInt(32.W))
        val newPc = Input(UInt(32.W))
    })

    val registers = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))  // x0-x31
    val pc = RegInit(0.U(32.W))

    // New values
    registers(0) := 0.U
    pc := io.newPc

    // TODO prevent write to the same location
    for(i <- 1 to 31) {
        when(io.portAlu.write.address === i.U) {
            registers(i) := io.portAlu.write.value
        } .elsewhen (io.portLoadStore.write.address === i.U) {
            registers(i) := io.portLoadStore.write.value
        } .otherwise {
            registers(i) := registers(i)
        }
    }

    io.portAlu.read.r1.value := registers(io.portAlu.read.r1.address)
    io.portAlu.read.r2.value := registers(io.portAlu.read.r2.address)

    io.portLoadStore.read.r1.value := registers(io.portLoadStore.read.r1.address)
    io.portLoadStore.read.r2.value := registers(io.portLoadStore.read.r2.address)

    io.portDebug.value := registers(io.portDebug.address)

    io.pc := pc
}