package tests;

import static org.junit.Assert.assertEquals;
import src.Marshaller;
import src.File;

import org.junit.Test;

public class MarshallerTest {
    @Test
    public void TestMarshallInteger() {
        int a, aUnmarshalled;
        byte[] aMarshalled;

        /* Test positive integers */
        // arrange
        a = 100;
        // act
        aMarshalled = Marshaller.marshall(a);
        aUnmarshalled = Marshaller.unmarshallInteger(aMarshalled);
        // assert
        assertEquals(a, aUnmarshalled);

        /* Test negative integers */
        // arrange
        a = -100;
        // act
        aMarshalled = Marshaller.marshall(a);
        aUnmarshalled = Marshaller.unmarshallInteger(aMarshalled);
        // assert
        assertEquals(a, aUnmarshalled);
    }

    @Test
    public void TestMarshallString() {
        String a, aUnmarshalled;
        byte[] aMarshalled;

        /* Test empty string */
        // arrange
        a = "";
        // act
        aMarshalled = Marshaller.marshall(a);
        aUnmarshalled = Marshaller.unmarshallString(aMarshalled);
        // assert
        assertEquals(a, aUnmarshalled);

        /* Test non-empty string */
        // arrange
        a = new String("This is a test");
        // act
        aMarshalled = Marshaller.marshall(a);
        aUnmarshalled = Marshaller.unmarshallString(aMarshalled);
        // assert
        assertEquals(a, aUnmarshalled);
    }

    @Test
    public void TestMarshallFile() {
        File a, aUnmarshalled;
        byte[] aMarshalled;

        // arrange
        a = new File("File1", "This is a new file");
        // act
        aMarshalled = Marshaller.marshall(a);
        aUnmarshalled = Marshaller.unmarshallFile(aMarshalled);
        // assert
        assertEquals(a.getFileName(), aUnmarshalled.getFileName());
        assertEquals(a.getFileContent(), aUnmarshalled.getFileContent());
        assertEquals(a.getTimeLastModified(), aUnmarshalled.getTimeLastModified());
    }
}
