package src.client;

import src.utils.Marshaller;
import src.utils.InMemoryFile;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Scanner;

import java.util.concurrent.ConcurrentHashMap;

class CacheEntry {
    String content;
    long lastFetchedTime;

    CacheEntry(String content) {
        this.content = content;
        this.lastFetchedTime = System.currentTimeMillis();
    }

    // Method to update the content and reset the fetched and validated times
    public void updateContent(String newContent) {
        this.content = newContent;
        long currentTime = System.currentTimeMillis();
        this.lastFetchedTime = currentTime;
    }
}


public class Client {
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort;

    // 1 : at-most-once
    // 0 : at-least-once
    private int invocationSemantic;

    private long freshnessInterval;

    private Scanner scanner;

    private ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, Boolean> requestHistory = new ConcurrentHashMap<>();

    public Client(String address, int port, int invocationSemantic, long freshnessInterval) throws Exception {
        this.socket = new DatagramSocket();
        this.serverAddress = InetAddress.getByName(address);
        this.serverPort = port;
        
        // 1 : at-most-once
        // 0 : at-least-once
        this.invocationSemantic = invocationSemantic;

        this.freshnessInterval = freshnessInterval;
        
        this.scanner = new Scanner(System.in);
    }


    // implementing marshalling & sending requests
    private byte[] prepareRequest(int operationCode, String filename, int offset, String content, int requestId) throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + 1 + Integer.BYTES + filename.length() + Integer.BYTES + (content != null ? content.length() : 0) + Integer.BYTES);
        buffer.putInt(requestId); // Unique request ID for at-most-once invocation semantics
        buffer.put((byte)operationCode); // Operation code
        buffer.putInt(filename.length()); // Filename length
        buffer.put(filename.getBytes()); // Filename
        buffer.putInt(offset); // Offset
        
        if (content != null) {
            buffer.putInt(content.length()); // Content length
            buffer.put(content.getBytes()); // Content
        } else {
            buffer.putInt(0); // Content length for operations without content
        }
        
        return buffer.array();
    }

    public void sendRequest(int operationCode, String filename, int offset, String content, int requestId) throws Exception {

        // 1 : at-most-once
        // 0 : at-least-once
        if (invocationSemantic == 1 && requestHistory.containsKey(requestId)) {
            System.out.println("Request ID " + requestId + " has already been processed.");
            return;
        }

        byte[] requestBytes = prepareRequest(operationCode, filename, offset, content, requestId);
        DatagramPacket requestPacket = new DatagramPacket(requestBytes, requestBytes.length, serverAddress, serverPort);
        
        try {
            socket.send(requestPacket);
            if (invocationSemantic == 0) {
                socket.setSoTimeout(5000); // Set a 5-second timeout for the response
            }
            requestHistory.put(requestId, true);
        } catch (SocketTimeoutException e) {
            if (invocationSemantic == 0) {
                System.out.println("Timeout reached, retrying...");
                socket.send(requestPacket); // Retry sending the request
            }
        }
    }

    // implementing unmarshalling and receiving responses
    public void receiveResponse(String filename) throws Exception {
        byte[] buffer = new byte[65535];
        DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
        socket.receive(responsePacket);
    
        String response = Marshaller.unmarshallString(Arrays.copyOfRange(responsePacket.getData(), 0, responsePacket.getLength()));
    
        if (response.startsWith("Error:")) {
            System.err.println("Server error: " + response);
        } else {
            System.out.println("Server response: " + response);
            
            // Update cache with new content and reset validation time
            cache.compute(filename, (key, entry) -> {
                if (entry == null) {
                    return new CacheEntry(response);
                } else {
                    entry.updateContent(response);
                    return entry;
                }
            });
        }
    }


    // Implement a method to check if cached content is still fresh based on a predefined freshness interval.
    private boolean isCacheFresh(String filename) {
        CacheEntry entry = cache.get(filename);
        return (System.currentTimeMillis() - entry.lastFetchedTime) < freshnessInterval;
    }    
    

    // MAIN INTERFACE
    // Move the while loop logic to the start method
    public void start() {
        int requestId = 1; // Initialize requestId here
        
        try{
            while (true) {
                System.out.println("\nSelect an operation:");
                System.out.println("1 - Read file content");
                System.out.println("2 - Insert content into a file");
                System.out.println("3 - Monitor file updates");
                System.out.println("4 - Get file info");
                System.out.println("5 - Append file content");
                System.out.println("0 - Exit");
                System.out.print("Enter choice: ");
                int choice = scanner.nextInt();
                scanner.nextLine(); // consume newline

                switch (choice) {
                    case 1:
                        performReadOperation(requestId);
                        break;
                    case 2:
                        performInsertOperation(requestId);
                        break;
                    case 3:
                        performMonitorOperation(requestId);
                        break;
                    case 4:
                        performGetFileInfoOperation(requestId);
                        break;
                    case 5:
                        performAppendContentOperation(requestId);
                        break;
                    case 0:
                        System.out.println("Exiting...");
                        return;
                    default:
                        System.out.println("Invalid choice.");
                        break;
                }

                requestId += 1;
            }
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("Socket closed.");
            }
        }
    }


    // THE FUNCTIONS
    private void performReadOperation(int requestId) {
        System.out.println("Enter filename:");
        String filename = scanner.nextLine();
        
        int offset = -1;
        while (offset < 0) {
            System.out.println("Enter offset:");
            while (!scanner.hasNextInt()) {
                System.out.println("Please enter a valid integer for offset:");
                scanner.next(); // consume the non-integer input
            }
            offset = scanner.nextInt();
        }

        int bytesToRead = -1;
        while (bytesToRead < 0) {
            System.out.println("Enter number of bytes to read:");
            while (!scanner.hasNextInt()) {
                System.out.println("Please enter a valid integer for the number of bytes to read:");
                scanner.next(); // consume the non-integer input
            }
            bytesToRead = scanner.nextInt();
        }

        String string_bytesToRead = String.valueOf(bytesToRead);

        scanner.nextLine(); // consume newline

        String cacheName = filename + "readFile" + String.valueOf(offset) + string_bytesToRead;

        // Check cache first
        if (cache.containsKey(cacheName) && isCacheFresh(cacheName)) {
            System.out.println("Cached content: " + cache.get(cacheName).content);
        } else {
            // Proceed to fetch from the server and update cache
            // Construct and send the request
            try {
                sendRequest(1, filename, offset, string_bytesToRead, requestId);
                receiveResponse(cacheName); // Handle the response appropriately
            } catch (Exception e) {
                System.err.println("Error during operation: " + e.getMessage());
            }
        }
    }
    

    private void performInsertOperation(int requestId) {
        System.out.println("Enter filename:");
        String filename = scanner.nextLine();

        // System.out.println("Enter offset:");
        // int offset = scanner.nextInt();
        int offset = -1;
        while (offset < 0) {
            System.out.println("Enter offset:");
            while (!scanner.hasNextInt()) {
                System.out.println("Please enter a valid integer for offset:");
                scanner.next(); // consume the non-integer input
            }
            offset = scanner.nextInt();
        }
        
        scanner.nextLine(); // consume newline
        System.out.println("Enter content to insert:");
        
        String content = scanner.nextLine();
    
        try {
            sendRequest(2, filename, offset, content, requestId);
            receiveResponse(filename);
        } catch (Exception e) {
            System.err.println("Error during insert operation: " + e.getMessage());
        }
    }


    private void performMonitorOperation(int requestId) {
        System.out.println("Enter filename to monitor:");
        String filename = scanner.nextLine();

        // System.out.println("Enter monitor interval (in seconds):");
        // int monitorInterval = scanner.nextInt();
        int monitorInterval = -1;
        while (monitorInterval < 0) {
            System.out.println("Enter monitor interval (in seconds):");
            while (!scanner.hasNextInt()) {
                System.out.println("Please enter a valid integer for the monitor interval (in seconds):");
                scanner.next(); // consume the non-integer input
            }
            monitorInterval = scanner.nextInt();
        }
        
        scanner.nextLine(); // consume newline

        final long endTime = System.currentTimeMillis() + (monitorInterval * 1000);
    
        try {
            // Preparing and sending the monitor request
            sendRequest(3, filename, monitorInterval, "", requestId); // Empty string for content as it's not needed
    
            // Starting a new thread to listen for updates
            new Thread(() -> {
                while (System.currentTimeMillis() < endTime) {
                    try {
                        receiveResponse(filename); // This method handles any incoming updates
                    } catch (Exception e) {
                        System.err.println("Error while monitoring updates: " + e.getMessage());
                        break; // Exit the loop in case of an error
                    }
                }
                System.out.println("Monitoring period has ended.");
            }).start();
        } catch (Exception e) {
            System.err.println("Error during monitor operation setup: " + e.getMessage());
        }
    }
    


    private void performGetFileInfoOperation(int requestId) {
        System.out.println("Enter filename to get info:");
        String filename = scanner.nextLine();

        String cacheName = filename + "fileInfo";

        // Check cache first
        if (cache.containsKey(filename) && isCacheFresh(filename)) {
            System.out.println("Cached content: " + cache.get(filename).content);
        } else {
            try {
                sendRequest(4, filename, 0, null, requestId); // Offset and content are not needed here.
                receiveResponse(cacheName);
            } catch (Exception e) {
                System.err.println("Error getting file info: " + e.getMessage());
            }
        }
    }
    
    
    private void performAppendContentOperation(int requestId) {
        System.out.println("Enter filename to append content:");
        String filename = scanner.nextLine();
        System.out.println("Enter content to append:");
        String content = scanner.nextLine();
    
        try {
            sendRequest(5, filename, 0, content, requestId); // Offset is not needed; assuming append happens at the end.
            receiveResponse(filename);
        } catch (Exception e) {
            System.err.println("Error during append operation: " + e.getMessage());
        }
    }
    
 
    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.out.println("Usage: java Client <server IP> <server port> <invocationSemantic> <freshnessInterval>");
            System.out.println("<invocationSemantic>: 0 - 'at-least-once'; 1 - 'at-most-once'");
            System.out.println("<freshnessInterval>: in seconds");
            return;
        }
    
        int invocationSemantic = Integer.parseInt(args[2]);
        if (invocationSemantic!= 0 && invocationSemantic != 1) {
            System.out.println("Invocation semantic must be 0 for 'at-least-once' or 1 for 'at-most-once'.");
            return;
        }

        long freshnessInterval = Long.parseLong(args[3] ) * 1000 ;   
    
        Client client = new Client(args[0], Integer.parseInt(args[1]), invocationSemantic, freshnessInterval);
        client.start();
    }
}
