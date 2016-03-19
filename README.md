# Readme

*last updated 03/19/16 12:44pm*


## Design Summary
- LogicalClocksSimulator is the entrypoint; it constructs three TwoSocketMachines.
- TwoSocketMachine opens sockets to two other machines and sends and receives messages. It maintains a logical clock and writes events to a log file.
- SocketThread handles socket construction and buffers received messages.
- MachineShutdownThread cleans up TwoSocketMachine resources on shutdown.


## Running the Code
- Compile the code: `javac *.java`.
- Run the simulation: `java LogicalClocksSimulator <port0> <port1> <port2> <logFileNameBase>`
  - port0, port1, port2: three unused ports on your local machine
  - logFileNameBase: the name base of the log files; each file will have its machine's id appended to its name
- See system output: enter any string not beginning with 'q', e.g. ' '
- Terminate the simulation: enter 'q'
- View logs: log files are stored in the log/ directory


## Known Weaknesses
- If the LogicalClocksSimulator process is terminated artificially (i.e. any termination other than sending a 'q' signal), it does nothing to terminate subprocesses.
- Layers-of-indirection are introduced by passing around objects in arrays. See, for instance, the use of `outContainer0` in TwoSocketMachine to enable `socketThread0` to pass an output writer back to its parent thread. I assume there is a better alternative to this but I couldn't figure it out.


## Design Details

This section duplicates the description of each class that can be found in its file.

### Logical Clocks Simulator

A process that creates a simulation of three machines messaging each other and maintaining logical clocks.

Exec:
java LogicalClocksSimulator <port0> <port1> <port2> <logFileNameBase>

<portN>: the Nth distinct port to use for machine communications
<logFileNameBase>: the name base for log files; a machine id will be appended

This process spawns three TwoSocketMachine processes with parameters that enable them to coordinate via sockets. It then listens on System.in for user input. Any input beginning with "q" terminates the three-machine simulation. For convenience, any other input logs per-process writes to System.out.

### Two Socket Machine

A process that simulates a machine and opens sockets to two other machines.

Exec:
java TwoSocketMachine <id> <other id 0> <port 0> <other id 1> <port 1> <log file name>

<id>: the unique id of this machine
<other id N>: the id of the Nth other machine that this machine will talk to
<port N>: the port to use for talking to the Nth other machine
<log file name>: the name of the log file to use

This process opens sockets to two other machines. For each socket, construction and subsequent message receiving is handled by forking off a SocketThread. Each SocketThread adds received messages to the shared `messages` buffer. Meanwhile, this parent thread simulates a clocked machine: on each tick, if a message is available it is consumed; else by random choice the machine either sends a message to one or both other machines or it registers an internal event. A logical clock is updated with each event. On every clock tick, the event is written to a log file. The machine's clock speed is selected at random on initialization. Safe resource cleanup is handled by a MachineShutdownThread registered as a shutdown hook.

### Socket Thread

A thread that opens a socket and adds received messages to a buffer.

Parameters:
  @int id: the id of this machine
  @int otherId: the id of the machine on the other side of the socket
  @int port: the socket's port
  @PrintWriter[] outContainer: length-1 array for a PrintWriter; used as a layer of indirection for passing the socket output writer
  @BlockingQueue<Integer> messages: the buffer to store messages in

This thread constructs a socket to another local machine. It assumes symmetric behavior on the other machine's side. If this machine has the lower id, this thread creates the ServerSocket, otherwise it tries connecting to an existing ServerSocket. It will retry after an interval if it fails. Upon successful connection, this thread creates I/O resources and stores the output writer in the `outContainer` array so the thread's parent may use it. This thread reads the input channel and adds messages to the `messages` buffer.

### Machine Shutdown Thread

A thread that cleans up TwoSocketMachine resources upon shutdown.

Parameters:
  @PrintWriter[] logFileContainer: array that holds one print writer
  @Thread[] socketThreadsContainer: array that holds two threads

This thread shuts down the resources it has been given if those resources have been constructed.
