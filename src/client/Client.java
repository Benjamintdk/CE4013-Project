package src.client;

import src.utils.Marshaller;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.*;

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

    private String uniqueID = UUID.randomUUID().toString();

    private long freshnessInterval;

    private Scanner scanner;

    private ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public Client(String address, int port, long freshnessInterval) throws Exception {
        this.socket = new DatagramSocket();
        this.serverAddress = InetAddress.getByName(address);
        this.serverPort = port;

        this.freshnessInterval = freshnessInterval;

        this.scanner = new Scanner(System.in);
    }

    // implementing marshalling & sending requests
    private byte[] prepareRequest(int operationCode, String filename, int offset, String content, String requestId)
            throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + requestId.length() + 1 + Integer.BYTES
                + filename.length() + Integer.BYTES + (content != null ? content.length() : 0) + Integer.BYTES);
        buffer.putInt(requestId.length()); // requestID length
        buffer.put(requestId.getBytes()); // Unique request ID for at-most-once invocation semantics
        buffer.put((byte) operationCode); // Operation code
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

    public void sendRequest(int operationCode, String filename, int offset, String content, String requestId)
            throws Exception {
        byte[] requestBytes = prepareRequest(operationCode, filename, offset, content, requestId);
        DatagramPacket requestPacket = new DatagramPacket(requestBytes, requestBytes.length, serverAddress, serverPort);
    
        // Simulate packet loss
        double lossProbability = 0.1; // 10% probability for loss
        double probability = new Random().nextDouble();

        if (probability < lossProbability) {
            System.out.println("Client Request Dropped: Simulated Packet Loss");
            return; // Early return simulates packet loss; request is not sent
        }
    
        socket.send(requestPacket);
        System.out.println("Request sent.");
    }

    // implementing unmarshalling and receiving responses
    public boolean receiveResponse(String filename, int operationCode, int offset, String content) throws Exception {
        final boolean[] responseReceived = { false };
        final byte[] buffer = new byte[65535];
        final DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);

        socket.setSoTimeout(5000); // Set a 5-second timeout for the response

        try {
            // System.out.println("before");
            socket.receive(responsePacket); // This call is blocking
            responseReceived[0] = true;

            // System.out.println("i hv reached!");

            String response = Marshaller
                    .unmarshallString(Arrays.copyOfRange(responsePacket.getData(), 0, responsePacket.getLength()));

            if (response.startsWith("Error:")) {
                System.err.println("Server error: " + response);
            } else {
                System.out.println("Server response: " + response);

                if (operationCode == 1) {
                    String cacheName = filename + "readFile" + String.valueOf(offset) + content;

                    // Update cache with new content and reset validation time
                    cache.compute(cacheName, (key, entry) -> {
                        if (entry == null) {
                            return new CacheEntry(response);
                        } else {
                            entry.updateContent(response);
                            return entry;
                        }
                    });
                } else if (operationCode == 4) {
                    String cacheName = filename + "fileInfo";

                    // Update cache with new content and reset validation time
                    cache.compute(cacheName, (key, entry) -> {
                        if (entry == null) {
                            return new CacheEntry(response);
                        } else {
                            entry.updateContent(response);
                            return entry;
                        }
                    });
                }
            }
            return true;

        } catch (SocketTimeoutException e) {
            // Handle the case where socket.receive times out
            System.out.println("Socket timeout reached.");
            return false;
        } catch (Exception e) {
            System.out.println("Error receiving response: " + e.getMessage());
            return false;
        } finally {
            if (!responseReceived[0])
                return false; // Ensure to cancel timeout task on early exit
        }
    }

    // implement monitor-specific response handling
    public boolean monitor_receiveResponse(String filename, int operationCode, int offset, String content, int timeout)
            throws Exception {
        final boolean[] responseReceived = { false };
        final byte[] buffer = new byte[65535];
        final DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);

        socket.setSoTimeout(timeout * 1000); // Set a 5-second timeout for the response

        try {
            socket.receive(responsePacket); // This call is blocking
            responseReceived[0] = true;
            String response = Marshaller
                    .unmarshallString(Arrays.copyOfRange(responsePacket.getData(), 0, responsePacket.getLength()));

            if (response.startsWith("Error:")) {
                System.err.println("Server error: " + response);
            } else {
                System.out.println("Server response: " + response);

                if (operationCode == 1) {
                    String cacheName = filename + "readFile" + String.valueOf(offset) + content;

                    // Update cache with new content and reset validation time
                    cache.compute(cacheName, (key, entry) -> {
                        if (entry == null) {
                            return new CacheEntry(response);
                        } else {
                            entry.updateContent(response);
                            return entry;
                        }
                    });
                } else if (operationCode == 4) {
                    String cacheName = filename + "fileInfo";

                    // Update cache with new content and reset validation time
                    cache.compute(cacheName, (key, entry) -> {
                        if (entry == null) {
                            return new CacheEntry(response);
                        } else {
                            entry.updateContent(response);
                            return entry;
                        }
                    });
                }
            }
            return true;

        } catch (SocketTimeoutException e) {
            // Handle the case where socket.receive times out
            System.out.println("Socket timeout reached.");
            return false;
        } catch (Exception e) {
            System.out.println("Error receiving response: " + e.getMessage());
            return false;
        } finally {
            if (!responseReceived[0])
                return false; // Ensure to cancel timeout task on early exit
        }
    }

    // Implement a method to check if cached content is still fresh based on a
    // predefined freshness interval.
    private boolean isCacheFresh(String filename) {
        CacheEntry entry = cache.get(filename);
        return (System.currentTimeMillis() - entry.lastFetchedTime) < freshnessInterval;
    }

    // MAIN INTERFACE
    // Move the while loop logic to the start method
    public void start() {
        int requestId = 1; // Initialize requestId here

        try {
            while (true) {
                System.out.println("\nSelect an operation:");
                System.out.println("1 - Read file content");
                System.out.println("2 - Insert content into a file");
                System.out.println("3 - Monitor file updates");
                System.out.println("4 - Get file info");
                System.out.println("5 - Append file content");
                System.out.println("0 - Exit");
                System.out.print("Enter choice: ");

                while (!scanner.hasNextInt()) { // Check if the next input is an integer
                    System.out.println("Please enter a valid number");
                    scanner.next(); // Consume the invalid input
                    System.out.print("Enter choice: ");
                }

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
        String uniqueReq = uniqueID + "_" + String.valueOf(requestId);

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
                boolean success = false;

                while (!success) {
                    sendRequest(1, filename, offset, string_bytesToRead, uniqueReq);
                    success = receiveResponse(filename, 1, offset, string_bytesToRead);
                }
            } catch (Exception e) {
                System.err.println("Error during operation: " + e.getMessage());
            }
        }
    }

    private void performInsertOperation(int requestId) {
        String uniqueReq = uniqueID + "_" + String.valueOf(requestId);

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

        scanner.nextLine(); // consume newline
        System.out.println("Enter content to insert:");

        String content = scanner.nextLine();

        try {
            boolean success = false;

            while (!success) {
                sendRequest(2, filename, offset, content, uniqueReq);
                success = receiveResponse(filename, 1, offset, content);
            }
        } catch (Exception e) {
            System.err.println("Error during insert operation: " + e.getMessage());
        }
    }

    private void performMonitorOperation(int requestId) {
        String uniqueReq = uniqueID + "_" + String.valueOf(requestId);

        System.out.println("Enter filename to monitor:");
        String filename = scanner.nextLine();
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
            // Preparing and sending the monitor request; ensuring it's sent through
            boolean success = false;
            while (!success) {
                sendRequest(3, filename, monitorInterval, "", uniqueReq); // Empty string for content as it's not needed
                success = receiveResponse(filename, 3, 0, "");
            }

            // Starting a new thread to listen for updates
            while (System.currentTimeMillis() < endTime) {
                try {
                    monitor_receiveResponse(filename, 3, 0, "", monitorInterval); // This method handles any incoming
                                                                                  // updates
                } catch (Exception e) {
                    System.err.println("Error while monitoring updates: " + e.getMessage());
                    break; // Exit the loop in case of an error
                }
            }
            System.out.println("Monitoring period has ended.");
        } catch (Exception e) {
            System.err.println("Error during monitor operation setup: " + e.getMessage());
        }
    }

    private void performGetFileInfoOperation(int requestId) {
        String uniqueReq = uniqueID + "_" + String.valueOf(requestId);

        System.out.println("Enter filename to get info:");
        String filename = scanner.nextLine();

        // Check cache first
        if (cache.containsKey(filename) && isCacheFresh(filename)) {
            System.out.println("Cached content: " + cache.get(filename).content);
        } else {
            try {
                boolean success = false;

                while (!success) {
                    sendRequest(4, filename, 0, null, uniqueReq); // Offset and content are not needed here.
                    success = receiveResponse(filename, 4, 0, null);
                }
            } catch (Exception e) {
                System.err.println("Error getting file info: " + e.getMessage());
            }
        }
    }

    private void performAppendContentOperation(int requestId) {
        String uniqueReq = uniqueID + "_" + String.valueOf(requestId);

        System.out.println("Enter filename to append content:");
        String filename = scanner.nextLine();
        System.out.println("Enter content to append:");
        String content = scanner.nextLine();

        try {
            boolean success = false;

            while (!success) {
                sendRequest(5, filename, 0, content, uniqueReq); // Offset is not needed; assuming append happens at the
                                                                 // end.
                success = receiveResponse(filename, 5, 0, content);
            }
        } catch (Exception e) {
            System.err.println("Error during append operation: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println("Usage: java Client <server IP> <server port> <freshnessInterval>");
            System.out.println("<freshnessInterval>: in seconds");
            return;
        }

        long freshnessInterval = Long.parseLong(args[2]) * 1000;

        Client client = new Client(args[0], Integer.parseInt(args[1]), freshnessInterval);
        client.start();
    }
}
