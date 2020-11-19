
import chisel3._
import chisel3.iotesters.PeekPokeTester

/**
 *
 */
class LoadStoreTester(dut: MemSys) extends PeekPokeTester(dut) {
    // load word from empty memory
    var function : Int = 3
    var address : Int = 4
    var offset : Int = 0
    var storeValue : Int = 25
    poke(dut.io.function, function)
    poke(dut.io.addressBase, address)
    poke(dut.io.addressOffset, offset)
    poke(dut.io.storeValue, storeValue)
    step(1)
    expect(dut.io.loadedValue, 0)

    // store value to memory
    function = 6
    poke(dut.io.function, function)
    poke(dut.io.addressBase, address)
    poke(dut.io.addressOffset, offset)
    poke(dut.io.storeValue, storeValue)
    step(1)

    // read stored value from memory
    function = 3
    address = 4
    offset = 0
    poke(dut.io.function, function)
    poke(dut.io.addressBase, address)
    poke(dut.io.addressOffset, offset)
    poke(dut.io.storeValue, storeValue)
    step(1)
    expect(dut.io.loadedValue, storeValue)

    // read stored value from memory with offset
    function = 3
    address = 2
    offset = 2
    poke(dut.io.function, function)
    poke(dut.io.addressBase, address)
    poke(dut.io.addressOffset, offset)
    poke(dut.io.storeValue, storeValue)
    step(1)
    expect(dut.io.loadedValue, storeValue)

    // store value to memory
    function = 6
    storeValue = 513
    address = 2
    offset = 10
    poke(dut.io.function, function)
    poke(dut.io.addressBase, address)
    poke(dut.io.addressOffset, offset)
    poke(dut.io.storeValue, storeValue)
    step(1)

    // store value to memory
    function = 6
    storeValue = 98
    address = 4
    offset = 0
    poke(dut.io.function, function)
    poke(dut.io.addressBase, address)
    poke(dut.io.addressOffset, offset)
    poke(dut.io.storeValue, storeValue)
    step(1)

    // read stored value from memory
    function = 3
    address = 4
    offset = 0
    poke(dut.io.function, function)
    poke(dut.io.addressBase, address)
    poke(dut.io.addressOffset, offset)
    poke(dut.io.storeValue, storeValue)
    step(1)
    expect(dut.io.loadedValue, 98)

    // read stored value from memory
    function = 3
    address = 12
    offset = 0
    poke(dut.io.function, function)
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