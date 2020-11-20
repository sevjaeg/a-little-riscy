
import chisel3._
import chisel3.iotesters.PeekPokeTester

/**
 *
 */
class MemoryTest(dut: Memory) extends PeekPokeTester(dut) {
    val data : Int = 12
    val address : Int = 4

    //val writeMask = Vector(true, true, true, true)

    // Write to memory
    poke(dut.io.write, true)
    poke(dut.io.address, address)
    poke(dut.io.dataIn, data)
    //poke(dut.io.writeMask, writeMask)
    step(1)

    // Check if output was zero for read
    expect(dut.io.dataOut, 0)

    // Read from memory
    poke(dut.io.write, false)
    poke(dut.io.address, address)
    poke(dut.io.dataIn, data)
    step(1)

    // Check result
    expect(dut.io.dataOut, data)
}

object MemoryTester extends App {
    println("Testing the Memory")
    iotesters.Driver.execute(Array("--target-dir", "generated", "--generate-vcd-output", "on"), () => new Memory()) {
        c => new MemoryTest(c)
    }
}