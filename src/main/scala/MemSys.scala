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