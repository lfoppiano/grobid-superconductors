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
import org.grobid.core.data.material.ChemicalComposition;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Singleton
public class ChemicalMaterialParserClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChemicalMaterialParserClient.class);

    private final String serverUrl;
    private GrobidSuperconductorsConfiguration configuration;
    private CloseableHttpClient httpClient;

    public ChemicalMaterialParserClient(String serverUrl) {
        this.serverUrl = serverUrl;
        this.httpClient = HttpClientBuilder.create().build();
    }

    @Inject
    public ChemicalMaterialParserClient(GrobidSuperconductorsConfiguration configuration) {
        this.configuration = configuration;
        this.serverUrl = configuration.getClassResolverUrl();
        this.httpClient = HttpClientBuilder.create().build();
    }

    public ChemicalComposition convertNameToFormula(String name) {

        ChemicalComposition outputFormula = new ChemicalComposition();
        try {
            final HttpPost request = new HttpPost(serverUrl + "/convert/name/formula");
            request.setHeader("Accept", APPLICATION_JSON);

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setCharset(StandardCharsets.UTF_8);
            builder.addTextBody("input", name, ContentType.APPLICATION_JSON);

            HttpEntity multipart = builder.build();
            request.setEntity(multipart);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode != HttpURLConnection.HTTP_OK) {
                    LOGGER.debug("Not OK answer. Input: " + name + ". Status code: " + response.getStatusLine().getStatusCode());
                } else {
                    outputFormula = fromJsonToChemicalComposition(response.getEntity().getContent());
                    if (outputFormula != null && outputFormula.getCode() != HttpURLConnection.HTTP_OK) {
                        LOGGER.debug("Not OK answer. Input: " + name + ". " +
                            "Status code: " + outputFormula.getCode() +
                            "Message: " + outputFormula.getMessage());
                        outputFormula = new ChemicalComposition();
                    }
                }
            }

        } catch (UnknownHostException e) {
            LOGGER.warn("The service is unreachable. Input: " + name + ". Ignoring it. ", e);
        } catch (IOException e) {
            LOGGER.error("Something generally bad happened. Input: " + name + ".", e);
        }

        return outputFormula;
    }

    public ChemicalComposition convertFormulaToComposition(String formula) {

        ChemicalComposition outputComposition = new ChemicalComposition();
        try {
            final HttpPost request = new HttpPost(serverUrl + "/convert/formula/composition");
            request.setHeader("Accept", APPLICATION_JSON);

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setCharset(StandardCharsets.UTF_8);
            builder.addTextBody("input", formula, ContentType.APPLICATION_JSON);

            HttpEntity multipart = builder.build();
            request.setEntity(multipart);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
                    LOGGER.debug("Not OK answer. Input: " + formula + ". Status code: " + response.getStatusLine().getStatusCode());
                } else {
                    outputComposition = fromJsonToChemicalComposition(response.getEntity().getContent());
                    if (outputComposition != null && outputComposition.getCode() != HttpURLConnection.HTTP_OK) {
                        LOGGER.debug("Not OK answer. Input: " + formula + ". Status code: " + outputComposition.getCode() +
                            "Message: " + outputComposition.getMessage());
                        outputComposition = new ChemicalComposition();
                    }
                }
            }

        } catch (UnknownHostException e) {
            LOGGER.warn("The service is unreachable. Input: " + formula + ".  Ignoring it. ", e);
        } catch (IOException e) {
            LOGGER.error("Something generally bad happened. ", e);
        }

        return outputComposition;
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

    public static ChemicalComposition fromJsonToChemicalComposition(InputStream inputLine) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
            mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
            return mapper.readValue(inputLine, new TypeReference<ChemicalComposition>() {
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
