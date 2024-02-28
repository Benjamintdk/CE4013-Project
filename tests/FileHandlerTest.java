package tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.StringIndexOutOfBoundsException;
import src.utils.FileHandler;
import src.utils.File;

import org.junit.Test;

public class FileHandlerTest {

    @Test
    public void TestUpdateFileContent() {
        // arrange
        File a = new File("File1", "This is a new file");
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
        File a = new File("File1", "This is a new file");
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
