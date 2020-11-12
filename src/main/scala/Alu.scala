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

    //TODO fix bit widths after shift (log vs. arithm)
    when(function === 1.U){  // logical shift left
        result := in1 << in2(5,0)
    } .elsewhen(function === 2.U) {  // logical shift right
        result := in1 >> in2(5,0)
    } .elsewhen(function === 3.U) { // aritmethic shift right
        //TODO
        result := in1 >> in2(5,0)
    } .elsewhen(function === 4.U) {  // addition
        result := in1 + in2
    } .elsewhen(function === 5.U) { // subtraction
        result := in1 - in2
    } .elsewhen(function === 6.U) {  // set less than
        result := in1 < in2
    } .elsewhen(function === 7.U) { // OR
        result := in1 | in2
    } .elsewhen(function === 8.U) {  // XOR
        result := in1 ^ in2
    } .elsewhen(function === 9.U) { // AND
        result := in1 & in2
    } .otherwise {
        result := 0.U
    }

    io.result := result
}