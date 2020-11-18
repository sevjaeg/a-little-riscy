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
    val pc = RegInit(0.U(32.W))
    val regs = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))  // x0-x31

    // Memory
    val memD = RegInit(VecInit(Seq.fill(64)(0.U(32.W))))  // Data memory
    val memI = RegInit(VecInit(Seq.fill(64)(0.U(32.W))))  // Instruction memory
    memI(0) := "h00000001".U
    memI(1) := "h00000002".U
    memI(2) := "h00000003".U
    memI(3) := "h00000004".U
    memI(4) := "h00000005".U
    memI(5) := "h00000006".U

    // Fetch Unit
    val fetchUnit = Module(new FetchUnit())
    fetchUnit.io.val1 := memI(fetchUnit.io.addr1)
    fetchUnit.io.val2 := memI(fetchUnit.io.addr2)
    fetchUnit.io.pcIn := pc
    pc := fetchUnit.io.pcOut
    val i1 = fetchUnit.io.i1
    val i2 = fetchUnit.io.i2

    // Instruction Queue


    // Dispatcher


    // ALU In Pipelining Registers
    val alu_function_reg =  RegInit(0.U(4.W))
    val alu_in1_reg =       RegInit(0.U(32.W))
    val alu_in2_reg =       RegInit(0.U(32.W))

    // ALU
    val alu = Module(new Alu())
    alu.io.in1 := alu_in1_reg
    alu.io.in2 := alu_in2_reg
    alu.io.function := alu_function_reg

    // ALU Out Pipelining Registers
    val alu_out_reg = RegInit(0.U(32.W))
    alu_out_reg := alu.io.result

    // Load/Store In Pipelining Registers
    val ls_function_reg = RegInit(0.U(3.W))
    val ls_store_reg =    RegInit(0.U(32.W))
    val ls_address_reg =  RegInit(0.U(32.W))
    val ls_offset_reg =   RegInit(0.U(12.W))

    // Load/Store
    val loadStore = Module(new LoadStoreUnit())
    loadStore.io.function := ls_function_reg
    loadStore.io.storeValue := ls_store_reg
    loadStore.io.address := ls_address_reg
    loadStore.io.offset := ls_offset_reg
    loadStore.io.memIn := memD(loadStore.io.calculatedAdress)
    val memOut = Output(UInt(32.W))
    when(loadStore.io.writeEnable === true.B) {
        memD(loadStore.io.calculatedAdress) := loadStore.io.memOut
    } .otherwise {
        memD(loadStore.io.calculatedAdress) := memD(loadStore.io.calculatedAdress)
    }


    // Load/Store out Pipelining Registers
    val ls_loaded_reg = RegInit(0.U(32.W))
    ls_loaded_reg := loadStore.io.loadedValue

    // Write Back Unit

    // Debug with LEDs and Switches
    alu_function_reg := io.sw(3,0)
    alu_in1_reg := io.sw(9,4)
    alu_in2_reg := io.sw(15,10)

    io.led := Cat(i1(7,0), i2(7,0))
}

/**
 * An object extending App to generate the Verilog code.
 */
object ALittleRiscyMain extends App {
    println("I will now generate the Verilog file!")
    chisel3.Driver.execute(Array("--target-dir", "generated"), () => new LittleRiscy())
}
