
import chisel3._
import chisel3.iotesters.PeekPokeTester

/**
 *
 */
class FetchTester(dut: FetchUnit) extends PeekPokeTester(dut) {

}

object FetchTester extends App {
    println("Testing Fetch Unit")
    iotesters.Driver.execute(Array("--target-dir", "generated", "--generate-vcd-output", "on"), () => new FetchUnit()) {
        c => new FetchTester(c)
    }
}