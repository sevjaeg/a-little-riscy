/*
 *
 */

import chisel3._
import chisel3.util._


class Dispatcher extends Module {
    val io = IO(new Bundle {
        val instructions = Input(UInt(96.W))
        val instructionsValid = Input(Bool())
        val ready = Output(Bool())
        val aluOut = Flipped(new AluInDispatcherIO())
        val loadStoreOut = Flipped(new LoadStoreInDispatcherIO())
        val regPortAlu = Flipped(new RegisterDualReadIO())
        val regPortLoadStore = Flipped(new RegisterDualReadIO())
        val pipelineFlushed = Output(Bool())
    })

    // Instructions from queue
    val valid = io.instructionsValid
    val pc1 = io.instructions(95,64)
    val instruction1 = io.instructions(63,32)
    val pc2 = pc1 + 1.U
    val instruction2 = io.instructions(31,0)

    // Instructions after function unit mapping
    val nop = 0.U(32.W)
    val instructionAlu = WireDefault(0.U(32.W))
    val pcAlu = WireDefault(0.U(32.W))
    val instructionLoadStore = WireDefault(0.U(32.W))
    val pcLoadStore = WireDefault(0.U(32.W))

    // Pipelining Registers
    val aluFunctionRegister = RegInit(0.U(4.W))
    val aluIn2Register = RegInit(0.U(32.W))
    val aluR1AddressRegister = RegInit(0.U(5.W))
    val aluR2AddressRegister = RegInit(0.U(5.W))
    val aluRdAddressRegister = RegInit(0.U(5.W))
    val aluImmediateRegister = RegInit(0.U(20.W))
    val aluPcRegister = RegInit(0.U(32.W))
    val aluIn1SelectRegister = RegInit(0.U(2.W))
    val aluIn2SelectRegister = RegInit(0.U(2.W))
    val lsFunctionRegister = RegInit(0.U(4.W))
    val lsR1AddressRegister = RegInit(0.U(5.W))
    val lsR2AddressRegister = RegInit(0.U(5.W))
    val lsRdAddressRegister = RegInit(0.U(5.W))
    val lsOffsetRegister = RegInit(0.U(12.W))
    val lsIn1SelectRegister = RegInit(0.U(2.W))
    val lsIn2SelectRegister = RegInit(0.U(2.W))

    // Handling of structural hazards
    val stallAlu = WireDefault(false.B)
    val stallLoadStore = WireDefault(false.B)
    val aluStallRegister = RegInit(0.U(64.W))  // holds pc, instruction
    val loadStoreStallRegister = RegInit(0.U(64.W)) // holds pc, instruction
    val wasAluStalled = RegInit(false.B)
    val wasLoadStoreStalled = RegInit(false.B)
    aluStallRegister := 0.U
    loadStoreStallRegister := 0.U
    wasAluStalled := false.B
    wasLoadStoreStalled := false.B

    // Handling of data hazards
    val lastAluRd = aluRdAddressRegister
    val lastLoadStoreRd = lsRdAddressRegister
    val aluBeforeLoadStore = WireDefault(false.B)
    val forwardAluR1Alu = WireDefault(false.B)
    val forwardAluR2Alu = WireDefault(false.B)
    val forwardLsuAddressAlu = WireDefault(false.B)
    val forwardLsuValueAlu = WireDefault(false.B)
    val forwardAluR1Lsu = WireDefault(false.B)
    val forwardAluR2Lsu = WireDefault(false.B)
    val forwardLsuAddressLsu = WireDefault(false.B)
    val forwardLsuValueLsu = WireDefault(false.B)

    // Function unit mapping and handling of structural hazards ********************************************************
    // FENCE, ECALL, EBREAK treated as NOP
    val isNop1 = instruction1 === 0.U | instruction1(6,0) === "b0001111".U | instruction1(6,4) === "b111".U
    val isAlu1 = (instruction1(4, 0) === "b10011".U & instruction1(6) === false.B) | instruction1(3,0) === "b0111".U
    val isLoadStore1 = instruction1(4, 0) === "b00011".U & instruction1(6) === false.B
    val isNop2 = instruction2 === 0.U | instruction2(6,0) === "b0001111".U | instruction2(6,4) === "b111".U
    val isAlu2 = instruction2(4, 0) === "b10011".U & instruction2(6) === false.B | instruction2(3,0) === "b0111".U
    val isLoadStore2 = instruction2(4, 0) === "b00011".U & instruction2(6) === false.B

