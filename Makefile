SBT = sbt

# Generate Verilog code
hdl:
	$(SBT) "run"

test-all:
	$(SBT) "test:runMain LittleRiscyTester"

test-fetch:
	$(SBT) "test:runMain FetchTester"

test-alu:
	$(SBT) "test:runMain AluTester"

test-load-store:
	$(SBT) "test:runMain LoadStoreTester"