/*
 * 
 */

import chisel3._

class LittleRiscy extends Module {
  val io = IO(new Bundle {
    val led = Output(UInt(1.W))
  })

  val controlUnit = Module(new ControlUnit())
  val memD = Mem(128, UInt(32.W))
  val memI = Mem(128, UInt(32.W))

  memD(32) :=  1.U(32.W)

  io.led := memD(24) === memD(32)
}

/**
 * An object extending App to generate the Verilog code.
 */
object ALittleRiscyMain extends App {
  println("I will now generate the Verilog file!")
  chisel3.Driver.execute(Array("--target-dir", "generated"), () => new LittleRiscy())
  chisel3.Driver.execute(Array("--target-dir", "generated"), () => new ControlUnit())
}
