/*
 * 
 */

import chisel3._

class LittleRiscy extends Module {
  val io = IO(new Bundle {
    val led = Output(UInt(1.W))
  })
  val CNT_MAX = (50000000 / 2 - 1).U; // 50000000 for FPGA (1 Hz), 100000 for simulation

  val cntReg = RegInit(0.U(32.W))
  val blkReg = RegInit(0.U(1.W))

  cntReg := cntReg + 1.U
  when(cntReg === CNT_MAX) {
    cntReg := 0.U
    blkReg := ~blkReg
  }
  io.led := blkReg
}

/**
 * An object extending App to generate the Verilog code.
 */
object ALittleRiscyMain extends App {
  println("I will now generate the Verilog file!")
  chisel3.Driver.execute(Array("--target-dir", "generated"), () => new LittleRiscy())
}
