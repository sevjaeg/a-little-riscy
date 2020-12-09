/*
 *
 */

import chisel3._
import chisel3.util._

class LittleRiscy extends Module {
    val io = IO(new Bundle {
        val sw = Input(UInt(16.W))
        val led = Output(UInt(16.W))
        val debugPort = new RegisterSingleReadIO()
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
    val instruction2 = fetchUnit.io.i2


    // Instruction Queue
    val instructionQueue = Module(new RegFifo(UInt(64.W), 4))
    instructionQueue.io.enq.bits := fetchUnit.io.i1

    val instructionReady = instructionQueue.io.enq.ready // TODO maybe more relevant conditions?
    instructionQueue.io.enq.valid := instructionReady


    // Dispatcher
    val dispatcher = Module(new Dispatcher)

    // TODO
    instructionQueue.io.deq.ready := true.B
    val readReady = instructionQueue.io.deq.valid
    when(readReady) {
        dispatcher.io.instruction := instructionQueue.io.deq.bits
    } .otherwise {
        dispatcher.io.instruction := 0.U  // TODO
    }

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
    registers.io.portAlu.write.address := alu.io.out.rd
    registers.io.portAlu.write.value := alu.io.out.result
    registers.io.portLoadStore.write.address := loadStore.io.out.rd
    registers.io.portLoadStore.write.value := loadStore.io.out.loadedValue


    // Debugging
    registers.io.portDebug.address := io.debugPort.address
    io.debugPort.value := registers.io.portDebug.value

    io.led := Cat(io.debugPort.value(15, 0))
}

/**
 * An object extending App to generate the Verilog code.
 */
object LittleRiscyMain extends App {
    println("I will now generate the Verilog file!")
    chisel3.Driver.execute(Array("--target-dir", "generated"), () => new LittleRiscy())
}
