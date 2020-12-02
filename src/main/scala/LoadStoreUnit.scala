/*
 *
 */

import chisel3._
import chisel3.util._

class LoadStoreUnit extends Module {
    val io = IO(new LoadStoreIO())

    val function = io.in.function
    val loadedValue = Wire(UInt(32.W))
    val writeEnable = Wire(Bool())
    val dataOut = Wire(UInt(32.W))
    val writeMask = Wire(Vec(4, Bool()))

    val byteAddress = io.in.addressBase + io.in.addressOffset
    val wordAddress = (byteAddress >> 2.U).asUInt()
    val remainder = byteAddress - (wordAddress << 2.U)

    writeEnable := false.B
    dataOut := 0.U
    loadedValue := 0.U
    for (idx <- 0 to 3) {
        writeMask(idx) := false.B
    }

    when(function === 1.U) {          // Load Byte
        val readByte = Wire(UInt(8.W))
        readByte := 0.U
        for (rem <- 0 to 3) {
            when(remainder === rem.U) {
                readByte := (io.memory.dataOut >> (rem * 8).U).asUInt()(7,0)
            }
        }
        loadedValue := Cat(0.U(24.W),readByte)

    } .elsewhen(function === 2.U) {  // Load Halfword
        val readBytes = Wire(UInt(16.W))
        readBytes := 0.U
        for (rem_half <- 0 to 1) {
            when(remainder === (rem_half*2).U) {
                readBytes := (io.memory.dataOut >> (rem_half * 16).U).asUInt()(15,0)
            }
        }
        loadedValue := Cat(0.U(16.W),readBytes)

    } .elsewhen(function === 3.U) {  // Load Word
        loadedValue := io.memory.dataOut

    } .elsewhen(function === 4.U) {  // Store Byte
        writeEnable := true.B
        for (rem <- 0 to 3) {
            when(remainder === rem.U) {
                dataOut := (io.in.storeValue << (rem * 8).U).asUInt()
                writeMask(rem) := true.B
            }
        }

    } .elsewhen(function === 5.U) {  // Store Halfword
        writeEnable := true.B
        for (rem_half <- 0 to 1) {
            when(remainder === (rem_half*2).U) {
                dataOut := (io.in.storeValue << (rem_half * 16).U).asUInt()(31,0)
                writeMask(rem_half*2) := true.B
                writeMask(rem_half*2+1) := true.B
            }
        }

    } .elsewhen(function === 6.U) {  // Store Word
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

class LoadStoreIO() extends Bundle {
    val in = new LoadStoreInIO()
    val out = new LoadStoreOutIO()
    val memory = Flipped(new MemoryIO())
}

/*
 * Bundle of Memory and LoadStoreUnit for reasonable Load/Store testing
 */

class MemSys extends Module {
    val io = IO(new Bundle {
        val in = new LoadStoreInIO()
        val out = new LoadStoreOutIO()
    })


    val ls = Module(new LoadStoreUnit())
    val mem = Module(new Memory())
    ls.io.memory <> mem.io
    ls.io.in <> io.in
    ls.io.out <> io.out
}

/**
 * An object extending App to generate the Verilog code.
 */
object MemSysMain extends App {
    println("I will now generate the Verilog file!")
    chisel3.Driver.execute(Array("--target-dir", "generated"), () => new MemSys())
}