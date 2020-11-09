package writer.except;

public class DockerComposeWriterVersionException extends DockerComposeWriterException {
    public DockerComposeWriterVersionException() {
        super("The component version could not be read from pom.xml!");
    }
}
