/*
 *
 */

import chisel3._
import chisel3.util._

class LoadStoreUnit extends Module {
    val io = IO(new LoadStoreIO())
    val loadedValue = Wire(UInt(32.W))
    val writeEnable = Wire(Bool())
    val dataOut = Wire(UInt(32.W))
    val writeMask = Wire(Vec(4, Bool()))

    val byteAddress = io.in.addressBase + io.in.addressOffset
    val wordAddress = (byteAddress >> 2.U).asUInt()
    val remainder = byteAddress(1,0)

    writeEnable := false.B
    dataOut := 0.U
    loadedValue := 0.U
    for (idx <- 0 to 3) {
        writeMask(idx) := false.B
    }

    switch(io.in.function) {
        is("b1000".U) { // Load Byte Signed
            val readByte = Wire(Bits(8.W))
            readByte := 0.U
            for (rem <- 0 to 3) {
                when(remainder === rem.U) {
                    readByte := (io.memory.dataOut >> (rem * 8).U)(7, 0)
                }
            }
            val byteExtended = Wire(SInt(32.W))
            byteExtended := readByte.asSInt()
            loadedValue := byteExtended.asUInt()
        }
        is("b1001".U) { // Load Halfword Signed
            val readBytes = Wire(Bits(16.W))
            readBytes := 0.U
            when(remainder === 0.U) {
                readBytes := io.memory.dataOut(15, 0)
            } .elsewhen(remainder === 2.U) {
                readBytes := (io.memory.dataOut >> 16.U)(15, 0)
            }
            val bytesExtended = Wire(SInt(32.W))
            bytesExtended := readBytes.asSInt()
            loadedValue := bytesExtended.asUInt()
        }
        is("b1010".U) { // Load Word
            loadedValue := io.memory.dataOut
        }
        is("b1100".U) { // Load Byte Unsigned
            val readByte = Wire(Bits(8.W))
            readByte := 0.U
            for (rem <- 0 to 3) {
                when(remainder === rem.U) {
                    readByte := (io.memory.dataOut >> (rem * 8).U)(7, 0)
                }
            }
            loadedValue := Cat(0.U(24.W), readByte)
        }
        is("b1101".U) { // Load Halfword Unsigned
            val readBytes = Wire(Bits(16.W))
            readBytes := 0.U
            when(remainder === 0.U) {
                readBytes := io.memory.dataOut(15, 0)
            } .elsewhen(remainder === 2.U) {
                readBytes := (io.memory.dataOut >> 16.U)(15, 0)
            }
            loadedValue := Cat(0.U(16.W), readBytes).asUInt()
        }
        is("b0111".U) { // Store Byte
            writeEnable := true.B
            for (rem <- 0 to 3) {
                when(remainder === rem.U) {
                    dataOut := (io.in.storeValue << (rem * 8).U)(31,0).asUInt()
                    writeMask(rem) := true.B
                }
            }
        }
        is("b0110".U) { // Store Halfword
            writeEnable := true.B
            when(remainder === 0.U) {
                dataOut := io.in.storeValue
                writeMask(0) := true.B
                writeMask(1) := true.B
            } .elsewhen(remainder === 2.U) {
                dataOut := (io.in.storeValue << 16.U)(31,0).asUInt()
                writeMask(2) := true.B
                writeMask(3) := true.B
            }
        }
        is("b0101".U) { // Store Word
            writeEnable := true.B
            dataOut := io.in.storeValue
            for (idx <- 0 to 3) {
                writeMask(idx) := true.B
            }
        }
    }

    val loadedValueRegister = RegInit(0.U(32.W))
    loadedValueRegister := loadedValue
    io.CDBout.value := loadedValueRegister

    val rdRegister =  RegInit(0.U(5.W))
    rdRegister := io.in.rd
    io.CDBout.rd := rdRegister

    io.memory.address := wordAddress
    io.memory.write := writeEnable
    io.memory.dataIn := dataOut
    io.memory.writeMask := writeMask
}

class LoadStoreInIO() extends Bundle {
    val function = Input(UInt(4.W))
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
    val CDBout = Flipped(new CDBInIO())
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
    ls.io.CDBout <> io.out
}

/**
 * An object extending App to generate the Verilog code.
 */
object MemSysMain extends App {
    println("I will now generate the Verilog file!")
    chisel3.Driver.execute(Array("--target-dir", "generated"), () => new MemSys())
}