
import chisel3._
import chisel3.iotesters.PeekPokeTester

/**
 *
 */
class LittleRiscyTester(dut: LittleRiscy) extends PeekPokeTester(dut) {
    for(i <- 0 to 7) {  // clock cycles
        poke(dut.io.debugPort.address, i)
        step(1)
        val v = peek(dut.io.debugPort.value)
        println("reg value @" + i +  ": " + v.toString)

    }
}

object LittleRiscyTester extends App {
    println("Testing LittleRiscy")
    iotesters.Driver.execute(Array("--target-dir", "generated", "--generate-vcd-output", "on"), () => new LittleRiscy()) {
        c => new LittleRiscyTester(c)
    }
}

/* Observations
 * FETCH
 *
 *
 *
 */