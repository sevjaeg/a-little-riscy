
import chisel3._
import chisel3.iotesters.PeekPokeTester

/**
 *
 */
class LoadStoreTester(dut: MemSys) extends PeekPokeTester(dut) {
    val rand = scala.util.Random

    var func_read_b : Int = 1
    var func_read_h : Int = 2
    var func_read : Int = 3
    var func_write_b : Int = 4
    var func_write_h : Int = 5
    var func_write : Int = 6

    var address : Int = 0
    var offset : Int = 0
    var storeValue : Int = 0

    for (i <- 0 to 5) {
        // Store word to memory
        address = rand.nextInt(32) * 4
        offset = rand.nextInt(32) * 4
        storeValue = rand.nextInt(10000)

        println("Wrote word to Mem @" + (address + offset).toString() + ": " + storeValue)

        poke(dut.io.in.function, func_write)
        poke(dut.io.in.addressBase, address)
        poke(dut.io.in.addressOffset, offset)
        poke(dut.io.in.storeValue, storeValue)
        step(1)

        // Read stored value from memory
        poke(dut.io.in.function, func_read)
        poke(dut.io.in.addressBase, address)
        poke(dut.io.in.addressOffset, offset)
        poke(dut.io.in.storeValue, storeValue)
        step(1)
        expect(dut.io.out.loadedValue, storeValue)

        // Read stored value from memory with different offset
        var shift: Int = rand.nextInt(address)
        address = address - shift
        offset = offset + shift
        poke(dut.io.in.function, func_read)
        poke(dut.io.in.addressBase, address)
        poke(dut.io.in.addressOffset, offset)
        poke(dut.io.in.storeValue, storeValue)
        step(1)
        expect(dut.io.out.loadedValue, storeValue)
    }

    for (i <- 0 to 5) {
        // Store halfword to memory
        address = rand.nextInt(32) * 2
        offset = rand.nextInt(32) * 2
        storeValue = rand.nextInt(1000)

        println("Wrote halfword to Mem @" + (address + offset).toString() + ": " + storeValue)

        poke(dut.io.in.function, func_write_h)
        poke(dut.io.in.addressBase, address)
        poke(dut.io.in.addressOffset, offset)
        poke(dut.io.in.storeValue, storeValue)
        step(1)

        // Read stored value from memory
        poke(dut.io.in.function, func_read_h)
        poke(dut.io.in.addressBase, address)
        poke(dut.io.in.addressOffset, offset)
        poke(dut.io.in.storeValue, storeValue)
        step(1)
        expect(dut.io.out.loadedValue, storeValue)

        // Read stored value from memory with different offset
        var shift: Int = rand.nextInt(address)
        address = address - shift
        offset = offset + shift
        poke(dut.io.in.function, func_read_h)
        poke(dut.io.in.addressBase, address)
        poke(dut.io.in.addressOffset, offset)
        poke(dut.io.in.storeValue, storeValue)
        step(1)
        expect(dut.io.out.loadedValue, storeValue)
    }

    for (i <- 0 to 5) {
        // Store byte to memory
        address = rand.nextInt(32) * 4
        offset = rand.nextInt(32)
        storeValue = rand.nextInt(255)

        println("Wrote byte to Mem @" + (address + offset).toString() + ": " + storeValue)

        poke(dut.io.in.function, func_write_b)
        poke(dut.io.in.addressBase, address)
        poke(dut.io.in.addressOffset, offset)
        poke(dut.io.in.storeValue, storeValue)
        step(1)

        // Read stored value from memory
        poke(dut.io.in.function, func_read_b)
        poke(dut.io.in.addressBase, address)
        poke(dut.io.in.addressOffset, offset)
        poke(dut.io.in.storeValue, storeValue)
        step(1)
        expect(dut.io.out.loadedValue, storeValue)

        // Read stored value from memory with different offset
        var shift: Int = rand.nextInt(address)
        address = address - shift
        offset = offset + shift
        poke(dut.io.in.function, func_read_b)
        poke(dut.io.in.addressBase, address)
        poke(dut.io.in.addressOffset, offset)
        poke(dut.io.in.storeValue, storeValue)
        step(1)
        expect(dut.io.out.loadedValue, storeValue)
    }

    println()
    // Read whole memory
    for (idx <- 0 to 255 by 4) {
        poke(dut.io.in.function, func_read)
        poke(dut.io.in.addressBase, idx)
        poke(dut.io.in.addressOffset, 0)
        poke(dut.io.in.storeValue, 0)
        step(1)
        println("Memory @"+idx+": "+peek(dut.io.out.loadedValue).toString())
    }
}

object LoadStoreTester extends App {
    println("Testing Load/Store Unit")
    iotesters.Driver.execute(Array("--target-dir", "generated", "--generate-vcd-output", "on"), () => new MemSys()) {
        c => new LoadStoreTester(c)
    }
}