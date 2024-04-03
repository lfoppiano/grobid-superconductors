package org.grobid.service.controller;

import com.ctc.wstx.stax.WstxInputFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.stax2.XMLStreamReader2;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.grobid.core.data.document.Span;
import org.grobid.core.data.document.TextPassage;
import org.grobid.core.engines.linking.CRFBasedLinker;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;
import org.grobid.service.exceptions.GrobidServiceException;
import org.grobid.trainer.stax.StaxUtils;
import org.grobid.trainer.stax.handler.AnnotationValuesStaxHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.length;

@Singleton
@Path("/linker")
public class LinkerController {
    private static final Logger LOGGER = LoggerFactory.getLogger(LinkerController.class);

    private final WstxInputFactory inputFactory = new WstxInputFactory();
    private final GrobidSuperconductorsConfiguration configuration;
    
    @Inject
    private CRFBasedLinker linker;

    @Inject
    public LinkerController(GrobidSuperconductorsConfiguration configuration) {
        this.configuration = configuration;
    }

    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Set<String> getLinkEngines() {
        return linker.getLinkingEngines().keySet();
    }

    @Path("link")
    @Produces(MediaType.APPLICATION_JSON)
    @POST
    public List<TextPassage> processTextSuperconductorsPost(@FormDataParam("text") String text, @FormDataParam("type") String linkerType) {
        return link(text, linkerType);
    }

    @Path("link")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public List<TextPassage> processTextSuperconductorsGet(@FormDataParam("text") String text, @FormDataParam("type") String linkerType) {
        return link(text, linkerType);
    }

    private List<TextPassage> link(@FormDataParam("text") String text, @FormDataParam("type") String linkerType) {
        if (!getLinkEngines().contains(linkerType)) {
            throw new RuntimeException("The supplied linker type " + linkerType + "does not exists. Please one among " + getLinkEngines());
        }

        List<TextPassage> textPassages = new ArrayList<>();

        String textPreprocessed = text.replace("\r\n", "\n");
        String artificialXml = "<text><p><s>" + textPreprocessed + "</s></p></text>";

        InputStream stream = new ByteArrayInputStream(artificialXml.getBytes(StandardCharsets.UTF_8));
        AnnotationValuesStaxHandler handler = new AnnotationValuesStaxHandler(linker.getLinkingEngines().get(linkerType).getAnnotationsToBeLinked());
        try {
            XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(stream);
            StaxUtils.traverse(reader, handler);

            List<Pair<String, String>> labeled = handler.getLabeledEntities();
            List<Integer> offsetsAnnotationsTags = handler.getOffsetsAnnotationsTags();
            List<Pair<String, String>> identifiers = handler.getIdentifiers();

            List<Span> annotations = new ArrayList<>();

            for (int i = 0; i < labeled.size(); i++) {
                Pair<String, String> labeledToken = labeled.get(i);
                String token = labeledToken.getLeft();
                String label = labeledToken.getRight();
                Integer integer = offsetsAnnotationsTags.get(i);

                String id = identifiers.get(i).getLeft();

                if (!label.equals(identifiers.get(i).getRight())) {
                    throw new RuntimeException("The label " + identifiers.get(i).getRight() + " corresponding to the id " + id + "does not match it's tag: " + label);
                }
                Span span = null;
                if ("".equals(id)) {
                    span = new Span(token, label);
                } else {
                    span = new Span(id, token, label);
                }

                span.setOffsetStart(integer);
                span.setOffsetEnd(integer + length(token));
                span.setLinkable(true);

                if (!span.getText().equals(handler.getGlobalAccumulatedText().substring(span.getOffsetStart(), span.getOffsetEnd()))) {
                    LOGGER.warn("Mismatch between the global accumulated text and the extracted entities. Span text: "
                        + span.getText() + " substring: "
                        + textPreprocessed.substring(span.getOffsetStart(), span.getOffsetEnd()));
                }

                annotations.add(span);
            }

            annotations.stream().forEach(s -> s.setLinkable(true));

            List<Span> taggedAnnotations = linker.process(handler.getGlobalAccumulatedText(), annotations, linkerType);

            TextPassage textPassage = new TextPassage();
            textPassage.setText(handler.getGlobalAccumulatedText());
            textPassage.setSpans(taggedAnnotations);

            textPassages.add(textPassage);
        } catch (XMLStreamException e) {
            throw new GrobidServiceException("Invalid data supplied to the linker", e, Response.Status.BAD_REQUEST);
        }

        return textPassages;
    }
}
