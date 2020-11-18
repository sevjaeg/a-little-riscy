
import chisel3._
import chisel3.iotesters.PeekPokeTester

/**
 *
 */
class AluTest(dut: Alu) extends PeekPokeTester(dut) {
    val a : Long = -500
    val b : Long = 2
    for (op <- 0 to 9) {
        val result =
            op match {
                case 0 => 0
                case 1 => a << b
                case 2 => a >> b
                case 3 => a >> b    // TODO
                case 4 => a + b
                case 5 => a - b
                case 6 => if(a < b) 1 else 0
                case 7 => a | b
                case 8 => a ^ b
                case 9 => a & b
            }
        val resMask = result & 0xffff

        poke(dut.io.function, op)
        poke(dut.io.in1, a)
        poke(dut.io.in2, b)
        step(1)
        expect(dut.io.result, resMask)
    }
}

object AluTester extends App {
    println("Testing the ALU")
    iotesters.Driver.execute(Array("--target-dir", "generated", "--generate-vcd-output", "on"), () => new Alu()) {
        c => new AluTest(c)
    }
}