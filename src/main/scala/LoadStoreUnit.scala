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

    val wordAddress = io.addressBase + io.addressOffset
    val byteAddress = wordAddress / 4.U
    val remainder = wordAddress % 4.U

    when(function === 1.U){          // Load Byte
        // TODO
        loadedValue := io.memory.dataOut
        dataOut := 0.U
        writeEnable := false.B
    } .elsewhen(function === 2.U) {  // Load Halfword
        // TODO
        loadedValue := io.memory.dataOut
        dataOut := 0.U
        writeEnable := false.B
    } .elsewhen(function === 3.U) {  // Load Word
        loadedValue := io.memory.dataOut
        dataOut := 0.U
        writeEnable := false.B
    } .elsewhen(function === 4.U) {  // Store Byte
        // TODO
        dataOut := io.storeValue
        writeEnable := true.B

        loadedValue := 0.U
    } .elsewhen(function === 5.U) {  // Store Halfword
        // TODO
        dataOut := io.storeValue
        writeEnable := true.B

        loadedValue := 0.U
    } .elsewhen(function === 6.U) {  // Store Word
        dataOut := io.storeValue
        writeEnable := true.B
        loadedValue := 0.U
    } .otherwise {
        dataOut := 0.U
        loadedValue := 0.U
        writeEnable := false.B
    }

    io.loadedValue := loadedValue

    io.memory.address := wordAddress
    io.memory.write := writeEnable
    io.memory.dataIn := dataOut
}