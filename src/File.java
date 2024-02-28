package src;

public class File {
    private String fileName;
    private String fileContent;
    private long timeLastModified;

    public File(String fileName, String fileContent) {
        this.fileName = fileName;
        this.fileContent = fileContent;
        this.timeLastModified = System.currentTimeMillis() / 1000L;
    }

    public File(String fileName, String fileContent, long timeLastModified) {
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

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    private void setFileContent(String fileContent) {
        this.fileContent = fileContent;
    }

    private void setTimeLastModified() {
        this.timeLastModified = System.currentTimeMillis() / 1000L;
    }

    public void updateFileContent(Integer offset, String newContent) {
        int curLength = this.fileContent.length();
        String prefix = this.fileContent.substring(0, offset);
        String suffix = this.fileContent.substring(offset, curLength);
        setFileContent(prefix + newContent + suffix);
        setTimeLastModified();
    }
}
