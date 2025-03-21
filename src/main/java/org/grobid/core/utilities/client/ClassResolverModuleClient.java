package org.grobid.core.utilities.client;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Singleton
public class ClassResolverModuleClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassResolverModuleClient.class);

    private final String serverUrl;
    private GrobidSuperconductorsConfiguration configuration;
    private CloseableHttpClient httpClient;

    public ClassResolverModuleClient(String serverUrl) {
        this.serverUrl = serverUrl;
        this.httpClient = HttpClientBuilder.create().build();
    }

    @Inject
    public ClassResolverModuleClient(GrobidSuperconductorsConfiguration configuration) {
        this.configuration = configuration;
        this.serverUrl = configuration.getClassResolverUrl();
        this.httpClient = HttpClientBuilder.create().build();
    }

    public List<String> getClassesFromFormula(String formula) {

        List<String> outputClasses = new ArrayList<>();
        try {
            final HttpPost request = new HttpPost(serverUrl + "/classify/formula");
            request.setHeader("Accept", APPLICATION_JSON);

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setCharset(StandardCharsets.UTF_8);
            builder.addTextBody("input", formula, ContentType.APPLICATION_JSON);

            HttpEntity multipart = builder.build();
            request.setEntity(multipart);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
                    LOGGER.error("Not OK answer. Status code: " + response.getStatusLine().getStatusCode());
                } else {
                    outputClasses = fromJson(response.getEntity().getContent());
                }
            }

        } catch (UnknownHostException e) {
            LOGGER.warn("The service is unreachable. Ignoring it. ", e);
        } catch (IOException e) {
            LOGGER.error("Something generally bad happened. ", e);
        }

        return outputClasses;
    }

    public List<List<String>> getClassesFromFormulas(List<String> formulas) {

        List<List<String>> outputClasses = new ArrayList<>();
        try {
            final HttpPost request = new HttpPost(serverUrl + "/classify/formula");
            request.setHeader("Accept", APPLICATION_JSON);

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setCharset(StandardCharsets.UTF_8);
            builder.addTextBody("input", toJson(formulas), ContentType.APPLICATION_JSON);

            HttpEntity multipart = builder.build();
            request.setEntity(multipart);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
                    LOGGER.error("Not OK answer. Status code: " + response.getStatusLine().getStatusCode());
                } else {
                    outputClasses = fromJsonMultiple(response.getEntity().getContent());
                }
            }

        } catch (UnknownHostException e) {
            LOGGER.warn("The service is unreachable. Ignoring it. ", e);
        } catch (IOException e) {
            LOGGER.error("Something generally bad happened. ", e);
        }

        return outputClasses;
    }


    public String toJson(List<String> passage) {
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

    public List<String> fromJson(InputStream inputLine) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
            mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
            return mapper.readValue(inputLine, new TypeReference<List<String>>() {
            });
        } catch (JsonGenerationException | JsonMappingException e) {
            LOGGER.error("The input line cannot be processed\n " + inputLine + "\n ", e);
        } catch (IOException e) {
            LOGGER.error("Some serious error when deserialize the JSON object: \n" + inputLine, e);
        }
        return null;
    }

    public List<List<String>> fromJsonMultiple(InputStream inputLine) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
            mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
            return mapper.readValue(inputLine, new TypeReference<List<List<String>>>() {
            });
        } catch (JsonGenerationException | JsonMappingException e) {
            LOGGER.error("The input line cannot be processed\n " + inputLine + "\n ", e);
        } catch (IOException e) {
            LOGGER.error("Some serious error when deserialize the JSON object: \n" + inputLine, e);
        }
        return null;
    }


}
