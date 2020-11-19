
import chisel3._
import chisel3.iotesters.PeekPokeTester

/**
 *
 */
class LittleRiscyTester(dut: LittleRiscy) extends PeekPokeTester(dut) {

}

object LittleRiscyTester extends App {
    println("Testing LittleRiscy")
    iotesters.Driver.execute(Array("--target-dir", "generated", "--generate-vcd-output", "on"), () => new LittleRiscy()) {
        c => new LittleRiscyTester(c)
    }
}