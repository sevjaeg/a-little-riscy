/*
 *
 */

import chisel3._
import chisel3.util._

class ReservationStationAlu(depth: Int) extends Module {
    val io = IO(new Bundle {
        val out = Flipped(new AluInIO())
        val operation = Input(new ReservationStationAluInstruction())
        val CDBin = new CDBInIO()
        val busy = Output(Bool())
    })

    val entries = Vec(depth, Reg(new ReservationStationAluInstruction))
    val entries_current = 0.U

    val busy = Wire(Bool())
    busy := entries_current =/= depth.U
    io.busy := busy

    when(!busy) {
        val newInstruction = io.operation.func
        for (idx <- 0 to depth) {
            when(entries_current === idx.U) {
                entries(idx) := newInstruction
            }
        }
    }

    // Check if new value available on CDB
    when(io.CDBin.rd =/= 0.U) {
        for (idx <- 0 to depth) {
            val entry = entries(idx)
            // Check if new value applicable to operand 1
            when(entry.op1.address === io.CDBin.rd) {
                entry.op1.value := io.CDBin.value
                entry.op1.address := 0.U
            }
            // Check if new value applicable to operand 2
            when(entry.op2.address === io.CDBin.rd) {
                entry.op2.value := io.CDBin.value
                entry.op2.address := 0.U
            }
        }
    }

    for (idx <- 0 to depth) {
        val entry = entries(idx)
        when(entry.func =/= 0.U && entry.op1.address === 0.U && entry.op2.address === 0.U) {
            // TODO instruction to ALU
            io.out.function := entry.func
            io.out.in1 := entry.op1
            io.out.in2 := entry.op2
            io.out.rd := entry.rd
            // TODO ... other inputs? Edit in ALU
        }
    }
}

class AluOperand() extends Bundle {
    val value = UInt(32.W)
    val address = UInt(5.W)
}

class ReservationStationAluInstruction() extends Bundle {
    val rd = UInt(5.W)
    val op1 = new AluOperand()
    val op2 = new AluOperand()
    val func = UInt(4.W)
}