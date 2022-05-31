package org.grobid.core.engines;

import org.grobid.core.data.SuperconEntry;
import org.grobid.core.data.document.DocumentResponse;

import java.io.InputStream;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

class TabularDataEngineTest {

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
    }

    @org.junit.jupiter.api.Test
    public void testComputeTabularData() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("14fcc539b1.json");

        DocumentResponse documentResponse = DocumentResponse.fromJson(is);

        List<SuperconEntry> superconEntries = TabularDataEngine.computeTabularData(documentResponse.getPassages());

        assertThat(superconEntries, hasSize(20));

        assertThat(superconEntries.get(18).getRawMaterial(), is("HgBa 2 Ca 2 Cu 3 O 9"));
        assertThat(superconEntries.get(18).getCriticalTemperature(), is("55 K"));
        assertThat(superconEntries.get(18).getAppliedPressure(), is(nullValue()));
        assertThat(superconEntries.get(18).getRawMaterial(), is("HgBa 2 Ca 2 Cu 3 O 9"));
        assertThat(superconEntries.get(19).getCriticalTemperature(), is("up to 164 K"));
        assertThat(superconEntries.get(19).getAppliedPressure(), is("30 GPa"));
    }

    @org.junit.jupiter.api.Test
    public void testComputeTabularData_sample() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("sample_response.json");

        DocumentResponse documentResponse = DocumentResponse.fromJson(is);

        List<SuperconEntry> superconEntries = TabularDataEngine.computeTabularData(documentResponse.getPassages());

        assertThat(superconEntries, hasSize(2));
        assertThat(superconEntries.get(0).getRawMaterial(), is("HgBa 2 Ca 2 Cu 3 O 9"));
        assertThat(superconEntries.get(0).getCriticalTemperature(), is("55 K"));
        assertThat(superconEntries.get(0).getAppliedPressure(), is(nullValue()));

        assertThat(superconEntries.get(1).getRawMaterial(), is("HgBa 2 Ca 2 Cu 3 O 9"));
        assertThat(superconEntries.get(1).getCriticalTemperature(), is("up to 164 K"));
        assertThat(superconEntries.get(1).getAppliedPressure(), is("30 GPa"));
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
    }
    
}