    val stalledLastCycle = wasAluStalled | wasLoadStoreStalled
    when(!stalledLastCycle) {
        when((isNop1 & isLoadStore2) | (isAlu1 & isNop2) | (isNop1 & isNop2) | (isAlu1 & isLoadStore2)) {
            // first alu, then load store
            when((!(instruction1(11,7) === 0.U)) & (instruction1(11,7) === instruction2(11,7))) {
                // same destination register (not x0)-> stall Load/Store
                stallLoadStore := true.B
            } .otherwise {
                instructionLoadStore := instruction2
                pcLoadStore := pc2
            }
            instructionAlu := instruction1
            pcAlu := pc1
            aluBeforeLoadStore := true.B
            aluStallRegister := Cat(pc1, instruction1)
            loadStoreStallRegister := Cat(pc2, instruction2)
        } .elsewhen((isAlu2 & isLoadStore1) | (isNop2 & isLoadStore1) | (isAlu2 & isNop1)) {
            // first load store, then alu
            when((!(instruction1(11,7) === 0.U)) & (instruction1(11,7) === instruction2(11,7))) {
                // same destination register (not x0) -> stall Alu
                stallAlu := true.B
            } .otherwise {
                instructionAlu := instruction2
                pcAlu := pc2
            }
            instructionLoadStore := instruction1
            pcLoadStore := pc1
            aluBeforeLoadStore := false.B
            aluStallRegister := Cat(pc2, instruction2)
            loadStoreStallRegister := Cat(pc1, instruction1)
        } .elsewhen(isAlu1 & isAlu2) {
            instructionAlu := instruction1
            pcAlu := pc1
            stallAlu := true.B
            aluStallRegister := Cat(pc2, instruction2)
        } .elsewhen(isLoadStore1 & isLoadStore2) {
            instructionLoadStore := instruction1
            pcLoadStore := pc1
            stallLoadStore := true.B
            loadStoreStallRegister := Cat(pc2, instruction2)
        }
    } .otherwise {  // currently stalled
        when(wasLoadStoreStalled === true.B) {
            instructionLoadStore := loadStoreStallRegister(31,0)
            pcLoadStore := loadStoreStallRegister(63,32)
            stallLoadStore := false.B
        } .elsewhen(wasAluStalled === true.B) {
            instructionAlu := aluStallRegister(31,0)
            pcAlu := aluStallRegister(63,32)
            stallAlu := false.B
        }
    }

    wasAluStalled := stallAlu
    wasLoadStoreStalled := stallLoadStore

    //******* ALU ******************************************************************************************
    val bitsOpCodeAlu = instructionAlu(6, 0)
    val bitsRdAlu = instructionAlu(11, 7)
    val bitsFunctionAlu = instructionAlu(14, 12)
    val bitsR1Alu = instructionAlu(19, 15)
    val bitsR2Alu = instructionAlu(24, 20)
    val bitsImmAlu = instructionAlu(31, 25)

    // Intermediate Signals (ALU)
    val aluFunction = WireDefault(0.U(4.W))
    val aluRdAddress = WireDefault(0.U(5.W))
    val aluR1Address = WireDefault(0.U(5.W))
    val aluR2Address = WireDefault(0.U(5.W))
    val aluHasImmediate = WireDefault(false.B)
    val aluIsAUIPC = WireDefault(false.B)
    val aluImmediate = WireDefault(0.U(20.W))

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

    val lsFunction = WireDefault(0.U(4.W))
    val lsRdAddress = WireDefault(0.U(5.W))
    val lsR1Address = WireDefault(0.U(5.W))
    val lsR2Address = WireDefault(0.U(5.W))
    val lsAddressBase = WireDefault(0.U(32.W))
    val lsOffset = WireDefault(0.U(12.W))
    val lsStoreValue = WireDefault(0.U(32.W))

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

    // Handling of Data Hazards ***************************************************************************

    val disableLoadStore = WireDefault(false.B)
    val disableAlu = WireDefault(false.B)

    when(aluBeforeLoadStore) {
        when(aluRdAddress =/= 0.U) {
            when(lsR1Address === aluRdAddress || lsR2Address === aluRdAddress) {
                loadStoreStallRegister := Cat(true.B, pcLoadStore, instructionLoadStore)
                stallLoadStore := true.B
                disableLoadStore := true.B
            }
        }
    } .otherwise{  // instruction 1: L/S, instruction 2: ALU
        when(lsRdAddress =/= 0.U) {
            when(aluR1Address === lsRdAddress || aluR2Address === lsRdAddress) {
                aluStallRegister := Cat(true.B, pcAlu, instructionAlu)
                stallAlu := true.B
                disableAlu := true.B
            }
        }
    }

