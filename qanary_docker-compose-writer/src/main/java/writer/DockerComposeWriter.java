package writer;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import writer.except.DockerComposeWriterException;
import writer.except.DockerComposeWriterImageException;
import writer.except.DockerComposeWriterVersionException;

public class DockerComposeWriter {

    private static final Logger logger = Logger.getLogger("DockerComposeWriter");

    private String dockerComposePath;
    private int basePort;
    private boolean includePipelineService;
    private boolean includeConfigUiService;
    private String imagePrefix;

    private String pipelineImage;
    private int pipelinePort;
    private String configUiImage;

    public DockerComposeWriter() {
        this.configureWriter();
    }

    /*
    fetches configuration from config.properties
     */
    private void configureWriter() {
        File configuration = new File("qanary_docker-compose-writer/src/main/config.properties");

        try (FileReader reader = new FileReader(configuration)) {
            Properties properties = new Properties();
            properties.load(reader);

            this.dockerComposePath = "docker-compose.yml";
            this.basePort = Integer.parseInt(properties.getProperty("basePort"));
            this.includePipelineService = Boolean.parseBoolean(properties.getProperty("includePipelineService"));
            this.pipelineImage = properties.getProperty("pipelineImage");
            this.pipelinePort = Integer.parseInt(properties.getProperty("pipelinePort"));
            this.includeConfigUiService = Boolean.parseBoolean(properties.getProperty("includeConfigUiService"));
            this.configUiImage = properties.getProperty("configUiImage");
            this.imagePrefix = properties.getProperty("imagePrefix");

            logger.log(Level.INFO, "Configuration:{0}\n",properties);

        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage());
        }
    }

    public void write() throws DockerComposeWriterException {
        this.createDockerComposeFile();
        this.writeInitialDockerComposeFile();
        this.iterateComponents();
    }

    /*
    create docker-compose file if it does not already exist
    an existing file will be replaced
     */
    private void createDockerComposeFile() throws DockerComposeWriterException {

        try {
            File file = new File(this.dockerComposePath);
            if (file.createNewFile()) {
                logger.log(Level.INFO, "created compose file: {0}", this.dockerComposePath);
            } else {
                logger.log(Level.INFO, "file already exists: {0}", this.dockerComposePath);
            }
        } catch (IOException e) {
            throw new DockerComposeWriterException("please set a valid path for qanaryComponentsPath in config.properties!");
        }
    }

    /*
    handles writing information separate to individual components:
    version, pipeline and config ui services
     */
    private void writeInitialDockerComposeFile() throws DockerComposeWriterException {

        try (FileWriter writer = new FileWriter(this.dockerComposePath)) {

            String head = "" +
                    "version: '3.5'\n" +
                    "services:\n\n";
            writer.write(head);

            // write pipeline service
            if (this.includePipelineService) {

                String pipeline = "" +
                        "  pipeline:\n" +
                        "    entrypoint: [\"java\", \"-jar\", \"/usr/share/qanary-question-answering-system/my-qanary-qa-system.jar\", \"--server.port="+this.basePort+"\"]\n" +
                        "    image: "+this.pipelineImage+"\n" +
                        "    network_mode: host\n" +
                        "    ports:\n" +
                        "      - \""+this.basePort+"\"\n\n";

                this.pipelinePort = basePort;
                this.basePort ++;
                writer.write(pipeline);
            }

            // write config ui service
            if (this.includeConfigUiService) {
                String configUi = "" +
                        "  config-ui:\n" +
                        "   image: "+this.configUiImage+"\n" +
                        "   network_mode: host\n" +
                        "   ports:\n" +
                        "     - \""+this.basePort+"\"\n\n";

                this.basePort ++;
                writer.write(configUi);
            }
        } catch (IOException e) {
            throw new DockerComposeWriterException(e.getMessage());
        }
    }

    /*
    writes information for each component to docker-compose file
     */
    private void appendToDockerComposeFile(Map<String, String> componentAttr) throws DockerComposeWriterException {

        try (FileWriter writer = new FileWriter(this.dockerComposePath, true)) {

            String version = componentAttr.get("component-version");
            String image = componentAttr.get("image");
            String newPort = componentAttr.get("newPort");

            String dockerCompose = "" +
                    "  " + image + ":\n" +
                    "    entrypoint: [\"java\", \"-jar\", \"/qanary-service.jar\", \"--server.port="+newPort+"\", \"--spring.boot.admin.url=http://0.0.0.0:"+this.pipelinePort+"\"]\n" +
                    "    image: " + this.imagePrefix + image + ":" + version + "\n" +
                    "    network_mode: host\n" +
                    "    ports: \n" +
                    "      - \"" + newPort+ "\"\n";

            writer.write(dockerCompose);

        } catch (IOException e) {
            throw new DockerComposeWriterException(e.getMessage());
        }

    }

    /*
    loops through component modules listed in pom
     */
    private void iterateComponents() throws DockerComposeWriterException {

        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;

        try {
            builder = builderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            throw new DockerComposeWriterException(e.getMessage());
        }

        try {
            // navigate pom structure to find component modules
            String pomPath = "pom.xml";
            Document document = builder.parse(new FileInputStream(pomPath));
            Element root = document.getDocumentElement();
            Element profiles = (Element)root.getElementsByTagName("profiles").item(0);
            Element modules = (Element)profiles.getElementsByTagName("modules").item(0);
            NodeList components = modules.getElementsByTagName("module");

            // iterate over components by name
            for(int i = 0; i < components.getLength(); ++i) {
                String component = components.item(i).getTextContent();
                if (!component.equals("qanary_docker-compose-writer")) { // exclude this module as it is not a component

                    // collect necessary properties
                    Map<String, String> componentAttr = new HashMap<>();

                    String componentPomFilePath = component+ "/pom.xml";
                    componentAttr.put("componentPom", componentPomFilePath);

                    // add docker.image.name and version
                    this.addPropertiesFromPom(componentAttr);

                    // add custom port
                    componentAttr.put("newPort", this.basePort+i+"");

                    // write to new service block in docker-compose file
                    this.appendToDockerComposeFile(componentAttr);
                }
            }
        } catch (IOException | SAXException e) {
            e.printStackTrace();
        }
    }

    /*
    find and add component version and docker.image.name from a component's pom
     */
    private void addPropertiesFromPom(Map<String, String> componentAttr) throws DockerComposeWriterException {

        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;

        String pom = componentAttr.get("componentPom");

        try {
            builder = builderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new DockerComposeWriterException(e.getMessage());
        }

        try {
            Document document = builder.parse(new FileInputStream(pom));
            Element root = document.getDocumentElement();

            String version;
            String dockerImageName;

            version = this.getVersion(root);
            dockerImageName = this.getDockerImageName(root);

            componentAttr.put("image", dockerImageName);
            componentAttr.put("component-version", version);

        } catch (SAXException | DockerComposeWriterImageException | IOException e) {
            throw new DockerComposeWriterException(e.getMessage());
        }
    }

    private String getVersion(Element root) throws DockerComposeWriterVersionException {

        String version;

        // get text content of pom version node
        try {
            Node versionNode = root.getElementsByTagName("version").item(0);
            version = versionNode.getTextContent();
        } catch (NullPointerException e) {
            throw new DockerComposeWriterVersionException();
        }
        // version might not be defined
        if (version.equals("")) {
            throw new DockerComposeWriterVersionException();
        } else return version;
    }

    private String getDockerImageName(Element root) throws DockerComposeWriterImageException {

        String imageName;

        // get text content of pom docker.image.name node
        try {
            Node dockerImageNameNode = ((Element)root.getElementsByTagName("properties").item(0)).getElementsByTagName("docker.image.name").item(0);
            imageName= dockerImageNameNode.getTextContent();
        } catch (NullPointerException e) {
            throw new DockerComposeWriterImageException();
        }
        // image might not be defined
        if (imageName.equals("")) {
            throw new DockerComposeWriterImageException();
        } else return imageName;
    }
}
