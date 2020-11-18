/*
 *
 */

import chisel3._
import chisel3.util._

class LoadStoreUnit extends Module {
    val io = IO(new Bundle {
        val function = Input(UInt(3.W))
        val storeValue = Input(UInt(32.W))
        val address = Input(UInt(32.W))         // word adress
        val offset = Input(UInt(12.W))
        val memIn = Input(UInt(32.W))
        val memOut = Output(UInt(32.W))
        val writeEnable = Output(UInt(1.W))
        val calculatedAdress = Output(UInt(32.W))
        val loadedValue = Output(UInt(32.W))
    })

    val function = io.function
    val loadedValue = Wire(UInt(32.W))
    val writeEnable = Wire(UInt(1.W))
    val memOut = Wire(UInt(32.W))
    val address = io.address + io.offset
    val calculatedAdress = address / 4.U
    val remainder = address % 4.U

    when(function === 1.U){          // Load Byte
        // TODO
        loadedValue := io.memIn
        memOut := 0.U
        writeEnable := false.B
    } .elsewhen(function === 2.U) {  // Load Halfword
        // TODO
        loadedValue := io.memIn
        memOut := 0.U
        writeEnable := false.B
    } .elsewhen(function === 3.U) {  // Load Word
        loadedValue := io.memIn
        memOut := 0.U
        writeEnable := false.B
    } .elsewhen(function === 4.U) {  // Store Byte
        // TODO
        memOut := io.storeValue
        writeEnable := true.B

        loadedValue := 0.U
    } .elsewhen(function === 5.U) {  // Store Halfword
        // TODO
        memOut := io.storeValue
        writeEnable := true.B

        loadedValue := 0.U
    } .elsewhen(function === 6.U) {  // Store Word
        memOut := io.storeValue
        writeEnable := true.B
        loadedValue := 0.U
    } .otherwise {
        memOut := 0.U
        loadedValue := 0.U
        writeEnable := false.B
    }

    io.loadedValue := loadedValue
    io.calculatedAdress := calculatedAdress
    io.writeEnable := writeEnable
    io.memOut := memOut
}