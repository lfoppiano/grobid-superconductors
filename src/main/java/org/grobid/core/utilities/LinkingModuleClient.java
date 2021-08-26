package org.grobid.core.utilities;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.grobid.core.data.TextPassage;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Singleton
public class LinkingModuleClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(LinkingModuleClient.class);

    private final String serverUrl;
    private GrobidSuperconductorsConfiguration configuration;
    private CloseableHttpClient httpClient;

    public LinkingModuleClient(String serverUrl) {
        this.serverUrl = serverUrl;
        this.httpClient = HttpClientBuilder.create().build();
    }

    @Inject
    public LinkingModuleClient(GrobidSuperconductorsConfiguration configuration) {
        this.configuration = configuration;
        this.serverUrl = configuration.getLinkingModuleUrl();
        this.httpClient = HttpClientBuilder.create().build();
    }

    public List<TextPassage> markCriticalTemperature(List<TextPassage> textPassage) {

        List<TextPassage> outPassage = textPassage;
        try {
            final HttpPost request = new HttpPost(serverUrl + "/classify/tc");
            request.setHeader("Accept", APPLICATION_JSON);

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setCharset(StandardCharsets.UTF_8);
            builder.addTextBody("input", toJson(textPassage), ContentType.APPLICATION_JSON);

            HttpEntity multipart = builder.build();
            request.setEntity(multipart);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
                    LOGGER.error("Not OK answer. Status code: " + response.getStatusLine().getStatusCode());
                } else {
                    outPassage = fromJson(response.getEntity().getContent());
                }
            }

        } catch (UnknownHostException e) {
            LOGGER.warn("The service is unreachable. Ignoring it. ");
        } catch (IOException e) {
            LOGGER.error("Something generally bad happened. ", e);
        }

        return outPassage;
    }

    public List<TextPassage> extractLinks(List<TextPassage> textPassage, List<String> linkTypes, boolean skipClassification) {
        try {
            final HttpPost request = new HttpPost(serverUrl + "/process/link/single");
            request.setHeader("Accept", APPLICATION_JSON);

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setCharset(StandardCharsets.UTF_8);
            builder.addTextBody("input", toJson(textPassage), ContentType.APPLICATION_JSON);
            builder.addTextBody("types", toJson_listOfString(linkTypes), ContentType.APPLICATION_JSON);
            builder.addTextBody("skip_classification", String.valueOf(skipClassification));

            HttpEntity multipart = builder.build();
            request.setEntity(multipart);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
                    LOGGER.error("Not OK answer. Status code: " + response.getStatusLine().getStatusCode());
                } else {
                    return fromJson(response.getEntity().getContent());
                }
            }

        } catch (UnknownHostException e) {
            LOGGER.warn("The service is unreachable. Ignoring it. ");
        } catch (IOException e) {
            LOGGER.error("Something generally bad happened. ", e);
        }

        return textPassage;
    }


    public String toJson(List<TextPassage> passage) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
            mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
            return mapper.writeValueAsString(passage);
        } catch (JsonGenerationException | JsonMappingException e) {
            LOGGER.error("The input line cannot be processed\n " + passage + "\n ", e);
        } catch (IOException e) {
            LOGGER.error("Some serious error when deserialize the JSON object: \n" + passage, e);
        }
        return null;
    }

    public String toJson_listOfString(List<String> linkTypes) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
            mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
            return mapper.writeValueAsString(linkTypes);
        } catch (JsonGenerationException | JsonMappingException e) {
            LOGGER.error("The input line cannot be processed\n " + linkTypes + "\n ", e);
        } catch (IOException e) {
            LOGGER.error("Some serious error when deserialize the JSON object: \n" + linkTypes, e);
        }
        return null;
    }

    public List<TextPassage> fromJson(InputStream inputLine) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
            mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
            return mapper.readValue(inputLine, new TypeReference<TextPassage>() {
            });
        } catch (JsonGenerationException | JsonMappingException e) {
            LOGGER.error("The input line cannot be processed\n " + inputLine + "\n ", e);
        } catch (IOException e) {
            LOGGER.error("Some serious error when deserialize the JSON object: \n" + inputLine, e);
        }
        return null;
    }


}
