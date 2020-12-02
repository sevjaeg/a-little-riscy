/*
 *
 */

import chisel3._
import chisel3.util._

class AluInIO() extends Bundle {
    val function = Input(UInt(4.W))
    val in1 = Input(UInt(32.W))
    val in2 = Input(UInt(32.W))
    val hasImmediate = Input(UInt(1.W))
    val inImmediate = Input(UInt(12.W))
    val rd = Input(UInt(5.W))
}

class AluOutIO() extends Bundle {
    val result = Output(UInt(32.W))
    val rd = Output(UInt(5.W))
}

class Alu extends Module {
    val io = IO(new Bundle {
        val in = new AluInIO()
        val out = new AluOutIO()
    })

    val function = io.in.function
    val in1 = io.in.in1
    val in2 = Wire(UInt(32.W))
    in2 := 0.U

    // Multiplexer: register or immediate
    switch(io.in.hasImmediate) {
        is(true.B) {in2 := io.in.inImmediate}
        is(false.B) {in2 := io.in.in2}
    }
    val result = Wire(UInt(32.W))

    when(function === "b1111".U) {  // addition: ADD(I)
        result := in1 + in2
    } .elsewhen(function === "b0111".U) { // subtraction: SUB
        result := in1 - in2
    } .elsewhen(function === "b1110".U){  // logical shift left: SLL(I)
        result := (in1 << in2(4,0))(31,0)
    } .elsewhen(function === "b1101".U) {  // set less than signed: SLT(I)
        val in1_signed = in1.asSInt
        val in2_signed = in2.asSInt
        result := in1_signed < in2_signed
    } .elsewhen(function === "b1100".U) {  // set less than unsigned: SLT(I)U
        result := in1 < in2
    } .elsewhen(function === "b1011".U) {  // XOR: XOR(I)
        result := in1 ^ in2
    } .elsewhen(function === "b1010".U) {  // logical shift right: SRL(I)
        result := in1 >> in2(4,0)
    } .elsewhen(function === "b0010".U) { // arithmetic shift right: SRA(I)
        val in1_signed = in1.asSInt
        result := (in1_signed >> in2(4,0)).asUInt
    }  .elsewhen(function === "b1001".U) { // OR: ORI
        result := in1 | in2
    }  .elsewhen(function === "b1000".U) { // AND: ANDI
        result := in1 & in2
    } .elsewhen(function === "b0001".U) { // Load upper immediate: LUI
        result := in1 | (in2(19, 0) << 12.U)(31,0)  // TODO in1 = rd
    } .elsewhen(function === "b0100".U) { // add upper immediate to pc: AUIPC
        result := in1 + (in2(19, 0) << 12.U)(31,0)  // TODO in1 = pc of AUIPC instruction
    } .otherwise {  // NOP (including fn = 0000)
        result := 0.U
    }

    val resultRegister = RegInit(0.U(32.W))
    resultRegister := result
    io.out.result := resultRegister

    val rdRegister =  RegInit(0.U(5.W))
    rdRegister := io.in.rd
    io.out.rd := rdRegister
}