/*
 *
 */

import chisel3._
import chisel3.util._


class Dispatcher extends Module {
    val io = IO(new Bundle {
        val instructions = Input(UInt(128.W))
        val instructionsValid = Input(Bool())
        val ready = Output(Bool())
        val aluOut = Flipped(new AluInIO())
        val loadStoreOut = Flipped(new LoadStoreInIO())
        val regPortAlu = Flipped(new RegisterReadIO())
        val regPortLoadStore = Flipped(new RegisterReadIO())
    })

    val valid = io.instructionsValid
    val pc1 = io.instructions(127,96)
    val instruction1 = io.instructions(95,64)
    val pc2 = io.instructions(63,32)
    val instruction2 = io.instructions(31,0)

    val stallAlu = Wire(Bool())
    val stallLoadStore = Wire(Bool())
    val aluStallRegister = RegInit(0.U(65.W))  // holds stalled bit, pc, instruction
    val loadStoreStallRegister = RegInit(0.U(65.W)) // holds stalled bit, pc, instruction
    aluStallRegister := 0.U
    loadStoreStallRegister := 0.U

    val instructionAlu = Wire(UInt(32.W))
    val pcAlu = Wire(UInt(32.W))
    val instructionLoadStore = Wire(UInt(32.W))
    val pcLoadStore = Wire(UInt(32.W))

    val nop = 0.U(32.W)
    instructionAlu := nop
    pcAlu := nop
    instructionLoadStore := nop
    pcLoadStore := nop

    // FENCE, ECALL, EBREAK treated as NOP
    val isNop1 = instruction1 === 0.U | instruction1(6,0) === "b0001111".U | instruction1(6,4) === "b111".U
    val isAlu1 = (instruction1(4, 0) === "b10011".U & instruction1(6) === false.B) | instruction1(3,0) === "b0111".U
    val isLoadStore1 = instruction1(4, 0) === "b00011".U & instruction1(6) === false.B
    val isNop2 = instruction2 === 0.U | instruction2(6,0) === "b0001111".U | instruction2(6,4) === "b111".U
    val isAlu2 = instruction2(4, 0) === "b10011".U & instruction2(6) === false.B | instruction2(3,0) === "b0111".U
    val isLoadStore2 = instruction2(4, 0) === "b00011".U & instruction2(6) === false.B

    stallAlu := false.B
    stallLoadStore := false.B
    val stalledLastCycle = loadStoreStallRegister(64) | aluStallRegister(64)
    when(!stalledLastCycle) {
        when((isNop1 & isLoadStore2) | (isAlu1 & isNop2) | (isNop1 & isNop2) | (isAlu1 & isLoadStore2)) {
            when((!(instruction1(11,7) === 0.U)) & (instruction1(11,7) === instruction2(11,7))) {
                // same destination register (not x0)-> stall Load/Store
                stallLoadStore := true.B
                loadStoreStallRegister := Cat(true.B, pc2, instruction2)
            } .otherwise {
                instructionLoadStore := instruction2
                pcLoadStore := pc2
            }
            instructionAlu := instruction1
            pcAlu := pc1
            instructionLoadStore := instruction2
            pcLoadStore := pc2
        }.elsewhen((isAlu2 & isLoadStore1) | (isNop2 & isLoadStore1) | (isAlu2 & isNop1)) {
            // TODO handle data hazard
            instructionLoadStore := instruction1
            pcLoadStore := pc1
            when((!(instruction1(11,7) === 0.U)) & (instruction1(11,7) === instruction2(11,7))) {
                // same destination register (not x0) -> stall Alu
                stallAlu := true.B
                aluStallRegister := Cat(true.B, pc2, instruction2)
            } .otherwise {
                instructionAlu := instruction2
                pcAlu := pc2
            }
        }.elsewhen(isAlu1 & isAlu2) {
            //TODO handle data hazard
            instructionAlu := instruction1
            pcAlu := pc1
            stallAlu := true.B
            aluStallRegister := Cat(true.B, pc2, instruction2)
        }.elsewhen(isLoadStore1 & isLoadStore2) {
            // TODO handle data hazard
            instructionLoadStore := instruction1
            pcLoadStore := pc1
            stallLoadStore := true.B
            loadStoreStallRegister := Cat(true.B, pc2, instruction2)
        }.otherwise {
            // TODO branches
            /*
            is("b1101111".U) {
                // JAL
            }
            is("b1100111".U) {
                // JALR
            }
            is("b1100011".U) {
                // BRANCH
            }
             */
        }
    } .otherwise {  // currently stalled
        when(loadStoreStallRegister(64) === true.B) {
            instructionLoadStore := loadStoreStallRegister(31,0)
            pcLoadStore := loadStoreStallRegister(63,32)
            stallLoadStore := false.B
            loadStoreStallRegister := 0.U
        } .elsewhen(aluStallRegister(64) === true.B) {
            instructionAlu := aluStallRegister(31,0)
            pcAlu := aluStallRegister(63,32)
            stallAlu := false.B
            aluStallRegister := 0.U
        }

    }

