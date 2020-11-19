
import chisel3._
import chisel3.iotesters.PeekPokeTester

/**
 *
 */
class LoadStoreTester(dut: LoadStoreUnit) extends PeekPokeTester(dut) {

}

object LoadStoreTester extends App {
    println("Testing Load/Store Unit")
    iotesters.Driver.execute(Array("--target-dir", "generated", "--generate-vcd-output", "on"), () => new LoadStoreUnit()) {
        c => new LoadStoreTester(c)
    }
}