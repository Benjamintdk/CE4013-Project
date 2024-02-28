package src.utils;

import java.lang.StringIndexOutOfBoundsException;
import java.lang.Math;

public class FileHandler {

    public static void writeToFile(String fileName) {
        /*
         * TO-DO:
         * This needs implementation without using any Java I/O utilities
         */
    }

    public static File readFromFile(String fileName) {
        /*
         * TO-DO:
         * This needs implementation without using any Java I/O utilities
         * Must also implement error handling for possible non-existence of file
         */
        return new File(fileName, "");
    }

    public static String getFileContent(File file, int offset, int numBytesToRead) {
        if (offset > file.getFileContent().length()) {
            throw new StringIndexOutOfBoundsException("Offset provided exceeds the current file length");
        }
        return file.getFileContent().substring(offset,
                Math.min(offset + numBytesToRead, file.getFileContent().length()));
    }

    public static void updateFileContent(File file, int offset, String newContent) {
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
