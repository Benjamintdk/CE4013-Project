# CE4013 Group Project AY 23/24

### Server class
(1) Reads some number of bytes from given file at a specified offset. Returns a byte array
```
private byte[] readFile(String filename,
                       Integer offset,
                       Integer bytesToRead) {
                        
    // must handle error message from non-existent file with given filename
}
```

(2) Inserts some number of bytes into a given file at a specified offset. 
```
private int insertContent(String filename,
                          Integer offset,
                          Byte[] bytesToInsert) {
                        
    // must handle error message from non-existent file with given filename
    // must handle error message if offset > file length
}
```

(3) Monitors updates to a given file at a within a given time interval, sending updated file content to the client everytime another client updates it
```
private void monitorUpdates(String filename,
                            Integer monitorInterval) {
    // must handle error message from non-existent file with given filename
}
```

(4) Some idempotent method (create new file)

(5) Some non-idempotent method (append to end of file)

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
