package tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.StringIndexOutOfBoundsException;
import src.utils.FileHandler;
import src.utils.InMemoryFile;
import src.utils.Marshaller;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

public class FileHandlerTest {

    @Test
    public void TestWriteFile() {
        // arrange
        String content = "testing file";
        String fileName = "test_file";
        // act
        FileHandler.writeToFile(fileName, Marshaller.marshall(content));
        // assert
        Path path = Paths.get(fileName);
        assertTrue(Files.exists(path));
        try {
            byte[] data = Files.readAllBytes(path);
            String readContent = Marshaller.unmarshallString(Arrays.copyOfRange(data, 0, data.length - Long.BYTES));
            assertEquals(readContent, content);
            Files.delete(path);
        } catch (IOException e) {
            System.out.println("Unable to delete file.");
        }

    }

    @Test
    public void TestReadFile() {
        // arrange
        String content = "testing file";
        String fileName = "test_file";
        FileHandler.writeToFile(fileName, Marshaller.marshall(content));
        Path path = Paths.get(fileName);
        // act
        InMemoryFile testFile = FileHandler.readFromFile(fileName);
        // assert
        assertEquals(testFile.getFileName(), fileName);
        assertEquals(testFile.getFileContent(), content);
        try {
            Files.delete(path);
        } catch (IOException e) {
            System.out.println("Unable to delete file.");
        }
    }

    @Test
    public void TestUpdateFileContent() {
        // arrange
        InMemoryFile a = new InMemoryFile("File1", "This is a new file");
        /* Successful attempt at update */
        long curTimeModified = a.getTimeLastModified();
        // act
        FileHandler.updateFileContent(a, 14, "updated ");
        // assert
        assertEquals(a.getFileContent(), "This is a new updated file");
        assertTrue(a.getTimeLastModified() > curTimeModified);

        /* Failed attempt at read due to offset being too large */
        // act & assert
        Exception e = assertThrows(StringIndexOutOfBoundsException.class,
                () -> FileHandler.updateFileContent(a, 100, ""));
        String expectedMessage = "Offset provided exceeds the current file length";
        String actualMessage = e.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void TestGetFileContent() {
        // arrange
        InMemoryFile a = new InMemoryFile("File1", "This is a new file");
        /* Successful attempt at read */
        // act
        String message = FileHandler.getFileContent(a, 5, 13);
        // assert
        assertEquals(message, "is a new file");

        /* Failed attempt at read due to offset being too large */
        // act & assert
        Exception e = assertThrows(StringIndexOutOfBoundsException.class, () -> FileHandler.getFileContent(a, 100, 10));
        String expectedMessage = "Offset provided exceeds the current file length";
        String actualMessage = e.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }
}
