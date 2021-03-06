/*
 *
 */

import chisel3._
import chisel3.util._

class AluInDispatcherIO() extends Bundle {
    val function = Input(UInt(4.W))
    val in1 = Input(UInt(32.W))
    val in2 = Input(UInt(32.W))
    val rd = Input(UInt(5.W))

    val in1Select = Input(UInt(2.W))
    val in2Select = Input(UInt(2.W))
    val immediate = Input(UInt(20.W))
    val pc = Input(UInt(32.W))
}

class AluInForwardingIO() extends Bundle {
    val aluFwd = Input(UInt(32.W))
    val lsuFwd = Input(UInt(32.W))
}

class AluOutIO() extends Bundle {
    val value = Output(UInt(32.W))
    val rd = Output(UInt(5.W))
}

class Alu extends Module {
    val io = IO(new Bundle {
        val in = new AluInDispatcherIO()
        val inFwd = new AluInForwardingIO()
        val out = new AluOutIO()
    })

    val function = io.in.function
    val in1 = WireDefault(0.U(32.W))
    val in2 = WireDefault(0.U(32.W))
    val imm = WireDefault(0.S(32.W))

    // Multiplexer for input 1: Register, aluForwarding, lsuForwarding or PC
    switch(io.in.in1Select) {
        is("b00".U) {in1 := io.in.in1}             // Register
        is("b10".U) {in1 := io.inFwd.aluFwd}       // aluForwarding
        is("b11".U) {in1 := io.inFwd.lsuFwd}       // lsuForwarding
        is("b01".U) {in1 := io.in.pc}              // PC
    }

    // Multiplexer for input 2: Register, aluForwarding, lsuForwarding or immediate
    switch(io.in.in2Select) {
        is("b00".U) {in2 := io.in.in2}             // Register
        is("b10".U) {in2 := io.inFwd.aluFwd}       // aluForwarding
        is("b11".U) {in2 := io.inFwd.lsuFwd}       // lsuForwarding
        is("b01".U) {imm := (io.in.immediate).asSInt()
                     in2 := imm.asUInt()}       // immediate (sign-extended)
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
        result := (in2(19, 0) << 12.U)(31,0)
    } .elsewhen(function === "b0100".U) { // add upper immediate to pc: AUIPC
        result := in1 + (in2(19, 0) << 12.U)(31,0)
    } .otherwise {  // NOP (including fn = 0000)
        result := 0.U
    }

    val resultRegister = RegInit(0.U(32.W))
    resultRegister := result
    io.out.value := resultRegister

    val rdRegister =  RegInit(0.U(5.W))
    rdRegister := io.in.rd
    io.out.rd := rdRegister
}
