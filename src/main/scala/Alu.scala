/*
 *
 */

import chisel3._
import chisel3.util._

class Alu extends Module {
    val io = IO(new Bundle {
        val function = Input(UInt(4.W))
        val in1 = Input(UInt(32.W))
        val in2 = Input(UInt(32.W))
        val result = Output(UInt(32.W))
    })
    val function = io.function
    val in1 = io.in1
    val in2 = io.in2

    val result = Wire(UInt(32.W))

    // TODO LUI, AUIPC

    when(function === 1.U){  // logical shift left: SLLI
        result := (in1 << in2(4,0))(31,0)
    } .elsewhen(function === 2.U) {  // logical shift right: SRLI
        result := in1 >> in2(4,0)
    } .elsewhen(function === 3.U) { // aritmethic shift right: SRAI
        val in1_signed = in1.asSInt
        result := (in1_signed >> in2(4,0)).asUInt
    } .elsewhen(function === 4.U) {  // addition: ADDI
        result := in1 + in2
    } .elsewhen(function === 5.U) { // subtraction
        result := in1 - in2
    } .elsewhen(function === 6.U) {  // set less than unsigned: SLTIU
        result := in1 < in2
    } .elsewhen(function === 7.U) {  // set less than signed: SLTI
        val in1_signed = in1.asSInt
        val in2_signed = in2.asSInt
        result := in1_signed < in2_signed
    }.elsewhen(function === 8.U) { // OR: ORI
        result := in1 | in2
    } .elsewhen(function === 9.U) {  // XOR: XORI
        result := in1 ^ in2
    } .elsewhen(function === 10.U) { // AND: ANDI
        result := in1 & in2
    } .otherwise {  // NOP
        result := 0.U
    }

    io.result := result
}