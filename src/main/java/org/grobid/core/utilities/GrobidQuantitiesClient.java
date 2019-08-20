package org.grobid.core.utilities;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.grobid.core.data.chemspot.Mention;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Singleton
public class GrobidQuantitiesClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidQuantitiesClient.class);

    private final String grobidQuantitiesUrl;
    private GrobidSuperconductorsConfiguration configuration;
    private HttpClient httpClient;

    public GrobidQuantitiesClient(String grobidQuantitiesUrl) {
        this.grobidQuantitiesUrl = grobidQuantitiesUrl;
        this.httpClient = HttpClientBuilder.create().build();
    }

    @Inject
    public GrobidQuantitiesClient(GrobidSuperconductorsConfiguration configuration) {
        this.configuration = configuration;
        this.grobidQuantitiesUrl = configuration.getGrobidQuantitiesUrl();
        this.httpClient = HttpClientBuilder.create().build();
    }

    public LinkedHashMap<String, Object> processText(String text) {

        try {
            final HttpPost request = new HttpPost(grobidQuantitiesUrl + "/service/processQuantityText");
            request.setHeader("Accept", APPLICATION_JSON);
            request.setHeader("Content-type", APPLICATION_FORM_URLENCODED);

//            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
//            builder.addTextBody("text", text);
//
//            HttpEntity multipart = builder.build();
//            request.setEntity(multipart);

            List<NameValuePair> formparams = new ArrayList<>();
            formparams.add(new BasicNameValuePair("text", text));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, Consts.UTF_8);
            request.setEntity(entity);

            final HttpResponse response = httpClient.execute(request);
            if (response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
                LOGGER.error("Not OK answer. Status code: " + response.getStatusLine().getStatusCode());
            } else {
                return fromJson(response.getEntity().getContent());
            }

        } catch (UnknownHostException e) {
            LOGGER.warn("Grobid quantities is unreachable. Ignoring it. ");
        } catch (IOException e) {
            LOGGER.error("Something generically bad happened. ", e);
        }

        return null;
    }

    public LinkedHashMap<String, Object> fromJson(InputStream inputLine) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
            mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
            return mapper.readValue(inputLine, LinkedHashMap.class);
        } catch (JsonGenerationException | JsonMappingException e) {
            LOGGER.error("The input line cannot be processed\n " + inputLine + "\n ", e);
        } catch (IOException e) {
            LOGGER.error("Some serious error when deserialize the JSON object: \n" + inputLine, e);
        }
        return null;
    }

}
