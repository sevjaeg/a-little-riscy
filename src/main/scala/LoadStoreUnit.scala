/*
 *
 */

import chisel3._
import chisel3.util._

class LoadStoreUnit extends Module {
    val io = IO(new Bundle {
        val function = Input(UInt(3.W))
        val addressBase = Input(UInt(32.W))    // word address
        val addressOffset = Input(UInt(12.W))  // word offset
        val storeValue = Input(UInt(32.W))
        val loadedValue = Output(UInt(32.W))
        val memory = Flipped(new MemoryIO())
    })

    val function = io.function
    val loadedValue = Wire(UInt(32.W))
    val writeEnable = Wire(Bool())
    val dataOut = Wire(UInt(32.W))
    val writeMask = Wire(Vec(4, Bool()))

    val byteAddress = io.addressBase + io.addressOffset
    val wordAddress = byteAddress / 4.U
    val remainder = byteAddress % 4.U

    writeEnable := false.B
    dataOut := 0.U
    loadedValue := 0.U
    for (idx <- 0 to 3) {
        writeMask(idx) := false.B
    }

    when(function === 1.U){          // Load Byte
        // TODO
        loadedValue := io.memory.dataOut

    } .elsewhen(function === 2.U) {  // Load Halfword
        // TODO
        loadedValue := io.memory.dataOut

    } .elsewhen(function === 3.U) {  // Load Word
        loadedValue := io.memory.dataOut

    } .elsewhen(function === 4.U) {  // Store Byte
        // TODO
        writeEnable := true.B
        dataOut := io.storeValue
        for (idx <- 0 to 3) {
            writeMask(idx) := true.B
        }
    } .elsewhen(function === 5.U) {  // Store Halfword
        // TODO
        writeEnable := true.B
        dataOut := io.storeValue
        for (idx <- 0 to 3) {
            writeMask(idx) := true.B
        }
    } .elsewhen(function === 6.U) {  // Store Word
        writeEnable := true.B
        dataOut := io.storeValue
        for (idx <- 0 to 3) {
            writeMask(idx) := true.B
        }
    }

    io.loadedValue := loadedValue

    io.memory.address := wordAddress
    io.memory.write := writeEnable
    io.memory.dataIn := dataOut
    io.memory.writeMask := writeMask
}

/*
 * Bundle of Memory and LoadStoreUnit for reasonable Load/Store testing
 */

import chisel3._
import chisel3.util._


class MemSys extends Module {
    val io = IO(new Bundle {
        val function = Input(UInt(3.W))
        val addressBase = Input(UInt(32.W))    // word address
        val addressOffset = Input(UInt(12.W))  // word offset
        val storeValue = Input(UInt(32.W))
        val loadedValue = Output(UInt(32.W))
    })

    val ls = Module(new LoadStoreUnit())
    val mem = Module(new Memory())
    ls.io.memory <> mem.io
    ls.io.function := io.function
    ls.io.addressBase := io.addressBase
    ls.io.addressOffset := io.addressOffset
    ls.io.storeValue := io.storeValue
    io.loadedValue := ls.io.loadedValue
}

/**
 * An object extending App to generate the Verilog code.
 */
object MemSysMain extends App {
    println("I will now generate the Verilog file!")
    chisel3.Driver.execute(Array("--target-dir", "generated"), () => new MemSys())
}