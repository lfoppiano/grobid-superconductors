package org.grobid.service.controller;

import com.squarespace.jersey2.guice.JerseyGuiceUtils;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Entity;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

public class AnnotationControllerTest {

    static {
        JerseyGuiceUtils.install((s, serviceLocator) -> null);
    }

    @ClassRule
    public static final ResourceTestRule RULE = ResourceTestRule.builder()
        .addResource(new AnnotationController(null, null))
        .addProvider(MultiPartFeature.class)
        .build();


    @Test
    public void testResource() {
        final MultiPart multiPartEntity = new FormDataMultiPart()
            .field("input", "{\"runtime\": 1234}", APPLICATION_JSON_TYPE);

        String s = RULE.client().target("/process/json")
            .register(MultiPartFeature.class).request()
            .post(Entity.entity(multiPartEntity, multiPartEntity.getMediaType()))
            .readEntity(String.class);

        System.out.println(s);
    }

}