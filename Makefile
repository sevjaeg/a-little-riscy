SBT = sbt

# Generate Verilog code
hdl:
	$(SBT) "run"

alu-test:
	$(SBT) "test:run AluTest"