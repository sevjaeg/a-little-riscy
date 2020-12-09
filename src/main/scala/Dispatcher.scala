/*
 *
 */

import chisel3._
import chisel3.util._


class Dispatcher extends Module {
    val io = IO(new Bundle {
        val instruction = Input(UInt(64.W))
        val aluOut = Flipped(new AluInIO())
        val loadStoreOut = Flipped(new LoadStoreInIO())
        val regPortAlu = Flipped(new RegisterReadIO())
        val regPortLoadStore = Flipped(new RegisterReadIO())
    })

    val pc = io.instruction(63,32)
    val instruction = io.instruction(31,0)

    // This should handle all instructions
    // TODO consider renaming
    val bits_opCode = instruction(6, 0)
    val bits_rd = instruction(11, 7)
    val bits_function = instruction(14, 12)
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
    val aluImmediate = Wire(UInt(20.W))
    aluFunction := 0.U
    aluIn1 := 0.U
    aluIn2 := 0.U
    aluRdAddress := 0.U
    aluR1Address := 0.U
    aluR2Address := 0.U
    aluHasImmediate := false.B
    aluImmediate := 0.U

    // Intermediate Signals (LoadStore)
    val lsFunction = Wire(UInt(4.W))
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

    switch(bits_opCode) {
        is("b0000011".U) { // Load
            lsFunction := Cat(true.B, bits_function)
            lsRdAddress := bits_rd
            lsR1Address := bits_r1
            lsOffset := Cat(bits_ms, bits_r2)
        }
        is("b0100011".U) { // Store
            lsFunction := ~Cat(true.B, bits_function)
            lsR1Address := bits_r1
            lsR2Address := bits_r2
            lsOffset := Cat(bits_ms, bits_rd)
        }
        is("b0110011".U) { // ALU
            aluFunction := ~Cat(bits_ms(5), bits_function)
            aluR1Address := bits_r1
            aluR2Address := bits_r2
            aluRdAddress := bits_rd
        }
        is("b0010011".U) { // ALU Immediate
            val alternateFunctionBit = Wire(UInt(1.W))
            aluHasImmediate := true.B
            aluImmediate := Cat(bits_ms, bits_r2)
            alternateFunctionBit := false.B
            when(bits_function === "b101".U) {  // Right Shift (logical or arithmetic)
                alternateFunctionBit := bits_ms(5)
                aluImmediate := bits_r2
            }
            aluFunction := ~Cat(alternateFunctionBit, bits_function)
            aluR1Address := bits_r1
            aluR2Address := 0.U
            aluRdAddress := bits_rd
        }
        is("b0110111".U) { // LUI
            aluFunction := "b0001".U
            aluRdAddress := bits_rd
            aluHasImmediate := true.B
            aluHasImmediate := Cat(bits_ms, bits_r2, bits_r1, bits_function)
        }
        is("b0010111".U) { // AUIPC
            // TODO in1 = pc of AUIPC instruction
            aluFunction := "b0100".U
            aluRdAddress := bits_rd
            aluHasImmediate := true.B
            aluImmediate := Cat(bits_ms, bits_r2, bits_r1, bits_function)
        }
        is("b1101111".U) {
            // JAL
        }
        is("b1100111".U) {
            // JALR
        }
        is("b1100011".U) {
            // BRANCH
        }
        is("b1110011".U) {
            // ENV
        }
        is("b0001111".U) {
            // FENCE
            // NOP
        }
    }
    val aluFunctionRegister = RegInit(0.U(4.W))
    val aluIn2Register = RegInit(0.U(32.W))
    val aluR1AddressRegister = RegInit(0.U(5.W))
    val aluR2AddressRegister = RegInit(0.U(5.W))
    val aluRdAddressRegister = RegInit(0.U(5.W))
    val aluHasImmediateRegister = RegInit(0.U(13.W))
    val aluImmediateRegister = RegInit(0.U(20.W))

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

    val lsFunctionRegister = RegInit(0.U(4.W))
    val lsR1AddressRegister = RegInit(0.U(5.W))
    val lsR2AddressRegister = RegInit(0.U(5.W))
    val lsRdAddressRegister = RegInit(0.U(5.W))
    val lsOffsetRegister = RegInit(0.U(12.W))

    lsFunctionRegister := lsFunction
    lsR1AddressRegister := lsR1Address
    lsR2AddressRegister := lsR2Address
    lsRdAddressRegister := lsRdAddress
    lsOffsetRegister := lsOffset

    io.regPortLoadStore.r1.address := lsR1AddressRegister
    io.regPortLoadStore.r2.address := lsR2AddressRegister
    io.loadStoreOut.rd := lsRdAddressRegister
    io.loadStoreOut.addressBase := io.regPortLoadStore.r1.value
    io.loadStoreOut.addressOffset := lsOffsetRegister
    io.loadStoreOut.function := lsFunctionRegister
    io.loadStoreOut.storeValue := io.regPortLoadStore.r2.value
}