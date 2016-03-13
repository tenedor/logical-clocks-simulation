import java.io.*;

public class LogicalClocksSimulator {
  public static void main(String[] args) throws IOException {

    if (args.length != 3) {
      System.err.println("Usage: java Parent <port0> <port1> <port2>");
      System.exit(1);
    }

    // ports to use
    String port0 = args[0];
    String port1 = args[1];
    String port2 = args[2];

    // commands for creating machine processes
    String[] cmds = {
      "java TwoSocketMachine 0 1 " + port0 + " 2 " + port1,
      "java TwoSocketMachine 1 0 " + port0 + " 2 " + port2,
      "java TwoSocketMachine 2 0 " + port1 + " 1 " + port2
    };

    try {
      // create machines
      Process[] machines = {null, null, null};
      for (int i = 0; i < 3; i++) {
        machines[i] = Runtime.getRuntime().exec(cmds[i]);
      }

      try (
        // read input from each machine
        BufferedReader machineInputs0 =
          new BufferedReader(
            new InputStreamReader(machines[0].getInputStream()));
        BufferedReader machineInputs1 =
          new BufferedReader(
            new InputStreamReader(machines[1].getInputStream()));
        BufferedReader machineInputs2 =
          new BufferedReader(
            new InputStreamReader(machines[2].getInputStream()));

        // read input from std in
        BufferedReader stdIn =
          new BufferedReader(
            new InputStreamReader(System.in));
      ) {
        // inputs from machines
        BufferedReader[] machineInputs = {
          machineInputs0,
          machineInputs1,
          machineInputs2
        };

        // refresh on each user input, quitting if user types 'q'
        String userInput;
        while ((userInput = stdIn.readLine()) != null) {
          // quit on 'q'
          if (userInput.charAt(0) == 'q') {
            break;
          }

          // else print new outpu
          for (int i = 0; i < 3; i++) {
            while (machineInputs[i].ready()) {
              System.out.println("" + i + ") " + machineInputs[i].readLine());
            }
          }
        }
      }

      // teardown each process
      for (int i = 0; i < 3; i++) {
        machines[i].destroy();
      }

    } catch (IOException e) {
      System.out.println("Failed to start machines");
      System.out.println(e.getMessage());
    }
  }
}

