/*
 *
 */

import chisel3._
import chisel3.util._

class LittleRiscy extends Module {
    val io = IO(new Bundle {
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
    registers.io.portFetchUnit <> fetchUnit.io.registers
    registers.io.newPc := fetchUnit.io.pcOut

    // Instruction Queue
    val instructionQueue = Module(new RegFifo(UInt(96.W), 4))
    fetchUnit.io.queue <> instructionQueue.io.enq

    // Dispatcher
    val dispatcher = Module(new Dispatcher)
    instructionQueue.io.deq.ready := dispatcher.io.ready
    dispatcher.io.instructions := instructionQueue.io.deq.bits
    dispatcher.io.instructionsValid := instructionQueue.io.deq.valid
    dispatcher.io.regPortAlu <> registers.io.portAlu.read
    dispatcher.io.regPortLoadStore <> registers.io.portLoadStore.read
    fetchUnit.io.pipelineFlushed := dispatcher.io.pipelineFlushed

    // ALU
    val alu = Module(new Alu())
    alu.io.in <> dispatcher.io.aluOut

    // Load/Store
    val loadStore = Module(new LoadStoreUnit())
    loadStore.io.in <> dispatcher.io.loadStoreOut
    loadStore.io.memory <> dataMemory.io

    // Operand forwarding
    alu.io.inFwd.aluFwd := alu.io.out.value
    alu.io.inFwd.lsuFwd := loadStore.io.out.value
    loadStore.io.inFwd.aluFwd := alu.io.out.value
    loadStore.io.inFwd.lsuFwd := loadStore.io.out.value

    // Write Back
    registers.io.portAlu.write.rd := alu.io.out.rd
    registers.io.portAlu.write.value := alu.io.out.value
    registers.io.portLoadStore.write.rd := loadStore.io.out.rd
    registers.io.portLoadStore.write.value := loadStore.io.out.value

    // Debug LED, necessary for FPGA evaluation
    io.led := alu.io.out.value(15,0)
}

/**
 * An object extending App to generate the Verilog code.
 */
object LittleRiscyMain extends App {
    println("I will now generate the Verilog file!")
    chisel3.Driver.execute(Array("--target-dir", "generated"), () => new LittleRiscy())
}
