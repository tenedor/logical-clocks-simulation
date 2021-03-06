import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;


// Two Socket Machine
//
// A process that simulates a machine and opens sockets to two other machines.
//
// Exec:
// java TwoSocketMachine <id> <other id 0> <port 0> <other id 1> <port 1>
//     <log file name>
//
// <id>: the unique id of this machine
// <other id N>: the id of the Nth other machine that this machine will talk to
// <port N>: the port to use for talking to the Nth other machine
// <log file name>: the name of the log file to use
//
// This process opens sockets to two other machines. For each socket,
// construction and subsequent message receiving is handled by forking off a
// SocketThread. Each SocketThread adds received messages to the shared
// `messages` buffer. Meanwhile, this parent thread simulates a clocked machine:
// on each tick, if a message is available it is consumed; else by random choice
// the machine either sends a message to one or both other machines or it
// registers an internal event. A logical clock is updated with each event. On
// every clock tick, the event is written to a log file. The machine's clock
// speed is selected at random on initialization. Safe resource cleanup is
// handled by a MachineShutdownThread registered as a shutdown hook.
public class TwoSocketMachine {
  static DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

  // main
  public static void main(String[] args) {
    if (args.length != 6) {
      System.err.println("Usage: java TwoSocketMachine <id> <other id 0> <port 0> <other id 1> <port 1> <log file name>");
      System.exit(1);
    }


    // Initialization
    // --------------

    // configs for this machine
    int id = Integer.parseInt(args[0]);
    int id0 = Integer.parseInt(args[1]);
    int port0 = Integer.parseInt(args[2]);
    int id1 = Integer.parseInt(args[3]);
    int port1 = Integer.parseInt(args[4]);
    String filename = args[5];

    // objects that should be closed on shutdown
    //
    // Note: this is a hacky way to make references to not-yet-existing objects.
    //   I assume java offers some better way to do this but I don't know it.
    PrintWriter[] logFileContainer = {null};
    Thread[] socketThreadsContainer = {null, null};

    // add shutdown hook for safe cleanup when process is killed
    Thread shutdownThread = new Thread(new MachineShutdownThread(
        logFileContainer, socketThreadsContainer));
    Runtime.getRuntime().addShutdownHook(shutdownThread);

    // open log file
    PrintWriter logFile;
    try {
      logFile = new PrintWriter(filename, "UTF-8");
      logFileContainer[0] = logFile;

    } catch (IOException e) {
      System.err.println("cannot open log file " + filename +
          ", shutting down.");
      return;
    }

    // print log file key
    logFile.println("R [n]: received message, n remaining messages in queue");
    logFile.println("S[id]: sent a message to machine with specified id");
    logFile.println("SB: sent a message to both other machines");
    logFile.println("IE: internal event");
    logFile.println("");

    // output writers to the other machines
    //
    // Note: this is a hacky way to make references to not-yet-existing objects.
    //   I assume java offers some better way to do this but I don't know it.
    PrintWriter[] outContainer0 = {null};
    PrintWriter[] outContainer1 = {null};

    // message queue for this machine
    BlockingQueue<Integer> messages = new LinkedBlockingQueue<Integer>();

    // determine clock speed
    int ticksPerSecond = ThreadLocalRandom.current().nextInt(1, 6 + 1);
    int tickDelay = 1000 / ticksPerSecond;
    System.out.println("clock speed: " + ticksPerSecond + " ticks per second");
    logFile.println("clock speed (ticks per second): " + ticksPerSecond);
    logFile.println("");

    // create sockets
    Thread socketThread0 = new Thread(new SocketThread(id, id0, port0,
          outContainer0, messages));
    socketThreadsContainer[0] = socketThread0;
    socketThread0.start();
    Thread socketThread1 = new Thread(new SocketThread(id, id1, port1,
          outContainer1, messages));
    socketThreadsContainer[1] = socketThread1;
    socketThread1.start();

    try {
      // wait for the subthreads to create the output writers
      while (outContainer0[0] == null || outContainer1[0] == null) {
        Thread.sleep(1);
      }

      // extract the output writers
      PrintWriter out0 = outContainer0[0];
      PrintWriter out1 = outContainer1[0];


      // Machine loop
      // ------------

      int logicalTime = 0;
      while (true) {
        // handle a message if one exists
        if (!messages.isEmpty()) {
          int receivedTime = messages.poll();
          if (receivedTime > logicalTime) {
            logicalTime = receivedTime;
          }
          logFile.println(messageLog(logicalTime, "R " + messages.size()));

        // else execute a task
        } else {
          // get random int in [1, 10]
          int rand = ThreadLocalRandom.current().nextInt(1, 10 + 1);

          // message send for 1-3
          if (rand == 1) {
            out0.println(logicalTime);
            logFile.println(messageLog(logicalTime, "S" + id0));
          } else if (rand == 2) {
            out1.println(logicalTime);
            logFile.println(messageLog(logicalTime, "S" + id1));
          } else if (rand == 3) {
            out1.println(logicalTime);
            logFile.println(messageLog(logicalTime, "SB"));

          // internal event for 4+
          } else {
            logFile.println(messageLog(logicalTime, "IE"));
          }
        }

        // increment logical clock, tick physical clock
        logicalTime++;
        Thread.sleep(tickDelay);
      }

    } catch (InterruptedException e) {
    }
  }

  // produce a log message in a consistently-formatted way
  private static String messageLog(int logicalTime, String message) {
    String systemTime = LocalTime.now().format(timeFormat);
    return systemTime + " " + logicalTime + " " + message;
  }
}
