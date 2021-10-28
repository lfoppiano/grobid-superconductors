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
import org.grobid.core.data.external.chemDataExtractor.ChemicalSpan;
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
import java.util.ArrayList;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Singleton
public class StructureIdentificationModuleClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(StructureIdentificationModuleClient.class);

    private final String serverUrl;
    private GrobidSuperconductorsConfiguration configuration;
    private CloseableHttpClient httpClient;

    public StructureIdentificationModuleClient(String serverUrl) {
        this.serverUrl = serverUrl;
        this.httpClient = HttpClientBuilder.create().build();
    }

    @Inject
    public StructureIdentificationModuleClient(GrobidSuperconductorsConfiguration configuration) {
        this.configuration = configuration;
        this.serverUrl = configuration.getLinkingModuleUrl();
        this.httpClient = HttpClientBuilder.create().build();
    }

    public List<String> processStructure(String text) {

        List<String> outputClasses = new ArrayList<>();
        try {
            final HttpPost request = new HttpPost(serverUrl + "/process/structure/text/single");
            request.setHeader("Accept", APPLICATION_JSON);

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setCharset(StandardCharsets.UTF_8);
            builder.addTextBody("text", text, ContentType.APPLICATION_JSON);

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

    public List<List<ChemicalSpan>> extractStructuresMulti(List<String> texts) {

        List<List<ChemicalSpan>> outputStructures = new ArrayList<>();
        try {
            final HttpPost request = new HttpPost(serverUrl + "/process/structure/text");
            request.setHeader("Accept", APPLICATION_JSON);

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setCharset(StandardCharsets.UTF_8);
            builder.addTextBody("input", toJson(texts), ContentType.APPLICATION_JSON);

            HttpEntity multipart = builder.build();
            request.setEntity(multipart);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
                    LOGGER.error("Not OK answer. Status code: " + response.getStatusLine().getStatusCode());
                } else {
                    outputStructures = ChemDataExtractorClient.fromJsonBulk(response.getEntity().getContent());
                }
            }

        } catch (UnknownHostException e) {
            LOGGER.warn("The service is unreachable. Ignoring it. ", e);
        } catch (IOException e) {
            LOGGER.error("Something generally bad happened. ", e);
        }

        return outputStructures;
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
            return mapper.readValue(inputLine, new TypeReference<List<String>>() {
            });
        } catch (JsonGenerationException | JsonMappingException e) {
            LOGGER.error("The input line cannot be processed\n " + inputLine + "\n ", e);
        } catch (IOException e) {
            LOGGER.error("Some serious error when deserialize the JSON object: \n" + inputLine, e);
        }
        return null;
    }


}
