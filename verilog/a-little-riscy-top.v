/* Minimal top level for the LittleRiscy.
  Wire reset to 0. */

module riscy_top(input clk, output led);

  wire h_io_led;
  wire res;

  assign led = h_io_led;
  assign res = 1'h0;
  LittleRiscy h(.clock(clk), .reset(res),
       .io_led( h_io_led ));
endmodule
