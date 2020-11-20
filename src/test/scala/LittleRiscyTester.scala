
import chisel3._
import chisel3.iotesters.PeekPokeTester

/**
 *
 */
class LittleRiscyTester(dut: LittleRiscy) extends PeekPokeTester(dut) {
    for(i <- 0 to 7) {  // clock cycles

        // fetched instructions
        val i1 = peek(dut.io.i1)
        println("i1: " + i1.toString)

        poke(dut.io.aluFn, 3)
        poke(dut.io.aluIn1, 500)
        poke(dut.io.aluIn2, 2)
        poke(dut.io.aluRd, 4)
        val aluRes = peek(dut.io.aluRes)
        println("aluRes: " + aluRes.toString)

        step(1)
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