    // TODO check data hazards
    // WAW or WAR conflict
    // queue: not ready
    // store one instruction
    // insert 2 NOPs
    // schedule
    // queue ready
    // TODO forwarding to reduce penalty

    //******* ALU ******************************************************************************************

    val bitsOpCodeAlu = instructionAlu(6, 0)
    val bitsRdAlu = instructionAlu(11, 7)
    val bitsFunctionAlu = instructionAlu(14, 12)
    val bitsR1Alu = instructionAlu(19, 15)
    val bitsR2Alu = instructionAlu(24, 20)
    val bitsImmAlu = instructionAlu(31, 25)

    // Intermediate Signals (ALU)
    val aluFunction = Wire(UInt(4.W))
    val aluIn1 = Wire(UInt(32.W))
    val aluIn2 = Wire(UInt(32.W))
    val aluRdAddress = Wire(UInt(5.W))
    val aluR1Address = Wire(UInt(5.W))
    val aluR2Address = Wire(UInt(5.W))
    val aluHasImmediate = Wire(UInt(1.W))
    val aluIsAUIPC = Wire(UInt(1.W))
    val aluImmediate = Wire(UInt(20.W))
    aluFunction := 0.U
    aluIn1 := 0.U
    aluIn2 := 0.U
    aluRdAddress := 0.U
    aluR1Address := 0.U
    aluR2Address := 0.U
    aluHasImmediate := false.B
    aluIsAUIPC := false.B
    aluImmediate := 0.U


    switch(bitsOpCodeAlu) {
        is("b0110011".U) { // ALU
            aluFunction := ~Cat(bitsImmAlu(5), bitsFunctionAlu)
            aluR1Address := bitsR1Alu
            aluR2Address := bitsR2Alu
            aluRdAddress := bitsRdAlu
        }
        is("b0010011".U) { // ALU Immediate
            val alternateFunctionBit = Wire(UInt(1.W))
            aluHasImmediate := true.B
            aluImmediate := Cat(bitsImmAlu, bitsR2Alu)
            alternateFunctionBit := false.B
            when(bitsFunctionAlu === "b101".U) {  // Right Shift (logical or arithmetic)
                alternateFunctionBit := bitsImmAlu(5)
                aluImmediate := bitsR2Alu
            }
            aluFunction := ~Cat(alternateFunctionBit, bitsFunctionAlu)
            aluR1Address := bitsR1Alu
            aluR2Address := 0.U
            aluRdAddress := bitsRdAlu
        }
        is("b0110111".U) { // LUI
            aluFunction := "b0001".U
            aluRdAddress := bitsRdAlu
            aluHasImmediate := true.B
            aluImmediate := Cat(bitsImmAlu, bitsR2Alu, bitsR1Alu, bitsFunctionAlu)
        }
        is("b0010111".U) { // AUIPC
            aluIsAUIPC := true.B
            aluFunction := "b0100".U
            aluRdAddress := bitsRdAlu
            aluHasImmediate := true.B
            aluImmediate := Cat(bitsImmAlu, bitsR2Alu, bitsR1Alu, bitsFunctionAlu)
        }
    }

