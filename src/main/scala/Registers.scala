/*
 *
 */

import chisel3._
import chisel3.util._


class RegisterPortIO() extends Bundle {
    val rdValue = Input(UInt(32.W))
    val rdAddress = Input(UInt(5.W))
    val r1Address = Input(UInt(5.W))
    val r2Address = Input(UInt(5.W))
    val r1 = Output(UInt(32.W))
    val r2 = Output(UInt(32.W))
}

// TODO deal with instructions using less registers (is it fine to just use address 0?)

class Registers extends Module {
    val io = IO(new Bundle {
        val portAlu = new RegisterPortIO()
        val portLoadStore = new RegisterPortIO()
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
        when(io.portAlu.rdAddress === i.U) {
            registers(i) := io.portAlu.rdValue
        } .elsewhen (io.portLoadStore.rdAddress === i.U) {
            registers(i) := io.portLoadStore.rdValue
        } .otherwise {
            registers(i) := registers(i)
        }
    }

    io.portAlu.r1 := registers(io.portAlu.r1Address)
    io.portAlu.r2 := registers(io.portAlu.r2Address)
    io.portLoadStore.r1 := registers(io.portLoadStore.r1Address)
    io.portLoadStore.r2 := registers(io.portLoadStore.r2Address)
    io.pc := pc
}