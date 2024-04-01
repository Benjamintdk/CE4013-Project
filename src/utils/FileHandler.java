package src.utils;

import java.lang.StringIndexOutOfBoundsException;
import java.util.Arrays;
import java.io.*;
import java.lang.Math;

public class FileHandler {

    public static void writeToFile(String fileName, byte[] content) {
        FileOutputStream fos = null;
        try {
            File file = new File(fileName);
            fos = new FileOutputStream(file);
            for (byte b : content) {
                fos.write(b);
            }
            long timeLastModified = System.currentTimeMillis();
            fos.write(Marshaller.marshall(timeLastModified));
            fos.close();
        } catch (IOException e) {
            System.out.println("Unable to read file: " + e.getMessage());
        }
    }

    public static InMemoryFile readFromFile(String fileName) {
        FileInputStream fis = null;
        byte[] buffer = null;
        InMemoryFile readFile = null;
        try {
            File file = new File(fileName);
            fis = new FileInputStream(file);
            int fileLength = (int) file.length();
            int fileContentLength = fileLength - Long.BYTES;
            buffer = new byte[fileLength];
            int bytesRead = 0;
            int offset = 0;
            while (offset < fileLength
                    && (bytesRead = fis.read(buffer, offset, fileLength - offset)) >= 0) {
                offset += bytesRead;
            }
            String fileContent = Marshaller.unmarshallString(Arrays.copyOfRange(buffer, 0, fileContentLength));
            long timeLastModified = Marshaller
                    .unmarshallLong(Arrays.copyOfRange(buffer, fileContentLength, fileLength));
            fis.close();
            readFile = new InMemoryFile(fileName, fileContent, timeLastModified);
        } catch (IOException e) {
            System.out.println("Unable to read file: " + e.getMessage());
        }
        return readFile;
    }

    public static String getFileContent(InMemoryFile file, int offset, int numBytesToRead) {
        if (offset > file.getFileContent().length()) {
            throw new StringIndexOutOfBoundsException("Offset provided exceeds the current file length");
        }
        return file.getFileContent().substring(offset,
                Math.min(offset + numBytesToRead, file.getFileContent().length()));
    }

    public static void updateFileContent(InMemoryFile file, int offset, String newContent) {
        if (offset > file.getFileContent().length()) {
            throw new StringIndexOutOfBoundsException("Offset provided exceeds the current file length");
        }
        int curLength = file.getFileContent().length();
        String prefix = file.getFileContent().substring(0, offset);
        String suffix = file.getFileContent().substring(offset, curLength);
        file.setFileContent(prefix + newContent + suffix);
        file.setTimeLastModified(System.currentTimeMillis());
    }
}
