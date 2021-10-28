package org.grobid.core.data;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.grobid.core.data.document.Token;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class TokenTest {

    @Test
    public void testJson() throws Exception {
        String token = "{\"text\": \".\", \"style\": \"baseline\", \"offset\": 140, \"fontSize\": 0.1, \"bold\": true, \"italic\": false}";

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        Token output = mapper.readValue(token, Token.class);
            
        assertThat(output, is(not(nullValue())));
        assertThat(output.getText(), is("."));
        assertThat(output.getStyle(), is("baseline"));
        assertThat(output.getOffset(), is(140));
        assertThat(output.getFont(), is(nullValue()));
        assertThat(output.isItalic(), is(false));
        assertThat(output.isBold(), is(true));
        assertThat(output.getFontSize(), is(0.1));

    }
}