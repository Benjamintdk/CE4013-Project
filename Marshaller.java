import java.nio.ByteBuffer;
import java.util.Arrays;

public class Marshaller {

    private static String bytesToString(byte[] message) {
        return new String(message);
    }

    private static Integer bytesToInteger(byte[] message) {
        return ByteBuffer.wrap(message).getInt();
    }

    private static File bytesToFile(byte[] file) {
        int fileNameLength = bytesToInteger(Arrays.copyOfRange(file, 0, Integer.BYTES));
        String fileName = bytesToString(Arrays.copyOfRange(file, Integer.BYTES, Integer.BYTES + fileNameLength));
        int fileContentLength = bytesToInteger(
                Arrays.copyOfRange(file, Integer.BYTES + fileNameLength, (2 * Integer.BYTES) + fileNameLength));
        String fileContent = bytesToString(Arrays.copyOfRange(file, (2 * Integer.BYTES) + fileNameLength,
                (2 * Integer.BYTES) + fileNameLength + fileContentLength));
        return new File(fileName, fileContent);

    }

    private static byte[] stringToBytes(String message) {
        return message.getBytes();
    }

    private static byte[] intToBytes(Integer value) {
        return ByteBuffer.allocate(Integer.BYTES).putInt(value).array();
    }

    private static byte[] fileToBytes(File file) {
        byte[] fileName = stringToBytes(file.getFileName());
        byte[] fileNameLength = intToBytes(fileName.length);
        byte[] fileContent = stringToBytes(file.getFileContent());
        byte[] fileContentLength = intToBytes(fileContent.length);

        byte[] combined = new byte[(2 * Integer.BYTES) + fileName.length + fileContent.length];
        System.arraycopy(fileNameLength, 0, combined, 0, Integer.BYTES);
        System.arraycopy(fileName, 0, combined, Integer.BYTES, fileName.length);
        System.arraycopy(fileContentLength, 0, combined, Integer.BYTES + fileName.length, Integer.BYTES);
        System.arraycopy(fileContent, 0, combined, (2 * Integer.BYTES) + fileName.length, fileContent.length);
        return combined;
    }

    public byte[] marshall(Integer message) {
        return intToBytes(message);
    }

    public byte[] marshall(String message) {
        return stringToBytes(message);
    }

    public byte[] marshall(File file) {
        return fileToBytes(file);
    }

    public String unmarshallString(byte[] message) {
        return bytesToString(message);
    }

    public Integer unmarshallInteger(byte[] message) {
        return bytesToInteger(message);
    }

    public File unmarshallFile(byte[] file) {
        return bytesToFile(file);
    }
}
