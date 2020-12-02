/*
 *
 */

import chisel3._
import chisel3.util._


class Dispatcher extends Module {
    val io = IO(new Bundle {
        val instruction = Input(UInt(32.W))
        val aluOut = Flipped(new AluInIO())
        val loadStoreOut = Flipped(new LoadStoreInIO())
        val regPortAlu = Flipped(new RegisterReadIO())
        val regPortLoadStore = Flipped(new RegisterReadIO())
    })

    val instruction = io.instruction

    // This should handle all instructions
    // TODO consider renaming
    val bits_opCode = instruction(6, 0)
    val bits_function = instruction(14, 12)
    val bits_rd = instruction(11, 7)
    val bits_r1 = instruction(19, 15)
    val bits_r2 = instruction(24, 20)
    val bits_ms = instruction(31, 25)

    // Intermediate Signals (ALU)
    val aluFunction = Wire(UInt(4.W))
    val aluIn1 = Wire(UInt(32.W))
    val aluIn2 = Wire(UInt(32.W))
    val aluRdAddress = Wire(UInt(5.W))
    val aluR1Address = Wire(UInt(5.W))
    val aluR2Address = Wire(UInt(5.W))
    val aluHasImmediate = Wire(UInt(1.W))
    val aluImmediate = Wire(UInt(12.W))
    aluFunction := 0.U
    aluIn1 := 0.U
    aluIn2 := 0.U
    aluRdAddress := 0.U
    aluR1Address := 0.U
    aluR2Address := 0.U
    aluHasImmediate := false.B
    aluImmediate := 0.U

    // Intermediate Signals (LoadStore)
    val lsFunction = Wire(UInt(3.W))
    val lsRdAddress = Wire(UInt(5.W))
    val lsR1Address = Wire(UInt(5.W))
    val lsR2Address = Wire(UInt(5.W))
    val lsAddressBase = Wire(UInt(32.W))
    val lsOffset = Wire(UInt(12.W))
    val lsStoreValue = Wire(UInt(32.W))
    lsFunction := 0.U
    lsRdAddress := 0.U
    lsR1Address := 0.U
    lsR2Address := 0.U
    lsAddressBase := 0.U
    lsOffset := 0.U
    lsStoreValue := 0.U

    // Alu function encoding: Inv(Cat(alternate_bit, fn)), already implemented in ALU
    // LUI: 0001, AUIPC: 0100

    when(bits_opCode === "b11".U(7.W)) { // Load
        // TODO ls encoding
        lsRdAddress := bits_rd
        lsR1Address := bits_r1
        lsOffset := Cat(bits_ms, bits_r2) // TODO LBU, LHU
    } .elsewhen(bits_opCode === "b100011".U(7.W)) { // Store
        // TODO ls encoding
        lsR1Address := bits_r1
        lsR2Address := bits_r2
        lsOffset := Cat(bits_ms, bits_rd)
    } .elsewhen(bits_opCode(4,0) === "b10011".U(7.W)) {  // ALU
        val alternateFunctionBit = Wire(UInt(1.W))
            when(bits_opCode(5) === true.B) { // ALU Register
                alternateFunctionBit := bits_ms(5)
            } .otherwise { // ALU Immediate
                aluHasImmediate := true.B
                aluImmediate := Cat(bits_ms, bits_r2)
                alternateFunctionBit := false.B
                when(bits_function === "b101".U) {
                    alternateFunctionBit := bits_ms(5)
                    aluImmediate := bits_r2
                }

            }
        aluFunction := ~ Cat(alternateFunctionBit, bits_function)
        aluR1Address := bits_r1
        switch(aluHasImmediate) {
            is(false.B) {aluR2Address := bits_r2}
            is(true.B) {aluR2Address := 0.U}
        }
        aluRdAddress := bits_rd
    } .elsewhen(bits_opCode === "b0110111".U(7.W)) {
        // LUI
        aluFunction := "b0001".U
    } .elsewhen(bits_opCode === "b0010111".U(7.W)) {
        // AUIPC
        aluFunction := "b0100".U
    } .elsewhen(bits_opCode === "b1101111".U(7.W)) {
        // JAL
    } .elsewhen(bits_opCode === "b1100111".U(7.W)) {
        // JALR
    } .elsewhen(bits_opCode === "b1100011".U(7.W)) {
        // BRANCH
    } .elsewhen(bits_opCode === "b1110011".U(7.W)) {
        // ENV
    } .elsewhen(bits_opCode === "b1111".U(7.W)) {
        // FENCE
    }

    val aluFunctionRegister = RegInit(0.U(4.W))
    val aluIn2Register = RegInit(0.U(32.W))
    val aluR1AddressRegister = RegInit(0.U(5.W))
    val aluR2AddressRegister = RegInit(0.U(5.W))
    val aluRdAddressRegister = RegInit(0.U(5.W))
    val aluHasImmediateRegister = RegInit(0.U(13.W))
    val aluImmediateRegister = RegInit(0.U(13.W))

    aluFunctionRegister := aluFunction
    aluRdAddressRegister := aluRdAddress
    aluR1AddressRegister := aluR1Address
    aluR2AddressRegister := aluR2Address
    aluHasImmediateRegister := aluHasImmediate
    aluImmediateRegister := aluImmediate

    // Assign outputs
    io.regPortAlu.r1.address := aluR1AddressRegister
    io.regPortAlu.r2.address := aluR2AddressRegister
    io.aluOut.function := aluFunctionRegister
    io.aluOut.in1 := io.regPortAlu.r1.value
    io.aluOut.in2 := io.regPortAlu.r2.value
    io.aluOut.hasImmediate := aluHasImmediateRegister
    io.aluOut.inImmediate := aluImmediateRegister

    io.aluOut.rd := aluRdAddressRegister

    val lsFunctionRegister = RegInit(0.U(3.W))
    val lsRdAddressRegister = RegInit(0.U(5.W))
    val lsOffsetRegister = RegInit(0.U(12.W))

    lsFunctionRegister := lsFunction
    lsRdAddressRegister := lsRdAddress
    lsOffsetRegister := lsOffset

    io.regPortLoadStore.r1.address := lsR1Address
    io.regPortLoadStore.r2.address := lsR2Address
    io.loadStoreOut.rd := lsRdAddressRegister
    io.loadStoreOut.addressBase := io.regPortAlu.r1.value
    io.loadStoreOut.addressOffset := lsOffsetRegister
    io.loadStoreOut.function := lsFunctionRegister
    io.loadStoreOut.storeValue := io.regPortAlu.r2.value
}