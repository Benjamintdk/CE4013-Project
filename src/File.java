package src;
public class File {
    private String fileName;
    private String fileContent;

    public File(String fileName, String fileContent) {
        this.fileName = fileName;
        this.fileContent = fileContent;
    }

    public String getFileName() {
        return this.fileName;
    }

    public String getFileContent() {
        return this.fileContent;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFileContent(String fileContent) {
        this.fileContent = fileContent;
    }
}
