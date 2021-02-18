package eu.wdaqua.qanary.languagedetection;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import com.cybozu.labs.langdetect.LangDetectException;

import eu.wdaqua.qanary.component.QanaryComponent;

@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan("eu.wdaqua.qanary.languagedetection")
public class Application {
	/**
	 * this method is needed to make the QanaryComponent in this project known to
	 * the QanaryServiceController in the qanary_component-template
	 * 
	 * @return
	 * @throws LangDetectException
	 * @throws IOException
	 */
	@Bean
	public QanaryComponent qanaryComponent(@Value("${spring.application.name}") final String applicationName)
			throws IOException, LangDetectException {
		return new LanguageDetection(applicationName);
	}
	
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
