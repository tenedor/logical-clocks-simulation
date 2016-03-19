import java.io.*;
import java.util.concurrent.*;


// Logical Clocks Simulator
//
// A process that creates a simulation of three machines messaging each other
// and maintaining logical clocks.
//
// Exec:
// java LogicalClocksSimulator <port0> <port1> <port2> <logFileNameBase>
//
// <portN>: the Nth distinct port to use for machine communications
// <logFileNameBase>: the name base for log files; a machine id will be appended
//
// This process spawns three TwoSocketMachine processes with parameters that
// enable them to coordinate via sockets. It then listens on System.in for user
// input. Any input beginning with "q" terminates the three-machine simulation.
// For convenience, any other input logs per-process writes to System.out.
public class LogicalClocksSimulator {
  public static void main(String[] args) throws IOException {

    if (args.length != 4) {
      System.err.println(
          "Usage: java LogicalClocksSimulator <port0> <port1> <port2> <logFileNameBase>");
      System.exit(1);
    }

    // ports to use
    String port0 = args[0];
    String port1 = args[1];
    String port2 = args[2];

    // log file names
    String logFileName0 = "log/" + args[3] + "0.txt";
    String logFileName1 = "log/" + args[3] + "1.txt";
    String logFileName2 = "log/" + args[3] + "2.txt";

    // commands for creating machine processes
    String[] cmds = {
      "java TwoSocketMachine 0 1 " + port0 + " 2 " + port1 + " " + logFileName0,
      "java TwoSocketMachine 1 0 " + port0 + " 2 " + port2 + " " + logFileName1,
      "java TwoSocketMachine 2 0 " + port1 + " 1 " + port2 + " " + logFileName2
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

        System.out.println("Enter a message starting with 'q' to quit. Enter any other message to print new per-machine output.");

        // refresh on each user input, quitting if user types 'q'
        String userInput;
        while ((userInput = stdIn.readLine()) != null) {
          // quit on 'q'
          if (userInput.charAt(0) == 'q') {
            break;
          }

          // else print new output
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

