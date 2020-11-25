/*
 *
 */

import chisel3._
import chisel3.util._

class LoadStoreInIO() extends Bundle {
    val function = Input(UInt(3.W))
    val addressBase = Input(UInt(32.W))    // word address
    val addressOffset = Input(UInt(12.W))  // word offset
    val storeValue = Input(UInt(32.W))
    val rd = Input(UInt(5.W))
}

class LoadStoreOutIO() extends Bundle {
    val loadedValue = Output(UInt(32.W))
    val rd = Output(UInt(5.W))
}

class LoadStoreUnit extends Module {
    val io = IO(new Bundle {
        val in = new LoadStoreInIO()
        val out = new LoadStoreOutIO()
        val memory = Flipped(new MemoryIO())
    })

    val function = io.in.function
    val loadedValue = Wire(UInt(32.W))
    val writeEnable = Wire(Bool())
    val dataOut = Wire(UInt(32.W))
    val writeMask = Wire(Vec(4, Bool()))

    val byteAddress = io.in.addressBase + io.in.addressOffset
    val wordAddress = byteAddress / 4.U
    val remainder = byteAddress % 4.U

    writeEnable := false.B
    dataOut := 0.U
    loadedValue := 0.U
    for (idx <- 0 to 3) {
        writeMask(idx) := false.B
    }

    when(function === 1.U){          // Load Byte
        // TODO chisel type of remainder does not support indexing
        //val readByte = io.memory.dataOut((remainder + 1.U) * 8.U - 1.U, remainder * 8.U)
        //val readByte = io.memory.dataOut((remainder + 1) * 8 - 1, remainder * 8)
        //loadedValue := Cat(0.U(24.W),readByte)
        loadedValue := io.memory.dataOut

    } .elsewhen(function === 2.U) {  // Load Halfword
        // TODO chisel type of remainder does not support indexing
        // TODO remainder % 2 === 0 ?
        // Would work for remainder === 1, but nor for remainder === 3
        //val readBytes = io.memory.dataOut((remainder + 2.U) * 8.U - 1.U, remainder * 8.U)
        //loadedValue := Cat(0.U(16.W),readBytes)
        loadedValue := io.memory.dataOut

    } .elsewhen(function === 3.U) {  // Load Word
        // TODO remainder zero?
        loadedValue := io.memory.dataOut

    } .elsewhen(function === 4.U) {  // Store Byte
        // TODO Check little vs big endian
        writeEnable := true.B
        dataOut := io.in.storeValue << remainder * 8.U
        when(remainder === 0.U) {
            writeMask(3) := true.B
        } .elsewhen(remainder === 1.U) {
            writeMask(2) := true.B
        } .elsewhen(remainder === 2.U) {
            writeMask(1) := true.B
        } .elsewhen(remainder === 3.U) {
            writeMask(0) := true.B
        }

    } .elsewhen(function === 5.U) {  // Store Halfword
        // TODO remainder % 2 === 0 ?
        writeEnable := true.B
        dataOut := io.in.storeValue
        for (idx <- 0 to 3) {
            writeMask(idx) := true.B
        }

    } .elsewhen(function === 6.U) {  // Store Word
        // TODO remainder zero?
        writeEnable := true.B
        dataOut := io.in.storeValue
        for (idx <- 0 to 3) {
            writeMask(idx) := true.B
        }
    }

    val loadedValueRegister = RegInit(0.U(32.W))
    loadedValueRegister := loadedValue
    io.out.loadedValue := loadedValueRegister

    val rdRegister =  RegInit(0.U(5.W))
    rdRegister := io.in.rd
    io.out.rd := rdRegister

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
    ls.io.in.function := io.function
    ls.io.in.addressBase := io.addressBase
    ls.io.in.addressOffset := io.addressOffset
    ls.io.in.storeValue := io.storeValue
    io.loadedValue := ls.io.out.loadedValue
}

/**
 * An object extending App to generate the Verilog code.
 */
object MemSysMain extends App {
    println("I will now generate the Verilog file!")
    chisel3.Driver.execute(Array("--target-dir", "generated"), () => new MemSys())
}