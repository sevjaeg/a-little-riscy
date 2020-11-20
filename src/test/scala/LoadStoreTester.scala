
import chisel3._
import chisel3.iotesters.PeekPokeTester

/**
 *
 */
class LoadStoreTester(dut: MemSys) extends PeekPokeTester(dut) {
    val rand = scala.util.Random

    var func_read: Int = 3
    var func_write : Int = 6

    var address : Int = 0
    var offset : Int = 0
    var storeValue : Int = 0

   /* for (address <- 0 to 63) {
        // Check empty memory
        poke(dut.io.function, func_read)
        poke(dut.io.addressBase, address)
        poke(dut.io.addressOffset, 0)
        step(1)
        expect(dut.io.loadedValue, 0)
    }*/

    // Store value to memory
    address = rand.nextInt(32)
    offset = rand.nextInt(32)
    storeValue = rand.nextInt(100)

    poke(dut.io.function, func_write)
    poke(dut.io.addressBase, address)
    poke(dut.io.addressOffset, offset)
    poke(dut.io.storeValue, storeValue)
    step(1)

    // Read stored value from memory
    poke(dut.io.function, func_read)
    poke(dut.io.addressBase, address)
    poke(dut.io.addressOffset, offset)
    poke(dut.io.storeValue, storeValue)
    step(1)
    expect(dut.io.loadedValue, storeValue)

    // Read stored value from memory with different offset
    var shift : Int = rand.nextInt(address)
    address = address - shift
    offset = offset + shift
    poke(dut.io.function, func_read)
    poke(dut.io.addressBase, address)
    poke(dut.io.addressOffset, offset)
    poke(dut.io.storeValue, storeValue)
    step(1)
    expect(dut.io.loadedValue, storeValue)

    // Store value to memory
    storeValue = 513
    address = 2
    offset = 10
    poke(dut.io.function, func_write)
    poke(dut.io.addressBase, address)
    poke(dut.io.addressOffset, offset)
    poke(dut.io.storeValue, storeValue)
    step(1)

    // Store value to memory
    storeValue = 98
    address = 4
    offset = 0
    poke(dut.io.function, func_write)
    poke(dut.io.addressBase, address)
    poke(dut.io.addressOffset, offset)
    poke(dut.io.storeValue, storeValue)
    step(1)

    // Read stored value from memory
    address = 4
    offset = 0
    poke(dut.io.function, func_read)
    poke(dut.io.addressBase, address)
    poke(dut.io.addressOffset, offset)
    poke(dut.io.storeValue, storeValue)
    step(1)
    expect(dut.io.loadedValue, 98)

    // Read stored value from memory
    address = 12
    offset = 0
    poke(dut.io.function, func_read)
    poke(dut.io.addressBase, address)
    poke(dut.io.addressOffset, offset)
    poke(dut.io.storeValue, storeValue)
    step(1)
    expect(dut.io.loadedValue, 513)
}

object LoadStoreTester extends App {
    println("Testing Load/Store Unit")
    iotesters.Driver.execute(Array("--target-dir", "generated", "--generate-vcd-output", "on"), () => new MemSys()) {
        c => new LoadStoreTester(c)
    }
}