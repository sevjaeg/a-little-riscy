/*
 *
 */

import chisel3._
import chisel3.util._


class Dispatcher extends Module {
    val io = IO(new Bundle {
        val instruction = Input(UInt(32.W))
        val aluIn = Flipped(new AluInIO())
        val lsIn = Flipped(new LoadStoreInIO())
    })

    val instruction = io.instruction

    // This should handle all instructions
    // TODO consider renaming
    val opCode = instruction(6, 0)
    val function = instruction(14, 12)
    val rd = instruction(11, 7)
    val r1 = instruction(19, 15)
    val r2 = instruction(24, 20)
    val firstBits = instruction(31, 25)

    when(opCode === "b11".U(7.W)) {
        // Load
    } .elsewhen(opCode === "b100011".U(7.W)) {
        // Store
    } .elsewhen(opCode === "b10011".U(7.W)) {
        // ALU Immediate
    } .elsewhen(opCode === "b110011".U(7.W)) {
        // ALU
    } .elsewhen(opCode === "b0110111".U(7.W)) {
        // LUI
    } .elsewhen(opCode === "b0010111".U(7.W)) {
        // AUIPC
    } .elsewhen(opCode === "b1101111".U(7.W)) {
        // JAL
    } .elsewhen(opCode === "b1100111".U(7.W)) {
        // JALR
    } .elsewhen(opCode === "b1100011".U(7.W)) {
        // BRANCH
    } .elsewhen(opCode === "b1110011".U(7.W)) {
        // ENV
    } .elsewhen(opCode === "b1111".U(7.W)) {
        // FENCE
    }


    // Assign outputs
}