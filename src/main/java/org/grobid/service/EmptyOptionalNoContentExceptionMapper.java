package org.grobid.service;

import io.dropwizard.jersey.optional.EmptyOptionalException;
import io.dropwizard.jersey.optional.EmptyOptionalExceptionMapper;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;


/**
 * Returns a 204 for Optional.empty()
 * {@link EmptyOptionalExceptionMapper} returns a 404 for Optional.empty()
 */
public class EmptyOptionalNoContentExceptionMapper implements ExceptionMapper<EmptyOptionalException> {
    @Override
    public Response toResponse(EmptyOptionalException exception) {
        return Response.noContent().build();
    }
}