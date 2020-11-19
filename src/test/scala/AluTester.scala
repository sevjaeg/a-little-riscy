
import chisel3._
import chisel3.iotesters.PeekPokeTester

/**
 *
 */
class AluTester(dut: Alu) extends PeekPokeTester(dut) {
    val a : Int = -100
    val b : Int = 2
    println("a = " + a.toString + " = " + a.toHexString)
    println("b = " + b.toString + " = " + b.toHexString)
    for (op <- 0 to 9) {
        val result =
            op match {
                case 0 => 0
                case 1 => a << b
                case 2 => a >>> b // TODO
                case 3 => a >> b
                case 4 => a + b
                case 5 => a - b
                case 6 => if(a < b) 1 else 0
                case 7 => if(a < b) 1 else 0
                case 8 => a | b
                case 9 => a ^ b
                case 10 => a & b
            }
        println(op.toString + ": " + result.toString + " = " +result.toHexString)

        poke(dut.io.function, op)
        poke(dut.io.in1, a)
        poke(dut.io.in2, b)
        step(1)
        expect(dut.io.result, result)
    }
}

object AluTester extends App {
    println("Testing the ALU")
    iotesters.Driver.execute(Array("--target-dir", "generated", "--generate-vcd-output", "on"), () => new Alu()) {
        c => new AluTester(c)
    }
}