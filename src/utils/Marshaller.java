package src.utils;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class Marshaller {

    private static String bytesToString(byte[] message) {
        return new String(message);
    }

    private static int bytesToInteger(byte[] message) {
        return ByteBuffer.wrap(message).getInt();
    }

    private static long bytesToLong(byte[] message) {
        return ByteBuffer.wrap(message).getLong();
    }

    private static InMemoryFile bytesToFile(byte[] file) {
        int fileNameLength = bytesToInteger(Arrays.copyOfRange(file, 0, Integer.BYTES));
        String fileName = bytesToString(Arrays.copyOfRange(file, Integer.BYTES, Integer.BYTES + fileNameLength));
        int fileContentLength = bytesToInteger(
                Arrays.copyOfRange(file, Integer.BYTES + fileNameLength, (2 * Integer.BYTES) + fileNameLength));
        String fileContent = bytesToString(Arrays.copyOfRange(file, (2 * Integer.BYTES) + fileNameLength,
                (2 * Integer.BYTES) + fileNameLength + fileContentLength));
        long fileTimeLastModified = bytesToLong(
                Arrays.copyOfRange(file, (2 * Integer.BYTES) + fileNameLength + fileContentLength,
                        (2 * Integer.BYTES) + fileNameLength + fileContentLength + Long.BYTES));
        return new InMemoryFile(fileName, fileContent, fileTimeLastModified);

    }

    private static byte[] stringToBytes(String message) {
        return message.getBytes();
    }

    private static byte[] intToBytes(int value) {
        return ByteBuffer.allocate(Integer.BYTES).putInt(value).array();
    }

    private static byte[] longToBytes(long value) {
        return ByteBuffer.allocate(Long.BYTES).putLong(value).array();
    }

    private static byte[] fileToBytes(InMemoryFile file) {
        byte[] fileName = stringToBytes(file.getFileName());
        byte[] fileNameLength = intToBytes(fileName.length);
        byte[] fileContent = stringToBytes(file.getFileContent());
        byte[] fileContentLength = intToBytes(fileContent.length);
        byte[] fileLastModifiedTime = longToBytes(file.getTimeLastModified());

        byte[] combined = new byte[Long.BYTES + (2 * Integer.BYTES) + fileName.length + fileContent.length];
        System.arraycopy(fileNameLength, 0, combined, 0, Integer.BYTES);
        System.arraycopy(fileName, 0, combined, Integer.BYTES, fileName.length);
        System.arraycopy(fileContentLength, 0, combined, Integer.BYTES + fileName.length, Integer.BYTES);
        System.arraycopy(fileContent, 0, combined, (2 * Integer.BYTES) + fileName.length, fileContent.length);
        System.arraycopy(fileLastModifiedTime, 0, combined, (2 * Integer.BYTES) + fileName.length + fileContent.length,
                Long.BYTES);
        return combined;
    }

    public static byte[] marshall(int message) {
        return intToBytes(message);
    }

    public static byte[] marshall(String message) {
        return stringToBytes(message);
    }

    public static byte[] marshall(long message) {
        return longToBytes(message);
    }

    public static byte[] marshall(InMemoryFile file) {
        return fileToBytes(file);
    }

    public static String unmarshallString(byte[] message) {
        return bytesToString(message);
    }

    public static int unmarshallInteger(byte[] message) {
        return bytesToInteger(message);
    }

    public static long unmarshallLong(byte[] message) {
        return bytesToLong(message);
    }

    public static InMemoryFile unmarshallFile(byte[] file) {
        return bytesToFile(file);
    }
}
