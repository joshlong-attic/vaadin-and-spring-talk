package places;


import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.http.MediaType;
import org.springframework.social.facebook.api.Facebook;
import org.springframework.social.facebook.api.impl.FacebookTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Map;

@Configuration
@ComponentScan
@EnableAutoConfiguration
public class Application {

    private Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());

    @Bean
    Facebook facebook(@Value("${facebook.appId}") String appId,
                      @Value("${facebook.appSecret}") String appSecret) {
        return new FacebookTemplate(appId + '|' + appSecret);
    }

    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    CommandLineRunner init(RestTemplate restTemplate,
                           Facebook facebook,
                           PlaceRepository placeRepository) {
        return args -> {

            String ip = restTemplate.getForObject("http://icanhazip.com", String.class)
                    .trim();

            Map<?, ?> loc = restTemplate.getForObject(
                    "http://freegeoip.net/json/{ip}",
                    Map.class, ip);

            double longitude = (Double) loc.get("longitude");
            double latitude = (Double) loc.get("latitude");

            logger.info("the IP of the current machine is: " + ip);
            logger.info("latitude & longitude: " + loc.toString());

            placeRepository.deleteAll();

            logger.info("all records near current IP:");

            Arrays.asList("Starbucks", "Philz", "Ike's Place", "Bite", "Umami").forEach(q ->
                    facebook.placesOperations()
                            .search(q, latitude, longitude, 50000 - 1).stream()
                            .map(p -> placeRepository.save(new Place(p)))
                            .forEach(System.out::println));

            logger.info("zooming in..");
            placeRepository.findByPositionNear(new Point(longitude, latitude), new Distance(1, Metrics.MILES)).forEach(System.out::println);
        };
    }


    @Bean
    EmbeddedServletContainerCustomizer servletContainerCustomizer() {
        return servletContainer -> ((TomcatEmbeddedServletContainerFactory) servletContainer)
                .addConnectorCustomizers(connector -> {
                    AbstractHttp11Protocol httpProtocol = (AbstractHttp11Protocol) connector
                            .getProtocolHandler();
                    httpProtocol.setCompression("on");
                    httpProtocol.setCompressionMinSize(256);
                    String mimeTypes = httpProtocol.getCompressableMimeTypes();
                    String mimeTypesWithJson = mimeTypes + ","
                            + MediaType.APPLICATION_JSON_VALUE
                            + ",application/javascript";
                    httpProtocol.setCompressableMimeTypes(mimeTypesWithJson);
                });
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
