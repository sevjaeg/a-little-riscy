/*
 *
 */

import chisel3._
import chisel3.util._

class LittleRiscy extends Module {
    val io = IO(new Bundle {
        val sw = Input(UInt(16.W))
        val led = Output(UInt(16.W))
    })

    // Registers
    val registers = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))  // x0-x31
    val pc = RegInit(0.U(32.W))

    // Memory
    val dataMemory = Module(new Memory())
    val instructionMemory = RegInit(VecInit(Seq.fill(64)(0.U(32.W))))
    instructionMemory(0) := "h00000001".U
    instructionMemory(1) := "h00000002".U
    instructionMemory(2) := "h00000003".U
    instructionMemory(3) := "h00000004".U
    instructionMemory(4) := "h00000005".U
    instructionMemory(5) := "h00000006".U

    // Fetch Unit
    val fetchUnit = Module(new FetchUnit())
    fetchUnit.io.val1 := instructionMemory(fetchUnit.io.addr1)
    fetchUnit.io.val2 := instructionMemory(fetchUnit.io.addr2)
    fetchUnit.io.pcIn := pc
    pc := fetchUnit.io.pcOut
    val instruction1 = fetchUnit.io.i1
    val instruction2 = fetchUnit.io.i2

    // Instruction Queue


    // Dispatcher


    // ALU In Pipelining Registers
    val aluFunctionReg =  RegInit(0.U(4.W))
    val aluIn1Reg =       RegInit(0.U(32.W))
    val aluIn2Reg =       RegInit(0.U(32.W))

    // ALU
    val alu = Module(new Alu())
    alu.io.function := aluFunctionReg
    alu.io.in1 := aluIn1Reg
    alu.io.in2 := aluIn2Reg

    // ALU Out Pipelining Registers
    val aluOutReg = RegInit(0.U(32.W))
    aluOutReg := alu.io.result

    // Load/Store In Pipelining Registers
    val loadStoreFunctionReg = RegInit(0.U(3.W))
    val loadStoreStoreReg =    RegInit(0.U(32.W))
    val loadStoreAddressReg =  RegInit(0.U(32.W))
    val loadStoreOffsetReg =   RegInit(0.U(12.W))

    // Load/Store
    val loadStore = Module(new LoadStoreUnit())
    loadStore.io.function := loadStoreFunctionReg
    loadStore.io.storeValue := loadStoreStoreReg
    loadStore.io.addressBase := loadStoreAddressReg
    loadStore.io.addressOffset := loadStoreOffsetReg
    loadStore.io.memory <> dataMemory.io

    // Load/Store out Pipelining Registers
    val ls_loaded_reg = RegInit(0.U(32.W))
    ls_loaded_reg := loadStore.io.loadedValue

    // Write Back Unit

    // Debug with LEDs and Switches
    aluFunctionReg := io.sw(3,0)
    aluIn1Reg := io.sw(9,4)
    aluIn2Reg := io.sw(15,10)

    io.led := Cat(instruction1(3, 0), instruction2(3, 0), aluOutReg(7, 0))
}

/**
 * An object extending App to generate the Verilog code.
 */
object LittleRiscyMain extends App {
    println("I will now generate the Verilog file!")
    chisel3.Driver.execute(Array("--target-dir", "generated"), () => new LittleRiscy())
}
