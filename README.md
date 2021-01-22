# a-little-riscy
Project for 191.105 Advanced Computer Architecture (2020W)


## Summary
A Little Riscy is a minimalistic superscalar RISC-V (RV32I) processor featuring parallel execution of ALU and load/store instructions implemented in [Chisel](https://www.chisel-lang.org/). Due to its four-stage pipeline it is able to efficiently handle data hazards, however its performance decreases drastically when branching is involved. Thus, it shows that superscalar processors do not only need parallel execution units, but also compiler support and branch prediction with speculative execution to achieve a high IPC outside artificial mixes of instructions.

For a detailed description of the implemented processor and some benchmarking results refer to `report.pdf`.

## How to build A Little Riscy

The make file contains the most important run configurations, they require the [sbt build tool](https://www.scala-sbt.org/). The Verilog file can be created by running

```
make all
```

and will be placed in the `generated` directory.

```
make test-all
```

runs the integration test and creates `.vcd` files.

Building the processor requires some instructions text file (like the ones in the `test_sw` directory), the path to the file is specified in `src/main/scala/InstructionMemory.scala`. The instructions are placed in the instruction memory during build time.

## How to run it on an FPGA

The `quartus` directory contains all files required for running the processor on an Altera DE2-115 evaluation board. This requires the Altera Quartus II software (tested with version 13.0).