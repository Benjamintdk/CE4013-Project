package src.utils;

public class File {
    private String fileName;
    private String fileContent;
    private long timeLastModified;

    public File(String fileName, String fileContent) {
        this.fileName = fileName;
        this.fileContent = fileContent;
        this.timeLastModified = System.currentTimeMillis();
    }

    protected File(String fileName, String fileContent, long timeLastModified) {
        this.fileName = fileName;
        this.fileContent = fileContent;
        this.timeLastModified = timeLastModified;
    }

    public String getFileName() {
        return this.fileName;
    }

    public String getFileContent() {
        return this.fileContent;
    }

    public long getTimeLastModified() {
        return this.timeLastModified;
    }

    protected void setFileName(String fileName) {
        this.fileName = fileName;
    }

    protected void setFileContent(String fileContent) {
        this.fileContent = fileContent;
    }

    protected void setTimeLastModified(long timeLastModified) {
        this.timeLastModified = timeLastModified;
    }
}
