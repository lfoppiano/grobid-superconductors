package org.grobid.core.features;

import org.grobid.core.layout.LayoutToken;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.grobid.core.engines.SuperconductorsParser.NONE_CHEMSPOT_TYPE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class FeaturesVectorSuperconductorsTest {

    FeaturesVectorSuperconductors target;

    @Before
    public void setUp() throws Exception {
        target = new FeaturesVectorSuperconductors();
    }

    @Ignore("to be activated with the new features")
    @Test
    public void printVector() {
        LayoutToken token = new LayoutToken();

        token.setText("token1");
        token.fontSize = 3;
        token.setFont("Arial");
        token.setItalic(false);
        token.setBold(true);
        token.setSuperscript(true);

        LayoutToken previousToken = new LayoutToken();

        previousToken.setText("token1");
        previousToken.fontSize = 3;
        previousToken.setFont("Arial");
        previousToken.setItalic(false);
        previousToken.setBold(true);


        FeaturesVectorSuperconductors features = target.addFeatures(token, "bao", previousToken, NONE_CHEMSPOT_TYPE);

        assertThat(features.printVector(), is("token1 token1 t to tok toke 1 n1 en1 ken1 NOCAPS CONTAINDIGIT 0 NOPUNCT tokenX xxxd xd SAMEFONT SAMEFONTSIZE true false BASELINE NONE bao"));
    }
}