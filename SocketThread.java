import java.net.*;
import java.io.*;
import java.util.concurrent.*;

public class SocketThread implements Runnable {
  int id, otherId, port;
  PrintWriter[] outContainer;
  BlockingQueue<Integer> messages;
  String logPrefix;

  // store configuration data on construction
  public SocketThread(int id, int otherId, int port,
      PrintWriter[] outContainer, BlockingQueue<Integer> messages) {
    this.id = id;
    this.otherId = otherId;
    this.port = port;
    this.outContainer = outContainer;
    this.messages = messages;
    this.logPrefix = "(S-" + otherId + ") ";
  }

  // connect to another machine, then forward input to the message queue
  public void run() {
    // open socket
    Socket s = null;
    int retries = 3;
    int retry_interval_ms = 100;
    for (int i = 0; i < retries; i++) {

      // create or find a socket server
      try {
        InetAddress localhost = InetAddress.getByName(null);
        if (id < otherId) {
          ServerSocket serverSocket = new ServerSocket(port, 0, localhost);
          s = serverSocket.accept();
        } else {
          s = new Socket(localhost, port);
        }
        System.out.println(logPrefix + "socket connected");
        break;

      // retry a few times if necessary
      } catch (IOException e) {
        System.out.println(logPrefix + "Socket connection failure.");
        System.out.println(logPrefix + e.getMessage());
        if (i < retries - 1) {
          System.out.println(logPrefix + "Trying again...");
          try {
            Thread.sleep(retry_interval_ms);
          } catch (InterruptedException e1) {
          }
        } else {
          System.out.println(logPrefix + "Giving up.");
        }
      }
    }

    // if socket exists, finish initialization then listen for messages
    if (s != null) {
      try (
          // get I/O streams
          PrintWriter xxx = outContainer[0] =
              new PrintWriter(s.getOutputStream(), true);
          BufferedReader in = new BufferedReader(new InputStreamReader(
              s.getInputStream()));
      ) {
        // enqueue received messages
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
          messages.add(Integer.parseInt(inputLine));
        }
      } catch (IOException e) {
        System.out.println(logPrefix + "Exception in socket thread.");
        System.out.println(logPrefix + e.getMessage());
      }
    }
  }
}
