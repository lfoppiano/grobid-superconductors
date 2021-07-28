package org.grobid.service.controller;

import com.ctc.wstx.stax.WstxInputFactory;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.stax2.XMLStreamReader2;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.grobid.core.data.TextPassage;
import org.grobid.core.data.Span;
import org.grobid.core.engines.CRFBasedLinker;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;
import org.grobid.trainer.stax.StaxUtils;
import org.grobid.trainer.stax.handler.AnnotationValuesStaxHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.length;

@Singleton
@Path("/linker")
public class LinkerController {
    private static final Logger LOGGER = LoggerFactory.getLogger(LinkerController.class);

    private WstxInputFactory inputFactory = new WstxInputFactory();
    private final GrobidSuperconductorsConfiguration configuration;
    @Inject
    private CRFBasedLinker linker;

    @Inject
    public LinkerController(GrobidSuperconductorsConfiguration configuration) {
        this.configuration = configuration;
    }

    @Path("link")
    @Produces(MediaType.APPLICATION_JSON)
    @POST
    public List<TextPassage> processTextSuperconductorsPost(@FormDataParam("text") String text) {
        return link(text);
    }

    @Path("link")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public List<TextPassage> processTextSuperconductorsGet(@FormDataParam("text") String text) {
        return link(text);
    }

    private List<TextPassage> link(@FormDataParam("text") String text) {
        List<TextPassage> textPassages = new ArrayList<>();

        String textPreprocessed = text.replace("\r\n", "\n");
        String artificialXml = "<text><p><s>" + textPreprocessed + "</s></p></text>";

        InputStream stream = new ByteArrayInputStream(artificialXml.getBytes(StandardCharsets.UTF_8));
        AnnotationValuesStaxHandler handler = new AnnotationValuesStaxHandler(Arrays.asList("material", "tcValue"));
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

            linker.process(handler.getGlobalAccumulatedText(), annotations);

            TextPassage textPassage = new TextPassage();
            textPassage.setText(handler.getGlobalAccumulatedText());
            textPassage.setSpans(annotations);

            textPassages.add(textPassage);
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }

        return textPassages;
    }
}
