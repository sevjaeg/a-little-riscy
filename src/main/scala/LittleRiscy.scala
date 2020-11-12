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

    // Modules
    val alu = Module(new Alu())
    val alu_function = alu.io.function
    val alu_in1 = alu.io.in1
    val alu_in2 = alu.io.in2
    val alu_out = alu.io.result

    // Pipelining Registers
    val alu_function_reg =  RegInit(0.U(4.W))
    val alu_in1_reg =       RegInit(0.U(32.W))
    val alu_in2_reg =       RegInit(0.U(32.W))
    val alu_out_reg =       RegInit(0.U(32.W))

    alu_in1 := alu_in1_reg
    alu_in2 := alu_in2_reg
    alu_function := alu_function_reg
    alu_out_reg := alu_out

    // Memory
    val memD = Mem(128, UInt(32.W))  // Data memory
    val memI = Mem(128, UInt(32.W))  // Instruction memory

    // Debug with LEDs and Switches
    alu_function_reg := io.sw(3,0)
    alu_in1_reg := io.sw(9,4)
    alu_in2_reg := io.sw(15,10)

    io.led := alu_out_reg(15, 0)
}

/**
 * An object extending App to generate the Verilog code.
 */
object ALittleRiscyMain extends App {
    println("I will now generate the Verilog file!")
    chisel3.Driver.execute(Array("--target-dir", "generated"), () => new LittleRiscy())
}
