/*
 *
 */

import chisel3._
import chisel3.util._

class LittleRiscy extends Module {
    val io = IO(new Bundle {
        val sw = Input(UInt(16.W))
        val led = Output(UInt(16.W))
        val i1 = Output(UInt(32.W))
        val i2 = Output(UInt(32.W))
        val aluIn1 = Input(UInt(32.W))
        val aluIn2 = Input(UInt(32.W))
        val aluFn = Input(UInt(4.W))
        val aluRd = Input(UInt(32.W))
        val aluRes = Output(UInt(32.W))
    })

    // Registers
    val registers = Module(new Registers())
    val pc = registers.io.pc
    val registersAlu = registers.io.portAlu
    val registersLoadStore = registers.io.portLoadStore

    // Memory
    val dataMemory = Module(new Memory())
    val instructionMemory = Module(new InstructionMemory())


    // Fetch Unit
    // TODO <> instruction memory <-> fetch unit
    val fetchUnit = Module(new FetchUnit())
    instructionMemory.io.port1 <> fetchUnit.io.IMemPort1
    instructionMemory.io.port2 <> fetchUnit.io.IMemPort2
    fetchUnit.io.pcIn := pc
    registers.io.newPc := fetchUnit.io.pcOut
    val instruction1 = fetchUnit.io.i1
    val instruction2 = fetchUnit.io.i2



    // Instruction Queue
    val instructionReg = RegInit(0.U(32.W))  // TODO replace with queue
    instructionReg := instruction1
    // Debug output
    io.i1 := instructionReg
    io.i2 := instruction2

    // Dispatcher


    // ALU In Pipelining Registers
    val aluFunctionReg =  RegInit(0.U(5.W))
    val aluIn1Reg =       RegInit(0.U(32.W))
    val aluIn2Reg =       RegInit(0.U(32.W))
    val aluRdReg =        RegInit(0.U(5.W))
    // ALU
    val alu = Module(new Alu())

    // Load/Store In Pipelining Registers
    val loadStoreFunctionReg = RegInit(0.U(3.W))
    val loadStoreStoreReg =    RegInit(0.U(32.W))
    val loadStoreAddressReg =  RegInit(0.U(32.W))
    val loadStoreOffsetReg =   RegInit(0.U(12.W))
    // Load/Store
    val loadStore = Module(new LoadStoreUnit())
    loadStore.io.in.function := loadStoreFunctionReg
    loadStore.io.in.storeValue := loadStoreStoreReg
    loadStore.io.in.addressBase := loadStoreAddressReg
    loadStore.io.in.addressOffset := loadStoreOffsetReg
    loadStore.io.memory <> dataMemory.io

    // Write Back
    // TODO check if not 0
    // TODO check of different addresses
    registers.io.portAlu.rdAddress := alu.io.out.rd
    registers.io.portAlu.rdValue := alu.io.out.result
    registers.io.portLoadStore.rdAddress := loadStore.io.out.rd
    registers.io.portLoadStore.rdValue := loadStore.io.out.loadedValue

    // Debug with LEDs and Switches
    registersAlu.r1Address := io.sw(7,4)
    registersAlu.r2Address := io.sw(7,4)
    registersLoadStore.r1Address := io.sw(11,8)
    registersLoadStore.r2Address := io.sw(11,8)
    loadStoreFunctionReg := 3.U
    loadStoreAddressReg := io.sw(15,13)

    aluFunctionReg := io.aluFn
    aluIn1Reg := io.aluIn1
    aluIn2Reg := io.aluIn2
    aluRdReg := io.aluRd
    io.aluRes := registersAlu.rdValue

    io.led := Cat(instruction1(3, 0), instruction2(3, 0))
}

/**
 * An object extending App to generate the Verilog code.
 */
object LittleRiscyMain extends App {
    println("I will now generate the Verilog file!")
    chisel3.Driver.execute(Array("--target-dir", "generated"), () => new LittleRiscy())
}
