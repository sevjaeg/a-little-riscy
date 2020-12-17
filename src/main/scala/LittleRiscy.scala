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
    val registers = Module(new Registers())
    val pc = registers.io.pc


    // Memory
    val dataMemory = Module(new Memory())
    val instructionMemory = Module(new InstructionMemory())


    // Fetch Unit
    val fetchUnit = Module(new FetchUnit())
    fetchUnit.io.IMem <> instructionMemory.io
    fetchUnit.io.pcIn := pc
    registers.io.newPc := fetchUnit.io.pcOut

    // Instruction Queue
    val instructionQueue = Module(new RegFifo(UInt(128.W), 6))
    fetchUnit.io.queue <> instructionQueue.io.enq


    // Dispatcher
    val dispatcher = Module(new Dispatcher)

    instructionQueue.io.deq.ready := dispatcher.io.ready
    dispatcher.io.instructions := instructionQueue.io.deq.bits
    dispatcher.io.instructionsValid := instructionQueue.io.deq.valid

    dispatcher.io.regPortAlu <> registers.io.portAlu.read
    dispatcher.io.regPortLoadStore <> registers.io.portLoadStore.read


    // ALU
    val alu = Module(new Alu())
    alu.io.in <> dispatcher.io.aluOut


    // Load/Store
    val loadStore = Module(new LoadStoreUnit())
    loadStore.io.in <> dispatcher.io.loadStoreOut
    loadStore.io.memory <> dataMemory.io


    // Write Back
    registers.io.portAlu.write.rd := alu.io.CDBout.rd
    registers.io.portAlu.write.value := alu.io.CDBout.value
    registers.io.portLoadStore.write.rd := loadStore.io.CDBout.rd
    registers.io.portLoadStore.write.value := loadStore.io.CDBout.value


    //
    io.led := 0.U
}

/**
 * An object extending App to generate the Verilog code.
 */
object LittleRiscyMain extends App {
    println("I will now generate the Verilog file!")
    chisel3.Driver.execute(Array("--target-dir", "generated"), () => new LittleRiscy())
}
