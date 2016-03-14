import java.net.*;
import java.io.*;
import java.util.concurrent.*;

public class MachineShutdownThread implements Runnable {
  PrintWriter[] logFileContainer;
  Thread[] socketThreadsContainer;

  // store arrays of objects to close on shutdown
  public MachineShutdownThread(PrintWriter[] logFileContainer,
      Thread[] socketThreadsContainer) {
    this.logFileContainer = logFileContainer;
    this.socketThreadsContainer = socketThreadsContainer;
  }

  // shut down any objects that have been opened
  public void run() {
    // close log file
    if (logFileContainer[0] != null) {
      logFileContainer[0].close();
    }

    // close sockets
    //
    // `destroy` calls are deprecated because of deadlock concerns, but the
    // only resource these threads share with anyone is the process-local
    // messages queue, so this should be fine
    if (socketThreadsContainer[0] != null) {
      socketThreadsContainer[0].destroy();
    }
    if (socketThreadsContainer[1] != null) {
      socketThreadsContainer[1].destroy();
    }

    System.out.println("Machine has shut down.");
  }
}
