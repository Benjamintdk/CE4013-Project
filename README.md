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

(4) Some idempotent method (delete file)

(5) Some non-idempotent method (delete or append)

### Client class

Started on command with the server's IP address, port number, as well as freshness interval t for caching purposes. 

(1) Client-side caching 
(2) Prints server's replies on screen
(3) Option for user to terminate connection after each request 