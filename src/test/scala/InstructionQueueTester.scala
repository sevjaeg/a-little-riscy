
import chisel3.iotesters
import chisel3.iotesters.PeekPokeTester

/**
 *
 */
class InstructionQueueTester(dut: InstructionQueue) extends PeekPokeTester(dut) {
    for (i <- 1 to 10) {
        poke(dut.io.enq.bits1, i)
        poke(dut.io.enq.bits2, i+20)
        poke(dut.io.deq.ready1, true)
        poke(dut.io.deq.ready2, true)
        step(1)
    }
}

object InstructionQueueTester extends App {
    println("Testing Load/Store Unit")
    iotesters.Driver.execute(Array("--target-dir", "generated", "--generate-vcd-output", "on"), () => new InstructionQueue(6)) {
        c => new InstructionQueueTester(c)
    }
}