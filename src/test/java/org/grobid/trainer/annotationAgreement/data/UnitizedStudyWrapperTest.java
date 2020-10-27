package org.grobid.trainer.annotationAgreement.data;

import org.dkpro.statistics.agreement.unitizing.UnitizingAnnotationStudy;
import org.easymock.EasyMock;
import org.grobid.trainer.stax.StackTags;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

public class UnitizedStudyWrapperTest {

    UnitizingAnnotationStudy mockedStudy;

    @Before
    public void setUp() throws Exception {
        mockedStudy = EasyMock.createMock(UnitizingAnnotationStudy.class);
    }

    @Test
    public void test_loadingFiles_shouldWork() throws Exception {

        List<InputStream> inputStreams = new ArrayList<>();
        inputStreams.add(this.getClass().getResourceAsStream("study1.xml"));
        inputStreams.add(this.getClass().getResourceAsStream("study2.xml"));
        inputStreams.add(this.getClass().getResourceAsStream("study3.xml"));

        UnitizedStudyWrapper unitizedStudyWrapper = new UnitizedStudyWrapper(inputStreams,
            Arrays.asList(StackTags.from("/tei/text/p")), Arrays.asList("material", "class"));

        assertThat(unitizedStudyWrapper.getContinuums(), hasSize(3));
        assertThat(unitizedStudyWrapper.getAgreementByCategory().keySet(), hasSize(2));
        assertThat(unitizedStudyWrapper.getAgreementByCategory().get("material"), is(1.0));
        assertThat(unitizedStudyWrapper.getAgreementByCategory().get("class"), is(1.0));
        assertThat(unitizedStudyWrapper.getAgreement(), is(1.0));
        assertThat(unitizedStudyWrapper.getStudy(), is(not(nullValue())));
    }

    @Test(expected = RuntimeException.class)
    public void test_loadingFiles_Malformed_shouldThrowException() throws Exception {

        List<InputStream> inputStreams = new ArrayList<>();
        inputStreams.add(this.getClass().getResourceAsStream("study1.xml"));
        inputStreams.add(this.getClass().getResourceAsStream("study2.malformed.xml"));
        inputStreams.add(this.getClass().getResourceAsStream("study3.xml"));

        new UnitizedStudyWrapper(inputStreams);
    }

    @Test(expected = RuntimeException.class)
    public void test_loadingFiles_Malformed2_shouldThrowException() throws Exception {

        List<InputStream> inputStreams = new ArrayList<>();
        inputStreams.add(this.getClass().getResourceAsStream("study1.xml"));
        inputStreams.add(this.getClass().getResourceAsStream(null));
        inputStreams.add(this.getClass().getResourceAsStream("study3.xml"));

        new UnitizedStudyWrapper(inputStreams);
    }

    @Test
    public void testGetAgreement_shouldWork() throws Exception {
        List<InputStream> inputStreams = new ArrayList<>();
        inputStreams.add(this.getClass().getResourceAsStream("study1.xml"));
        inputStreams.add(this.getClass().getResourceAsStream("study2.xml"));
        inputStreams.add(this.getClass().getResourceAsStream("study3.xml"));

        UnitizedStudyWrapper unitizedStudyWrapper = new UnitizedStudyWrapper(inputStreams,
            Arrays.asList(StackTags.from("/tei/text/p")), Arrays.asList("material", "class"));

        assertThat(unitizedStudyWrapper.getAgreement(), is(UnitizedStudyWrapper.getAgreement(unitizedStudyWrapper.getStudy())));
    }

    @Test
    public void testPairwiseComparision_100percentAgreement_shouldWork() throws Exception {

        List<InputStream> inputStreams = new ArrayList<>();
        inputStreams.add(this.getClass().getResourceAsStream("study1.xml"));
        inputStreams.add(this.getClass().getResourceAsStream("study2.xml"));
        inputStreams.add(this.getClass().getResourceAsStream("study3.xml"));

        UnitizedStudyWrapper unitizedStudyWrapper = new UnitizedStudyWrapper(inputStreams,
            Arrays.asList(StackTags.from("/tei/text/p")), Arrays.asList("material", "class"));

        List<InterAnnotationAgreementPairwiseComparisonEntry> matrices
                = unitizedStudyWrapper.getPairwiseRaterAgreementMatrices();

        assertThat(matrices, hasSize(3));
        assertThat(matrices.get(0).getRater0(), is(0));
        assertThat(matrices.get(0).getRater1(), is(1));
        assertThat(matrices.get(0).getAgreementAverage(), is(1.0));

        assertThat(matrices.get(1).getRater0(), is(0));
        assertThat(matrices.get(1).getRater1(), is(2));
        assertThat(matrices.get(1).getAgreementAverage(), is(1.0));

        assertThat(matrices.get(2).getRater0(), is(1));
        assertThat(matrices.get(2).getRater1(), is(2));
        assertThat(matrices.get(2).getAgreementAverage(), is(1.0));
    }


//    @Test
//    public void test_loadingFiles_shouldWork() throws Exception {
//
//        List<InputStream> inputStreams = new ArrayList<>();
//        inputStreams.add(this.getClass().getResourceAsStream("study1.xml"));
//        inputStreams.add(this.getClass().getResourceAsStream("study2.xml"));
//        inputStreams.add(this.getClass().getResourceAsStream("study3.xml"));
//
//        UnitizedStudyWrapper unitizedStudyWrapper = new UnitizedStudyWrapper(inputStreams);
//
//        assertThat(unitizedStudyWrapper.getContinuums(), hasSize(3));
//        assertThat(unitizedStudyWrapper.getAgreementByCategory().keySet(), hasSize(2));
//        assertThat(unitizedStudyWrapper.getAgreementByCategory().get("supercon"), is(1.0));
//        assertThat(unitizedStudyWrapper.getAgreementByCategory().get("propertyValue"), is(1.0));
//        assertThat(unitizedStudyWrapper.getAgreement(), is(1.0));
//        assertThat(unitizedStudyWrapper.getStudy(), is(not(nullValue())));
//    }

}