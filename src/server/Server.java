package src.server;

import src.utils.FileHandler;
import src.utils.InMemoryFile;
import src.utils.Marshaller;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.net.InetAddress;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

class ClientInfo {
    InetAddress address;
    int port;
    long expiryTime;

    ClientInfo(InetAddress address, int port, long expiryTime) {
        this.address = address;
        this.port = port;
        this.expiryTime = expiryTime;
    }
}

public class Server {
    private DatagramSocket socket;
    private boolean running;
    private String invocationSemantics; // "at-least-once" or "at-most-once"

    private ConcurrentHashMap<String, List<ClientInfo>> monitorSubscriptions;
    private ConcurrentHashMap<String, Long> requestHistory; // caching of the requests for "at-most-once"
    private ConcurrentHashMap<String, byte[]> responseCache; // help keep track of handled request IDs
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public Server(int port, String invocationSemantics) throws Exception {
        this.socket = new DatagramSocket(port);
        this.invocationSemantics = invocationSemantics;
        this.monitorSubscriptions = new ConcurrentHashMap<>();
        this.responseCache = new ConcurrentHashMap<>();
        this.requestHistory = new ConcurrentHashMap<>();

        scheduleCacheCleanup();
    }
    private void scheduleCacheCleanup() {
        scheduler.scheduleAtFixedRate(() -> {
            long expiryThreshold = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24);
            
            // collect expired request IDs
            List<String> expiredRequestIds = requestHistory.entrySet().stream()
                    .filter(entry -> entry.getValue() < expiryThreshold)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            // remove expired requests from requestHistory
            expiredRequestIds.forEach(requestHistory::remove);

            // remove corresponding entries from responseCache
            expiredRequestIds.forEach(responseCache::remove);
            
            System.out.println("Removed expired requestIDs");
        }, 0, 1, TimeUnit.HOURS); // start now and repeat every hour
    }

    public void listen() throws Exception {

        running = true;
        System.out.println("Server is running with " + invocationSemantics + " semantics.");

        while (running) {
            byte[] buf = new byte[65535];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);

            ByteBuffer buffer = ByteBuffer.wrap(packet.getData());
            int requestIdLength = buffer.getInt(); // Read requestID length first
            byte[] requestIdBytes = new byte[requestIdLength];
            buffer.get(requestIdBytes);
            String requestId = new String(requestIdBytes); // Convert bytes to string
            byte operationCode = buffer.get(); // extracting the operation code

            if ("at-most-once".equals(invocationSemantics) && requestHistory.containsKey(requestId)) {
                byte[] cachedResponse = responseCache.get(requestId);
                if (cachedResponse != null) {
                    System.out.println("Sending cached response to " + packet.getAddress().getHostAddress() + ":"
                            + packet.getPort());
                    sendPacket(cachedResponse, packet.getAddress(), packet.getPort());
                    continue; // we skip the operation processing for this duplicate request under
                              // "at-most-once" semantics
                }
            }
            // for the non duplicate requests under "at-most-once" or any request under
            // "at-least-once" we can proceed with operation processing.
            // then record the request ID for "at-most-once" semantics to prevent duplicate
            // processing.
            if ("at-most-once".equals(invocationSemantics)) {
                requestHistory.put(requestId, System.currentTimeMillis());
            }

            // choose specific operation based on the operation code.
            switch (operationCode) {
                case 1: // read
                    handleReadOperation(buffer, packet);
                    break;
                case 2: // insert
                    handleInsertOperation(buffer, packet);
                    break;
                case 3: // monitor
                    handleMonitorOperation(buffer, packet);
                    break;
                case 4: // get file info = idempotent
                    handleGetFileInfo(buffer, packet);
                    break;
                case 5: // append file content = non-idempotent
                    handleAppendContent(buffer, packet);
                    break;
                default:
                    sendErrorResponse(packet, "Invalid operation code.");
                    break;
            }
        }

        socket.close();
    }

    private void sendPacket(byte[] data, InetAddress address, int port) throws IOException {
        // Generate a random number between 0.0 and 1.0
        double randomValue = Math.random();
    
        if (randomValue < 0.8) {
            System.out.println("Simulated packet loss: packet not sent.");
            return; 
        }
    
        // Proceed to send the packet as normal if not dropped
        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        socket.send(packet);
    }
    

    private void handleReadOperation(ByteBuffer buffer, DatagramPacket requestPacket) {
        try {
            int filenameLength = buffer.getInt();
            byte[] filenameBytes = new byte[filenameLength];
            buffer.get(filenameBytes);
            String filename = new String(filenameBytes);
            int offset = buffer.getInt();
            int lengthofbytesToRead = buffer.getInt();
            byte[] bytesToReadBytes = new byte[lengthofbytesToRead];
            buffer.get(bytesToReadBytes);
            String bytesToReadString = new String(bytesToReadBytes);
            int bytesToRead = Integer.parseInt(bytesToReadString.trim());

            InMemoryFile file = FileHandler.readFromFile(filename);
            if (file != null) {
                String content = FileHandler.getFileContent(file, offset, bytesToRead);
                byte[] responseBytes = Marshaller.marshall(content);
                cacheResponse(requestPacket, responseBytes);
                sendPacket(responseBytes, requestPacket.getAddress(), requestPacket.getPort());
            } else {
                sendErrorResponse(requestPacket, "File does not exist.");
            }
        } catch (Exception e) {
            sendErrorResponse(requestPacket, "Error during read operation: " + e.getMessage());
        }
    }

    private void handleInsertOperation(ByteBuffer buffer, DatagramPacket requestPacket) {
        try {
            int filenameLength = buffer.getInt();
            byte[] filenameBytes = new byte[filenameLength];
            buffer.get(filenameBytes);
            String filename = new String(filenameBytes);

            int offset = buffer.getInt();
            int lengthofbytesToRead = buffer.getInt();
            byte[] contentToInsert_bytes = new byte[lengthofbytesToRead];
            buffer.get(contentToInsert_bytes);
            String contentToInsert = new String(contentToInsert_bytes);

            InMemoryFile file = FileHandler.readFromFile(filename);
            if (file != null) {
                FileHandler.updateFileContent(file, offset, contentToInsert);
                byte[] updatedFileData = Marshaller.marshall(file.getFileContent());
                FileHandler.writeToFile(filename, updatedFileData);
                notifyClientsOfUpdate(filename, file.getFileContent());

                byte[] responseBytes = "Success".getBytes();
                cacheResponse(requestPacket, responseBytes);
                sendPacket(responseBytes, requestPacket.getAddress(), requestPacket.getPort());
            } else {
                sendErrorResponse(requestPacket, "File does not exist.");
            }
        } catch (Exception e) {
            sendErrorResponse(requestPacket, "Error during insert operation: " + e.getMessage());
        }
    }

    private void handleMonitorOperation(ByteBuffer buffer, DatagramPacket packet) {
        int filenameLength = buffer.getInt();
        byte[] filenameBytes = new byte[filenameLength];
        buffer.get(filenameBytes);
        String filename = new String(filenameBytes);
        int monitorInterval = buffer.getInt();

        long expiryTime = System.currentTimeMillis() + (monitorInterval * 1000L);
        ClientInfo clientInfo = new ClientInfo(packet.getAddress(), packet.getPort(), expiryTime);
        monitorSubscriptions.computeIfAbsent(filename, k -> new ArrayList<>()).add(clientInfo);

        String message = "Monitoring registration successful";
        byte[] responseBytes = Marshaller.marshall(message);
        try {
            sendPacket(responseBytes, packet.getAddress(), packet.getPort());
            cacheResponse(packet, responseBytes);
        } catch (IOException e) {
            System.err.println("Error sending packet: " + e.getMessage());
        }

    }

    private void notifyClientsOfUpdate(String filename, String fileContent) {
        List<ClientInfo> clients = monitorSubscriptions.getOrDefault(filename, new ArrayList<>());
        for (ClientInfo client : clients) {
            if (System.currentTimeMillis() > client.expiryTime) {
                // skip for the expired subscriptions
                continue;
            }
            try {
                byte[] contentBytes = Marshaller.marshall(fileContent);
                DatagramPacket updatePacket = new DatagramPacket(contentBytes, contentBytes.length, client.address,
                        client.port);
                socket.send(updatePacket);
            } catch (IOException e) {
                System.err.println("Failed to send update: " + e.getMessage());
            }
        }
        // we remove expired subscriptions
        clients.removeIf(client -> System.currentTimeMillis() > client.expiryTime);
    }

    private void handleGetFileInfo(ByteBuffer buffer, DatagramPacket packet) {
        try {
            int filenameLength = buffer.getInt();
            byte[] filenameBytes = new byte[filenameLength];
            buffer.get(filenameBytes);
            String filename = new String(filenameBytes);

            InMemoryFile file = FileHandler.readFromFile(filename);
            if (file != null) {
                String fileInfo = "Name: " + file.getFileName() +
                        ", Size: " + file.getFileContent().length() +
                        " bytes, Last Modified: " + file.getTimeLastModified();
                byte[] responseBytes = Marshaller.marshall(fileInfo);
                sendPacket(responseBytes, packet.getAddress(), packet.getPort());
                cacheResponse(packet, responseBytes);
            } else {
                sendErrorResponse(packet, "File does not exist.");
            }
        } catch (Exception e) {
            sendErrorResponse(packet, "Error retrieving file info: " + e.getMessage());
        }
    }

    private void handleAppendContent(ByteBuffer buffer, DatagramPacket packet) {
        try {
            int filenameLength = buffer.getInt();
            byte[] filenameBytes = new byte[filenameLength];
            buffer.get(filenameBytes);
            String filename = new String(filenameBytes);

            buffer.getInt(); // just to ignore the extra default "offset" sent by client
            int lengthofbytesToRead = buffer.getInt();
            byte[] contentToAppend_bytes = new byte[lengthofbytesToRead];
            buffer.get(contentToAppend_bytes);
            String contentToAppend = new String(contentToAppend_bytes);

            InMemoryFile file = FileHandler.readFromFile(filename);
            if (file != null) {
                int offset = file.getFileContent().length();
                FileHandler.updateFileContent(file, offset, contentToAppend);
                byte[] updatedFileData = Marshaller.marshall(file.getFileContent());
                FileHandler.writeToFile(filename, updatedFileData);
                notifyClientsOfUpdate(filename, file.getFileContent());
                String message = "Content appended successfully";
                byte[] responseBytes = Marshaller.marshall(message);
                sendPacket(responseBytes, packet.getAddress(), packet.getPort());
                cacheResponse(packet, responseBytes);
            } else {
                sendErrorResponse(packet, "File does not exist.");
            }
        } catch (Exception e) {
            sendErrorResponse(packet, "Error appending content: " + e.getMessage());
        }
    }

    // cache the response for a given request
    private void cacheResponse(DatagramPacket requestPacket, byte[] responseBytes) {
        if ("at-least-once".equals(invocationSemantics)) {
            return;
        }
        ByteBuffer buffer = ByteBuffer.wrap(requestPacket.getData());

        // Extract the length of the requestId
        int requestIdLength = buffer.getInt();

        // Extract the requestId bytes based on the length and convert to String
        byte[] requestIdBytes = new byte[requestIdLength];
        buffer.get(requestIdBytes);
        String requestId = new String(requestIdBytes);

        // Use the string requestId to cache the response
        responseCache.put(requestId, responseBytes); // Use String requestId as the key
    }

    private void sendErrorResponse(DatagramPacket packet, String errorMessage) {
        try {
            byte[] errorResponse = errorMessage.getBytes();
            DatagramPacket responsePacket = new DatagramPacket(errorResponse, errorResponse.length, packet.getAddress(),
                    packet.getPort());
            socket.send(responsePacket);
            cacheResponse(packet, errorResponse);
        } catch (IOException e) {
            System.err.println("Failed to send error response: " + e.getMessage());
        }
    }

    private static void createInitialFiles(String[] fileNames) {
        for (String fileName : fileNames) {
            try {
                Path path = Paths.get(fileName);
                // Check and confirm file creation
                if (Files.exists(path)) {
                    System.out.println("Confirmation: File exists - " + path.toAbsolutePath());
                } else {
                    Files.createFile(path);
                    System.out.println("Created file: " + path.toAbsolutePath());
                }
            } catch (IOException e) {
                System.err.println("Could not create file " + fileName + ": " + e.getMessage());
            }
        }
    }

    private static void deleteInitialFiles(String[] fileNames) {
        for (String fileName : fileNames) {
            try {
                Files.deleteIfExists(Paths.get(fileName));
            } catch (NoSuchFileException e) {
                System.out.println(
                        "No such file exists");
            } catch (IOException e) {
                System.out.println("Invalid permissions.");
            }
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java Server <port number> <invocation semantics>");
            System.exit(1);
        }
        int port = Integer.parseInt(args[0]);
        String semantics = args[1]; // "at-least-once" or "at-most-once"

        // File names to create
        String[] fileNames = { "file1", "file2", "file3" };
        deleteInitialFiles(fileNames);
        createInitialFiles(fileNames);
        FileHandler.writeToFile("file1", Marshaller.marshall("Hello World"));
        FileHandler.writeToFile("file2", Marshaller.marshall("Distributed systems"));

        try {
            Server server = new Server(port, semantics);
            server.listen();
            deleteInitialFiles(fileNames);
        } catch (Exception e) {
            System.err.println("Server failed to start: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
