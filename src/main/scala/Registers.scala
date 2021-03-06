/*
 *
 */

import chisel3._
import chisel3.util._

class Registers extends Module {
    val io = IO(new Bundle {
        val portFetchUnit = new RegisterPortIO()
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

    registers := registers
    when(io.portAlu.write.rd =/= 0.U) {
        registers(io.portAlu.write.rd) := io.portAlu.write.value
    }
    when(io.portLoadStore.write.rd =/= 0.U) {
        registers(io.portLoadStore.write.rd) := io.portLoadStore.write.value
    }


    io.portAlu.read.r1.value := registers(io.portAlu.read.r1.rd)
    io.portAlu.read.r2.value := registers(io.portAlu.read.r2.rd)

    io.portFetchUnit.read.r1.value := registers(io.portFetchUnit.read.r1.rd)
    io.portFetchUnit.read.r2.value := registers(io.portFetchUnit.read.r2.rd)

    io.portLoadStore.read.r1.value := registers(io.portLoadStore.read.r1.rd)
    io.portLoadStore.read.r2.value := registers(io.portLoadStore.read.r2.rd)

    io.pc := pc
}

class RegisterSingleReadIO() extends Bundle {
    val rd = Input(UInt(5.W))
    val value = Output(UInt(32.W))
}

class CDBInIO() extends Bundle {
    val rd = Input(UInt(5.W))
    val value = Input(UInt(32.W))
}

class RegisterDualReadIO() extends Bundle {
    val r1 = new RegisterSingleReadIO()
    val r2 = new RegisterSingleReadIO()
}

class RegisterPortIO() extends Bundle {
    val read = new RegisterDualReadIO()
    val write = new CDBInIO()
}