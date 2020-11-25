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


    // Memory
    val dataMemory = Module(new Memory())
    val instructionMemory = Module(new InstructionMemory())


    // Fetch Unit
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
    val dispatcher = Module(new Dispatcher)
    dispatcher.io.instruction := instructionReg
    dispatcher.io.regPortAlu <> registers.io.portAlu
    dispatcher.io.regPortLoadStore <> registers.io.portLoadStore


    // ALU
    val alu = Module(new Alu())
    alu.io.in <> dispatcher.io.aluOut


    // Load/Store
    val loadStore = Module(new LoadStoreUnit())
    loadStore.io.in <> dispatcher.io.loadStoreOut
    loadStore.io.memory <> dataMemory.io

    // Write Back
    registers.io.portAlu.rdAddress := alu.io.out.rd
    registers.io.portAlu.rdValue := alu.io.out.result
    registers.io.portLoadStore.rdAddress := loadStore.io.out.rd
    registers.io.portLoadStore.rdValue := loadStore.io.out.loadedValue

    io.led := Cat(instruction1(3, 0), instruction2(3, 0))
}

/**
 * An object extending App to generate the Verilog code.
 */
object LittleRiscyMain extends App {
    println("I will now generate the Verilog file!")
    chisel3.Driver.execute(Array("--target-dir", "generated"), () => new LittleRiscy())
}
