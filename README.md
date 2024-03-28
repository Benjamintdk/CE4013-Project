# CE4013 Group Project AY 23/24

# Server Implementation Details

This document provides an overview of the server class implementation for remote file access based on a client-server architecture. The server supports various file operations executed through UDP communication. 

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

Started on command with the server's IP address, port number, as well as freshness interval t for caching purposes. 

(1) Client-side caching 

(2) Prints server's replies on screen

(3) Option for user to terminate connection after each request 

### Marshalling 

The *Marshaller* is the class responsible for performing all marshalling and unmarshalling of data. All methods from this class are static. 

To marshall any data, we simply call the *marshall method*:

```Marshaller.marshall(your_data)```

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

The *FileHandler* class is the class responsible for handling all utilities relating to file I/O operations. All methods from this class are static. 

The static methods in this class include:

1) readFromFile
2) writeToFile
3) getFileContent
4) updateFileContent

### File

The *File* class is a container class to hold all attributes of a class. The 3 attributes of a *File* object are:

1) fileName
2) fileContent
3) timeLastModified

There are 2 possible ways to initialize it:

```
/* Standard Initialization - client should always use this constructor */
public File(String fileName, String fileContent)

/* This constructor is protected and can only be used within the src.utils package. Client and server should not need to interact directly with this constructor. */
public File(String fileName, String fileContent, long timeLastModified)
```
