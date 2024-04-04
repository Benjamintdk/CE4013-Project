# CE4013 Group Project AY 23/24

# Server Implementation Details

This section provides an overview of the server class implementation for remote file access based on a client-server architecture. The server supports various file operations executed through UDP communication.

## Server Class Overview

The server maintains several key data structures for operation handling and invocation semantics:

- **monitorSubscriptions**: Tracks clients monitoring file updates.
- **requestHistory**: Caches responses for idempotent operations to support "at-most-once" semantics.
- **responseCache**: Helps keep track of handled request IDs and their corresponding responses.

## Key Methods

### Starting the Server

- **listen()**: The server listens for incoming UDP packets. It distinguishes requests based on invocation semantics ("at-least-once" or "at-most-once") and handles operations accordingly.

### Operation Handling

- **handleReadOperation()**: Reads a specified number of bytes from a file at a given offset. Errors are handled for non-existent files.
- **handleInsertOperation()**: Inserts a byte array into a file at a specified offset. Errors are managed for non-existent files and offsets beyond the file length.
- **handleMonitorOperation()**: Monitors updates to a file within a given interval, sending updated content to clients after any update.
- **handleGetFileInfo()**: An idempotent operation that retrieves information about a file.
- **handleAppendContent()**: A non-idempotent operation that appends content to the end of a file.

### Response Handling

- **sendPacket()**: Sends a UDP packet to a client address and port.
- **sendErrorResponse()**: Sends an error message to the client if an operation fails.
- **cacheResponse()**: Caches the response for a given request ID to support "at-most-once" semantics.

## Invocation Semantics

The server supports two invocation semantics:

- **At-least-once**: The server processes every incoming request, including duplicates. Suitable for idempotent operations.
- **At-most-once**: The server checks for duplicate requests using a request history and serves cached responses when duplicates are detected. It prevents the server from performing operations multiple times.

## Running the Server

To start the server, use the command:

```bash
java Server <port number> <invocation semantics>
```

### Client class

This section provides an overview of the client class implementation for remote file access based on a client-server architecture. The Client class is designed to communicate with a server via UDP to perform a variety of file operations. It offers an interactive interface for users, enabling them to execute different operations on files stored on the server.

## Client Class Overview

The client maintains several key data structures for managing communication with the server, handling user operation requests, implementing client-side caching, and managing invocation semantics for reliability and fault tolerance:

- **cache**: A ConcurrentHashMap that implements client-side caching of file contents, allowing users to read "fresh" cached file content directly without contacting the server.
- **requestHistory**: Tracks processed request IDs to support "at-most-once" semantics.
- **socket**: A DatagramSocket instance for sending and receiving UDP packets.
- **serverAddress** and **serverPort**: Server's IP address and port number for establishing communication.

## Key Methods

### Starting the Client

- **start()**: This method initializes the Client, sets up the user interface for selecting file operations, and handles the execution of chosen operations.

### User Interface Options

- **1: Read File Content**: Allows users to input a filename, offset, and the number of bytes to be read within the file.
- **2: Insert Content into a File**: Allows users to input a filename, offset, and content to be inserted into the file.
- **3: Monitor File Updates**: Allows users to input a filename and monitor interval to track any changes to the file during the provided interval duration.
- **4: Get File Information**: This is the idempotent function implemented by our team. Users can input a filename and will be returned details about the given file.
- **5: Append File Content**: This is the non-idempotent function implemented by our team. Users can input a filename and the content to be appended to the end of the given file.
- **0: Exit**: This gives users the options to terminate the client when they are done querying the server. This shuts-down the client cleanly.

### Response Handling

- **sendRequest()** and **prepareRequest**: Prepares and sends a request to the server based on the selected operation, filename, offset, content, and request ID. Supports "at-most-once" semantics by checking the request history.
- **receiveResponse()**: Receives a response from the server, updates the cache if necessary, and displays the server's reply on the screen.

### Operation Handling

- **isCacheFresh()**: Checks if the cached server responses are still 'fresh' in comparison to the pre-entered freshness interval. Ran before requests are sent to the server.
- **performReadOperation()**, **performInsertOperation()**, **performMonitorOperation()**, **performGetFileInfoOperation()**, **performAppendContentOperation()**: These methods correspond to the user-selected operations. They gather required input from the user and invoke `sendRequest()` with appropriate parameters.

### Client-Side Caching

The client implements caching to enhance performance for read operations. It maintains a cache of recently fetched file contents, checks the freshness of cached data before sending a read request, and updates the cache upon receiving new data from the server.

## Running the Client

To start the Client, use the following command, providing the server IP address, port number, the invocation semantic flag, and the freshness interval for caching:

```bash
java Client <ServerIP> <ServerPort> <FreshnessInterval>
```

`FreshnessInterval` : in seconds

### Marshalling

The _Marshaller_ is the class responsible for performing all marshalling and unmarshalling of data. All methods from this class are static.

To marshall any data, we simply call the _marshall method_:

`Marshaller.marshall(your_data)`

Currently, the types of supported data types for marshalling are integers, longs, strings and [File](#file). This was implemented using method overloading.

To unmarshall any data, we need to know the datatype which we are unmarshalling to beforehand. Similarly as before, there are the 4 supported data types that can be unmarshalled. All unmarshalling methods receive a byte array (`byte[]`) as a parameter.

```
/* Unmarshall file */
File newFile = Marshaller.unmarshellFile(your_file_bytes)

/* Unmarshall string */
String message = Marshaller.unmarshellString(your_string_bytes)

/* Unmarshall integer */
int value = Marshaller.unmarshellInteger(your_integer_bytes)

/* Unmarshall long */
long value = Marshaller.unmarshellLong(your_long_bytes)
```

### FileHandler

The _FileHandler_ class is the class responsible for handling all utilities relating to file I/O operations. All methods from this class are static.

The static methods in this class include:

1. readFromFile
2. writeToFile
3. getFileContent
4. updateFileContent

### File

The _File_ class is a container class to hold all attributes of a class. The 3 attributes of a _File_ object are:

1. fileName
2. fileContent
3. timeLastModified

There are 2 possible ways to initialize it:

```
/* Standard Initialization - client should always use this constructor */
public File(String fileName, String fileContent)

/* This constructor is protected and can only be used within the src.utils package. Client and server should not need to interact directly with this constructor. */
public File(String fileName, String fileContent, long timeLastModified)
```
