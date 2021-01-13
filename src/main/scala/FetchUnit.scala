/*
 *
 */

import chisel3._
import chisel3.util._


class FetchUnit extends Module {
    val io = IO(new Bundle {
        val pcIn = Input(UInt(32.W))
        val pcOut = Output(UInt(32.W))
        val IMem = Flipped(new IMemIO())
        val registers = Flipped(new RegisterPortIO)
        val queue = new DecoupledIO(UInt(128.W))
        val pipelineFlushed = Input(Bool())
    })
    val pc = io.pcIn
    val pc1 = Wire(UInt(32.W))
    val pc2 = Wire(UInt(32.W))
    val pcOut = Wire(UInt(32.W))
    val queueReady = io.queue.ready
    val flushed = io.pipelineFlushed

    val nop = 0.U(32.W)

    io.registers.write.rd := 0.U
    io.registers.write.value := 0.U
    io.registers.read.r1.rd := 0.U
    io.registers.read.r2.rd := 0.U

    val r1Value = io.registers.read.r1.value
    val r2Value = io.registers.read.r2.value
    val r1Signed = r1Value.asSInt()
    val r2Signed = r2Value.asSInt()

    val instructionIn1 = io.IMem.port1.value
    val instructionIn2 = io.IMem.port2.value

    val instruction1 = Wire(UInt(32.W))
    val instruction2 = Wire(UInt(32.W))
    instruction1 := instructionIn1
    instruction2 := instructionIn2

    val addImmOpCode = "b0010011".U
    val addImmRs1Function = 0.U(8.W)
    val isBranch1 = instructionIn1(6,4) === "b110".U
    val isBranch2 = instructionIn2(6,4) === "b110".U
    val isBranch = isBranch1 | isBranch2

    val branchInstruction = Wire(UInt(32.W))
    val branchAddInstruction = Wire(UInt(32.W))
    val branchNeighbourInstruction = Wire(UInt(32.W))
    val branchTarget = Wire(UInt(32.W))  // TODO width
    val branchTaken = Wire(Bool())
    val branchRd = Wire(UInt(5.W))
    val pcBranch = Wire(UInt(32.W))

    branchInstruction := nop
    branchAddInstruction := nop
    branchNeighbourInstruction := nop
    branchTarget := 0.U
    branchTaken := false.B
    branchRd := 0.U
    pcBranch := 0.U

    val stallingPipeline = RegInit(false.B)

    pc1 := pc
    pc2 := pc + 1.U
    pcOut := pc

    when(isBranch1) {
        branchInstruction := instructionIn1
        branchNeighbourInstruction := instructionIn2
        pcBranch := pc1
    } .elsewhen(isBranch2) {
        branchInstruction := instructionIn2
        branchNeighbourInstruction := instructionIn1
        pcBranch := pc2
    }

    // TODO validate
    when(queueReady) {
        when(isBranch) {
            when(!stallingPipeline) {  // new branch -> stall dispatching
                instruction1 := nop
                instruction2 := nop
                stallingPipeline := true.B
            } .otherwise {             // dispatching already stalled
                when(!flushed) {       // pipeline not yet empty -> wait
                    instruction1 := nop
                    instruction2 := nop
                    stallingPipeline := true.B
                } .otherwise {         // Pipeline flushed -> everything ready
                    stallingPipeline := false.B
                    when(branchInstruction(3) === true.B) {  // JAL
                        branchTaken := true.B
                        branchTarget := (pcBranch.asSInt() + (Cat(branchInstruction(31), branchInstruction(19,12), branchInstruction(20), branchInstruction(30,21), false.B)  >> 2).asSInt()).asUInt()
                        branchRd := branchInstruction(11,7)
                    } .elsewhen(Cat(branchInstruction(14,12), branchInstruction(2)) === "b0001".U) { // JALR
                        branchTaken := true.B
                        io.registers.read.r1.rd := branchInstruction(19,15)
                        branchTarget := ((r1Value.asSInt() + branchInstruction(31,20).asSInt()).asUInt() >> 2)
                        branchRd := branchInstruction(11,7)
                    } .otherwise { // Branch
                        branchTarget := (pc.asSInt() + (Cat(branchInstruction(31), branchInstruction(7), branchInstruction(30, 25), branchInstruction(11, 8), false.B) >> 2).asSInt()).asUInt()
                        io.registers.read.r1.rd := branchInstruction(19, 15)
                        io.registers.read.r2.rd := branchInstruction(24, 20)
                        switch(branchInstruction(14, 12)) {
                            is("b000".U) { // BEQ
                                branchTaken := r1Value === r2Value
                            }
                            is("b001".U) { // BNE
                                branchTaken := r1Value =/= r2Value
                            }
                            is("b100".U) { // BLT
                                branchTaken := r1Signed < r2Signed
                            }
                            is("b101".U) { // BGE
                                branchTaken := r1Signed >= r2Signed
                            }
                            is("b110".U) { // BLTU
                                branchTaken := r1Value < r2Value
                            }
                            is("b111".U) { // BGEU
                                branchTaken := r1Value >= r2Value
                            }
                        }
                    }
                    when(branchTaken) {
                        pcOut := branchTarget
                        val pcNext = (pcOut + 1.U)(11,0)
                        // TODO fine for B-type?
                        branchAddInstruction := Cat(pcNext, "b00000".U, "b000".U, branchRd, "b0010011".U)
                        when(isBranch1) {
                            instruction1 := branchAddInstruction
                            instruction2 := nop
                        } .otherwise {
                            instruction1 := branchNeighbourInstruction
                            instruction2 := branchAddInstruction
                        }
                    } .otherwise {  // only applicable for B not for J type, thus branchAddInstruction not used
                        when(pc >= 125.U) { // instruction memory size
                            pcOut := pc
                        }.otherwise {
                            pcOut := pc + 2.U
                        }
                        when(isBranch1) {
                            instruction1 := nop
                            instruction2 := branchNeighbourInstruction
                        } .otherwise {
                            instruction1 := branchNeighbourInstruction
                            instruction2 := nop
                        }
                    }

                }
            }


        }  .otherwise {
            stallingPipeline := stallingPipeline
            when(pc >= 125.U) { // instruction memory size
                pcOut := pc
            }.otherwise {
                pcOut := pc + 2.U
            }
        }
    } .otherwise {
        pcOut := pc
        instruction1 := nop
        instruction2 := nop
        stallingPipeline := stallingPipeline
    }

    io.IMem.port1.address := pc1
    io.IMem.port2.address := pc2
    io.pcOut := pcOut

    io.queue.bits := Cat(pc1, instruction1, instruction2)
    io.queue.valid := true.B
}