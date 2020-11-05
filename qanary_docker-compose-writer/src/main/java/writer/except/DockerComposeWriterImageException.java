package writer.except;

public class DockerComposeWriterImageException extends DockerComposeWriterException {
    public DockerComposeWriterImageException() {
        super("The component image name cold not be read from pom.xml!");
    }
}