    // ******* LOAD/STORE ***********************************************************************************

    val bitsOpCodeLoadStore = instructionLoadStore(6, 0)
    val bitsRdLoadStore = instructionLoadStore(11, 7)
    val bitsFunctionLoadStore = instructionLoadStore(14, 12)
    val bitsR1LoadStore = instructionLoadStore(19, 15)
    val bitsR2LoadStore = instructionLoadStore(24, 20)
    val bitsImmLoadStore = instructionLoadStore(31, 25)

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

    switch(bitsOpCodeLoadStore) {
        is("b0000011".U) { // Load
            lsFunction := Cat(true.B, bitsFunctionLoadStore)
            lsRdAddress := bitsRdLoadStore
            lsR1Address := bitsR1LoadStore
            lsOffset := Cat(bitsImmLoadStore, bitsR2LoadStore)
        }
        is("b0100011".U) { // Store
            lsFunction := ~Cat(true.B, bitsFunctionLoadStore)
            lsR1Address := bitsR1LoadStore
            lsR2Address := bitsR2LoadStore
            lsOffset := Cat(bitsImmLoadStore, bitsRdLoadStore)
        }
    }

    // Pipelining Registers *********************************************************************************
    val aluFunctionRegister = RegInit(0.U(4.W))
    val aluIn2Register = RegInit(0.U(32.W))
    val aluR1AddressRegister = RegInit(0.U(5.W))
    val aluR2AddressRegister = RegInit(0.U(5.W))
    val aluRdAddressRegister = RegInit(0.U(5.W))
    val aluHasImmediateRegister = RegInit(0.U(1.W))
    val aluImmediateRegister = RegInit(0.U(20.W))
    val aluIsAUIPCRegister = RegInit(0.U(1.W))
    val aluPcRegister = RegInit(0.U(32.W))

    aluFunctionRegister := aluFunction
    aluRdAddressRegister := aluRdAddress
    aluR1AddressRegister := aluR1Address
    aluR2AddressRegister := aluR2Address
    aluHasImmediateRegister := aluHasImmediate
    aluImmediateRegister := aluImmediate
    aluIsAUIPCRegister := aluIsAUIPC
    aluPcRegister := pcAlu

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

    // **** Assign outputs *********************************************************************************
    io.regPortAlu.r1.address := aluR1AddressRegister
    io.regPortAlu.r2.address := aluR2AddressRegister
    io.aluOut.function := aluFunctionRegister
    io.aluOut.in1 := io.regPortAlu.r1.value
    io.aluOut.in2 := io.regPortAlu.r2.value
    io.aluOut.hasImmediate := aluHasImmediateRegister
    io.aluOut.inImmediate := aluImmediateRegister
    io.aluOut.isAUIPC := aluIsAUIPCRegister
    io.aluOut.inPc := aluPcRegister
    io.aluOut.rd := aluRdAddressRegister

    io.regPortLoadStore.r1.address := lsR1AddressRegister
    io.regPortLoadStore.r2.address := lsR2AddressRegister
    io.loadStoreOut.rd := lsRdAddressRegister
    io.loadStoreOut.addressBase := io.regPortLoadStore.r1.value
    io.loadStoreOut.addressOffset := lsOffsetRegister
    io.loadStoreOut.function := lsFunctionRegister
    io.loadStoreOut.storeValue := io.regPortLoadStore.r2.value

    // TODO
    io.ready := !(stallAlu | stallLoadStore)
}