import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;


public class TwoSocketMachine {
  public static void main(String[] args) {
    if (args.length != 5) {
      System.err.println("Usage: java TwoSocketMachine <id> <other id 0> <port 0> <other id 1> <port 1>");
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

    // output writers to the other machines
    //
    // Note: this is a hacky way to let the subthreads construct the writers.
    //   I assume java offers some better way to do this but I don't know it.
    PrintWriter[] outContainer0 = {null};
    PrintWriter[] outContainer1 = {null};

    // message queue for this machine
    BlockingQueue<Integer> messages = new LinkedBlockingQueue<Integer>();

    // determine clock speed
    int ticksPerSecond = ThreadLocalRandom.current().nextInt(1, 6 + 1);
    int tickDelay = 1000 / ticksPerSecond;
    System.out.println("clock speed: " + ticksPerSecond +
        " ticks per second");

    // create sockets
    Thread socketThread0 = new Thread(new SocketThread(id, id0, port0,
          outContainer0, messages));
    socketThread0.start();
    Thread socketThread1 = new Thread(new SocketThread(id, id1, port1,
          outContainer1, messages));
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

      int logicalClock = 0;
      while (true) {
        // handle a message if one exists
        if (!messages.isEmpty()) {
          int receivedTime = messages.poll();
          System.out.println("" + logicalClock + ": received " + receivedTime);
          if (receivedTime > logicalClock) {
            logicalClock = receivedTime;
          }

        // else execute a task
        } else {
          // get random int in [1, 10]
          int rand = ThreadLocalRandom.current().nextInt(1, 10 + 1);

          // message send for 1-3
          if (rand == 1) {
            out0.println(logicalClock);
            System.out.println("" + logicalClock + ": sent message to " + id0);
          } else if (rand == 2) {
            out1.println(logicalClock);
            System.out.println("" + logicalClock + ": sent message to " + id1);
          } else if (rand == 3) {
            out1.println(logicalClock);
            System.out.println("" + logicalClock + ": sent message to both");

          // anonymous event for 4+
          } else {
            System.out.println("" + logicalClock + ": ticked");
          }
        }

        // increment logical clock, tick physical clock
        logicalClock++;
        Thread.sleep(tickDelay);
      }

    } catch (InterruptedException e) {
      // unsafe shutdown; fix this before using it as production code
      System.out.println("TwoSocketMachine shutting down.");

      // specifically, `destroy` calls are deprecated
      socketThread0.destroy();
      socketThread1.destroy();
    }
  }
}
