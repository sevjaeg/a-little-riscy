
import chisel3._
import chisel3.iotesters.PeekPokeTester

/**
 *
 */
class LittleRiscyTester(dut: LittleRiscy) extends PeekPokeTester(dut) {
    var addr: Int = 2
    for(i <- 0 to 50) {  // clock cycles
        poke(dut.io.debugPort.address, addr)
        step(1)
        val v = peek(dut.io.debugPort.value)
        println("reg value @" + addr +  ": " + v.toString)

    }
}

object LittleRiscyTester extends App {
    println("Testing LittleRiscy")
    iotesters.Driver.execute(Array("--target-dir", "generated", "--generate-vcd-output", "on"), () => new LittleRiscy()) {
        c => new LittleRiscyTester(c)
    }
}
