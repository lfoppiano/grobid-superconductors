package org.grobid.core.engines;

import org.grobid.core.data.SuperconEntry;
import org.grobid.core.data.document.DocumentResponse;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.grobid.core.engines.label.SuperconductorsTaggingLabels.SUPERCONDUCTORS_MATERIAL_LABEL;
import static org.grobid.core.engines.label.SuperconductorsTaggingLabels.SUPERCONDUCTORS_TC_VALUE_LABEL;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

class TabularDataEngineTest {

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
    }

    @Test
    public void testComputeTabularData_noLinks() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("example-no_links.json");

        DocumentResponse documentResponse = DocumentResponse.fromJson(is);

        List<SuperconEntry> superconEntries = TabularDataEngine.computeTabularData(documentResponse.getPassages());

        assertThat(superconEntries, hasSize(0));
    }

    @org.junit.jupiter.api.Test
    public void testComputeTabularData_fullDocument() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("14fcc539b1.json");

        DocumentResponse documentResponse = DocumentResponse.fromJson(is);

        List<SuperconEntry> superconEntries = TabularDataEngine.computeTabularData(documentResponse.getPassages());

        assertThat(superconEntries, hasSize(20));

        assertThat(superconEntries.get(18).getRawMaterial(), is("HgBa 2 Ca 2 Cu 3 O 9"));
        assertThat(superconEntries.get(18).getCriticalTemperature(), is("55 K"));
        assertThat(superconEntries.get(18).getAppliedPressure(), is(nullValue()));
        assertThat(superconEntries.get(18).getSpans(), hasSize(2));

        assertThat(superconEntries.get(19).getRawMaterial(), is("HgBa 2 Ca 2 Cu 3 O 9"));
        assertThat(superconEntries.get(19).getCriticalTemperature(), is("up to 164 K"));
        assertThat(superconEntries.get(19).getAppliedPressure(), is("30 GPa"));
        assertThat(superconEntries.get(19).getSpans(), hasSize(3));

        superconEntries.stream().forEach(sE -> sE.getSpans().stream()
            .forEach(s -> {
                assertThat(s.getId(), is(notNullValue()));
                assertThat(s.getTokenStart(), is(0));
                assertThat(s.getTokenEnd(), is(0));
                assertThat(s.getType(), is(notNullValue()));
                assertThat(s.getOffsetStart(), is(greaterThan(0)));
                assertThat(s.getOffsetEnd(), is(greaterThan(s.getOffsetStart())));
            }));

    }

    @org.junit.jupiter.api.Test
    public void testComputeTabularData_multipleLinksInTc() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("sample_response.json");

        DocumentResponse documentResponse = DocumentResponse.fromJson(is);

        List<SuperconEntry> superconEntries = TabularDataEngine.computeTabularData(documentResponse.getPassages());

        assertThat(superconEntries, hasSize(2));
        assertThat(superconEntries.get(0).getRawMaterial(), is("HgBa 2 Ca 2 Cu 3 O 9"));
        assertThat(superconEntries.get(0).getCriticalTemperature(), is("55 K"));
        assertThat(superconEntries.get(0).getAppliedPressure(), is(nullValue()));
        assertThat(superconEntries.get(0).getSpans(), hasSize(2));

        assertThat(superconEntries.get(1).getRawMaterial(), is("HgBa 2 Ca 2 Cu 3 O 9"));
        assertThat(superconEntries.get(1).getCriticalTemperature(), is("up to 164 K"));
        assertThat(superconEntries.get(1).getAppliedPressure(), is("30 GPa"));
        assertThat(superconEntries.get(1).getSpans(), hasSize(3));
    }

    @org.junit.jupiter.api.Test
    public void testComputeTabularData_measurementMethod() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("sample_response_meMethods.json");

        DocumentResponse documentResponse = DocumentResponse.fromJson(is);

        List<SuperconEntry> superconEntries = TabularDataEngine.computeTabularData(documentResponse.getPassages());

        assertThat(superconEntries, hasSize(1));
        assertThat(superconEntries.get(0).getRawMaterial(), is("La 3 Ir 2 Ge 2"));
        assertThat(superconEntries.get(0).getCriticalTemperature(), is("4.7 K"));
        assertThat(superconEntries.get(0).getMeasurementMethod(), is("resistance"));
        assertThat(superconEntries.get(0).getAppliedPressure(), is("4.2 GPa"));
        assertThat(superconEntries.get(0).getSpans(), hasSize(4));
    }

}