    // Forward if I1 or I2 depends on one previous
    val wasDisabledAlu = RegInit(false.B)
    val wasDisabledLoadStore = RegInit(false.B)
    wasDisabledLoadStore := disableLoadStore
    wasDisabledAlu := disableAlu

    when(aluR1Address =/= 0.U) {
        when(!wasDisabledAlu & aluR1Address === lastAluRd) {
            forwardAluR1Alu := true.B
        }.elsewhen(!wasDisabledLoadStore & aluR1Address === lastLoadStoreRd) {
            forwardAluR1Lsu := true.B
        }
    }
    when(aluR2Address =/= 0.U) {
        when(!wasDisabledAlu & aluR2Address === lastAluRd) {
            forwardAluR2Alu := true.B
        }.elsewhen(!wasDisabledLoadStore & aluR2Address === lastLoadStoreRd) {
            forwardAluR2Lsu := true.B
        }
    }

    when(lsR1Address =/= 0.U) {
        when(!wasDisabledAlu & lsR1Address === lastAluRd) {
            forwardLsuAddressAlu := true.B
        }.elsewhen(!wasDisabledLoadStore & lsR1Address === lastLoadStoreRd) {
            forwardLsuAddressLsu := true.B
        }
    }
    when(lsR2Address =/= 0.U) {
        when(!wasDisabledAlu & lsR2Address === lastAluRd) {
            forwardLsuValueAlu := true.B
        }.elsewhen(!wasDisabledLoadStore & lsR2Address === lastLoadStoreRd) {
            forwardLsuValueLsu := true.B
        }
    }



    // Pipeline Empty? *********************************************************************************
    val isNop = isNop1 & isNop2
    val wasNop = RegInit(false.B)
    wasNop := isNop
    val wasWasNop = RegInit(false.B)
    wasWasNop := wasNop
    io.pipelineFlushed := isNop & wasNop & wasWasNop

    // Pipelining Registers *********************************************************************************

    when(disableAlu) {
        aluFunctionRegister := 0.U
    } .otherwise {
        aluFunctionRegister := aluFunction
    }

    aluRdAddressRegister := aluRdAddress
    aluR1AddressRegister := aluR1Address
    aluR2AddressRegister := aluR2Address
    aluImmediateRegister := aluImmediate
    aluPcRegister := pcAlu

    aluIn1SelectRegister := Cat(forwardAluR1Lsu | forwardAluR1Alu, aluIsAUIPC | forwardAluR1Lsu)
    aluIn2SelectRegister := Cat(forwardAluR2Lsu | forwardAluR2Alu, aluHasImmediate | forwardAluR2Lsu)

    when(disableLoadStore) {
        lsFunctionRegister := 0.U
    } .otherwise {
        lsFunctionRegister := lsFunction
    }

    lsR1AddressRegister := lsR1Address
    lsR2AddressRegister := lsR2Address
    lsRdAddressRegister := lsRdAddress
    lsOffsetRegister := lsOffset

    lsIn1SelectRegister:= Cat(forwardLsuValueAlu | forwardLsuValueLsu, forwardLsuValueLsu)
    lsIn2SelectRegister := Cat(forwardLsuAddressAlu | forwardLsuAddressLsu, forwardLsuAddressLsu)


    // **** Assign outputs *********************************************************************************
    io.regPortAlu.r1.rd := aluR1AddressRegister
    io.regPortAlu.r2.rd := aluR2AddressRegister
    io.aluOut.function := aluFunctionRegister
    io.aluOut.in1 := io.regPortAlu.r1.value
    io.aluOut.in2 := io.regPortAlu.r2.value
    io.aluOut.rd := aluRdAddressRegister
    io.aluOut.in1Select := aluIn1SelectRegister
    io.aluOut.in2Select := aluIn2SelectRegister
    io.aluOut.immediate := aluImmediateRegister
    io.aluOut.pc := aluPcRegister

    io.regPortLoadStore.r1.rd := lsR1AddressRegister
    io.regPortLoadStore.r2.rd := lsR2AddressRegister
    io.loadStoreOut.rd := lsRdAddressRegister
    io.loadStoreOut.addressBaseReg := io.regPortLoadStore.r1.value
    io.loadStoreOut.addressOffset := lsOffsetRegister
    io.loadStoreOut.function := lsFunctionRegister
    io.loadStoreOut.valueReg := io.regPortLoadStore.r2.value
    io.loadStoreOut.addressBaseSelect := lsIn2SelectRegister
    io.loadStoreOut.valueSelect := lsIn1SelectRegister

    io.ready := !(stallAlu | stallLoadStore)
}