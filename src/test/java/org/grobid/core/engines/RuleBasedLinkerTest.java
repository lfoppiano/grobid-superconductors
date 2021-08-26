package org.grobid.core.engines;


import org.easymock.Capture;
import org.easymock.EasyMock;
import org.grobid.core.data.Link;
import org.grobid.core.data.Span;
import org.grobid.core.data.TextPassage;
import org.grobid.core.data.Token;
import org.grobid.core.utilities.LinkingModuleClient;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class RuleBasedLinkerTest {

    RuleBasedLinker target;
    LinkingModuleClient mockLinkingModuleClient;

    @Before
    public void setUp() throws Exception {
        mockLinkingModuleClient = EasyMock.createMock(LinkingModuleClient.class);
        GrobidSuperconductorsConfiguration configuration = new GrobidSuperconductorsConfiguration();
        target = new RuleBasedLinker(configuration, mockLinkingModuleClient);
    }

    @Test
    public void testProcess_CorrectlyCreatingNewObjects() throws Exception {
        TextPassage textPassage = new TextPassage();
        textPassage.setText("This is a text");
        textPassage.setTokens(Arrays.asList(new Token("Ciao", "", 2.0, "", 2, false, false)));
        Span inputSpan = new Span("bao", "material");
        inputSpan.getAttributes().put("ciao", "miao");
        textPassage.setSpans(Arrays.asList(inputSpan));

        TextPassage clientReturnedPassage = new TextPassage();
        clientReturnedPassage.setText("This is a text eeehh?");
        Span outputSpan = new Span("bao", "material");
        Link createdLink = new Link("1", "tefdfd", "<tcValue>", "type");
        outputSpan.setLinks(Arrays.asList(createdLink));
        clientReturnedPassage.setSpans(Arrays.asList(outputSpan));
        Capture<List<TextPassage>> passageCapture = EasyMock.newCapture();
        EasyMock
            .expect(mockLinkingModuleClient.extractLinks(EasyMock.capture(passageCapture), EasyMock.anyObject(), EasyMock.anyBoolean()))
            .andReturn(Arrays.asList(clientReturnedPassage));

        EasyMock.replay(mockLinkingModuleClient);

        List<TextPassage> outputPassages = target.process(Arrays.asList(textPassage));
        TextPassage outputPassage = outputPassages.get(0);
        
        List<TextPassage> capturedPassages = passageCapture.getValue();
        TextPassage capturedPassage = capturedPassages.get(0);
        assertThat(capturedPassage, is(not(nullValue())));
        assertThat(capturedPassage.getSpans(), hasSize(1));
        assertThat(capturedPassage.getSpans().get(0).getLinks(), hasSize(0));
        assertThat(outputPassage.getSpans(), hasSize(1));
        assertThat(outputPassage.getSpans().get(0).getLinks(), hasSize(1));
        assertThat(outputPassage.getSpans().get(0).getLinks().get(0).getTargetId(), is(createdLink.getTargetId()));
        assertThat(outputPassage.getSpans().get(0).getLinks().get(0).getType(), is(createdLink.getType()));
        assertThat(outputPassage.getSpans().get(0).getLinks().get(0).getTargetText(), is(createdLink.getTargetText()));
        assertThat(outputPassage.getSpans().get(0).getLinks().get(0).getTargetType(), is(createdLink.getTargetType()));

        assertThat(outputPassage.getSpans().get(0).getAttributes().keySet(), hasSize(1));

        EasyMock.verify(mockLinkingModuleClient);
    }

    @Test
    public void testmarkTemperature_CorrectlyCreatingNewObjects() throws Exception {
        TextPassage textPassage = new TextPassage();
        textPassage.setText("This is a text");
        textPassage.setTokens(Arrays.asList(new Token("Ciao", "", 2.0, "", 2, false, false)));
        Span inputSpan1 = new Span("bao", "tcValue");
        Span inputSpan2 = new Span("miao", "tcValue");
        inputSpan1.getAttributes().put("ciao", "miao");
        inputSpan1.getAttributes().put("ciao2", "miao");
        textPassage.setSpans(Arrays.asList(inputSpan1, inputSpan2));

        TextPassage clientReturnedPassage = new TextPassage();
        clientReturnedPassage.setText("This is a text eeehh?");
        Span outputSpan1 = new Span("bao", "tcValue");
        Span outputSpan2 = new Span("miao", "tcValue");
        outputSpan1.setLinkable(true);
        clientReturnedPassage.setSpans(Arrays.asList(outputSpan1, outputSpan2));
        Capture<List<TextPassage>> passageCapture = EasyMock.newCapture();
        EasyMock
            .expect(mockLinkingModuleClient.markCriticalTemperature(EasyMock.capture(passageCapture)))
            .andReturn(Arrays.asList(clientReturnedPassage));

        EasyMock.replay(mockLinkingModuleClient);

        List<TextPassage> outputPassages = target.markTemperatures(Arrays.asList(textPassage));
        TextPassage outputPassage = outputPassages.get(0);  

        List<TextPassage> capturedPassages = passageCapture.getValue();
        TextPassage capturedPassage = capturedPassages.get(0);

        assertThat(capturedPassage, is(not(nullValue())));
        assertThat(capturedPassage.getSpans(), hasSize(2));
        assertThat(capturedPassage.getSpans().get(0).getLinks(), hasSize(0));
        assertThat(capturedPassage.getSpans().get(0).isLinkable(), is(false));
        assertThat(capturedPassage.getSpans().get(1).getLinks(), hasSize(0));
        assertThat(capturedPassage.getSpans().get(1).isLinkable(), is(false));

        assertThat(outputPassage.getSpans(), hasSize(2));
        assertThat(outputPassage.getSpans().get(0).getLinks(), hasSize(0));
        assertThat(outputPassage.getSpans().get(0).getText(), is(inputSpan1.getText()));
        assertThat(outputPassage.getSpans().get(0).isLinkable(), is(true));
        assertThat(outputPassage.getSpans().get(1).getLinks(), hasSize(0));
        assertThat(outputPassage.getSpans().get(1).isLinkable(), is(false));

        EasyMock.verify(mockLinkingModuleClient);
    }
}