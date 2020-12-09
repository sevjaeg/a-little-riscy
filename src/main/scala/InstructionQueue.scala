/*
 *
 */


import chisel3._
import chisel3.util._
import chisel3.util.DecoupledIO

class InstructionQueueInIO() extends Bundle {
    val ready = Output(Bool())
    val bits1 = Input(UInt(64.W))
    val bits2 = Input(UInt(64.W))
}

class InstructionQueueOutIO() extends Bundle {
    val ready1 = Input(Bool())
    val ready2 = Input(Bool())
    val valid1 = Output(Bool())
    val valid2 = Output(Bool())
    val bits1 = Output(UInt(64.W))
    val bits2 = Output(UInt(64.W))
}


class InstructionQueue(depth: Int) extends Module {
    val io = IO(new Bundle {
        val enq = new InstructionQueueInIO()
        val deq = new InstructionQueueOutIO()
    })

    def counter(depth: Int, incrOne: Bool, incrTwo: Bool): (UInt, UInt, UInt, UInt) = {
        val cntReg = RegInit(0.U(log2Ceil(depth).W))
        val nextVal = Mux(cntReg === (depth-1).U, 0.U, cntReg + 1.U)
        val secondNextVal = Mux(nextVal === (depth-1).U, 0.U, nextVal + 1.U)
        val thirdNextVal = Mux(secondNextVal === (depth-1).U, 0.U, secondNextVal + 1.U)
        when (incrOne) {
            cntReg := nextVal
        } .elsewhen(incrTwo) {
            cntReg := secondNextVal
        }
        (cntReg, nextVal, secondNextVal, thirdNextVal)
    }

    // the register based memory
    val memReg = Reg(Vec(depth, UInt(64.W)))

    val incrOneRead = WireInit(false.B)
    val incrTwoRead = WireInit(false.B)
    val incrTwoWrite = WireInit(false.B)
    val (readPtr, nextRead, secondNextRead, thirdNextRead) = counter(depth, incrOneRead, incrTwoRead)
    val (writePtr, nextWrite,secondNextWrite, thirdNextWrite) = counter(depth, false.B, incrTwoWrite)

    val emptyReg = RegInit(true.B)
    val oneLeftReg = RegInit(false.B)
    val oneFreeReg = RegInit(false.B)
    val fullReg = RegInit(false.B)

    when (!oneFreeReg && !fullReg) {  // at least two slots free --> write
        memReg(writePtr) := io.enq.bits1
        memReg(nextWrite) := io.enq.bits2
        emptyReg := false.B
        oneFreeReg := false.B
        fullReg := secondNextRead === readPtr
        oneLeftReg := thirdNextRead === readPtr
        incrTwoWrite := true.B
    }

    when (io.deq.ready1 && io.deq.ready2 && !emptyReg && !oneLeftReg) {  // read both
        fullReg := false.B
        oneFreeReg := false.B
        emptyReg := secondNextRead === writePtr
        oneLeftReg := thirdNextRead === writePtr
        incrOneRead := false.B
        incrTwoRead := true.B
    }

    when (io.deq.ready1 && !emptyReg) {  // read one (data 1)
        oneLeftReg := fullReg
        fullReg := false.B
        emptyReg := nextRead === writePtr
        oneLeftReg := secondNextRead === writePtr
        incrOneRead := true.B
        incrTwoRead := false.B
    }

    io.deq.bits1 := memReg(readPtr)
    io.deq.bits2 := memReg(nextRead)
    io.enq.ready := (!fullReg && !oneFreeReg)
    io.deq.valid1 := !emptyReg
    io.deq.valid2 := !oneLeftReg

}



class RegFifo[T <: Data](gen: T, depth: Int) extends Fifo(gen: T, depth: Int) {

    def counter(depth: Int, incr: Bool): (UInt, UInt) = {
        val cntReg = RegInit(0.U(log2Ceil(depth).W))
        val nextVal = Mux(cntReg === (depth-1).U, 0.U, cntReg + 1.U)
        when (incr) {
            cntReg := nextVal
        }
        (cntReg, nextVal)
    }

    // the register based memory
    val memReg = Reg(Vec(depth, gen))

    val incrRead = WireInit(false.B)
    val incrWrite = WireInit(false.B)
    val (readPtr, nextRead) = counter(depth, incrRead)
    val (writePtr, nextWrite) = counter(depth, incrWrite)

    val emptyReg = RegInit(true.B)
    val fullReg = RegInit(false.B)

    when (io.enq.valid && !fullReg) {
        memReg(writePtr) := io.enq.bits
        emptyReg := false.B
        fullReg := nextWrite === readPtr
        incrWrite := true.B
    }

    when (io.deq.ready && !emptyReg) {
        fullReg := false.B
        emptyReg := nextRead === writePtr
        incrRead := true.B
    }

    io.deq.bits := memReg(readPtr)
    io.enq.ready := !fullReg
    io.deq.valid := !emptyReg
}

/**
 * FIFO IO with enqueue and dequeue ports using the ready/valid interface.
 */
class FifoIO[T <: Data](private val gen: T) extends Bundle {
    val enq = Flipped(new DecoupledIO(gen))
    val deq = new DecoupledIO(gen)
}

/**
 * Base class for all FIFOs.
 */
abstract class Fifo[T <: Data](gen: T, depth: Int) extends Module {
    val io = IO(new FifoIO(gen))

    assert(depth > 0, "Number of buffer elements needs to be larger than 0")
}