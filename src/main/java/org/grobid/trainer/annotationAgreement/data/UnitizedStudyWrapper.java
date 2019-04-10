package org.grobid.trainer.annotationAgreement.data;

import com.ctc.wstx.stax.WstxInputFactory;
import org.codehaus.stax2.XMLStreamReader2;
import org.dkpro.statistics.agreement.unitizing.IUnitizingAnnotationStudy;
import org.dkpro.statistics.agreement.unitizing.IUnitizingAnnotationUnit;
import org.dkpro.statistics.agreement.unitizing.KrippendorffAlphaUnitizingAgreement;
import org.dkpro.statistics.agreement.unitizing.UnitizingAnnotationStudy;
import org.grobid.trainer.stax.AnnotationExtractionStaxHandler;
import org.grobid.trainer.stax.StaxUtils;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.grobid.service.command.InterAnnotationAgreementCommand.ANNOTATION_DEFAULT_TAGS;
import static org.grobid.service.command.InterAnnotationAgreementCommand.TOP_LEVEL_ANNOTATION_DEFAULT_TAGS;

/**
 * Wrapper around the UnifiedStudy which will simplify the processing of inter annotation agreement measures
 */
public class UnitizedStudyWrapper {

    private WstxInputFactory inputFactory = new WstxInputFactory();

    private List<InputStream> filenames = new ArrayList<>();

    private List<String> continuums = new ArrayList<>();

    private UnitizingAnnotationStudy study;

    /**
     * Initialise the Unitized study and load the list of files. These files are supposed to be part of the same
     * annotated document, each file is a version produced by a different annotator.
     * <p>
     * If there are parsing problems or one of the files has a different text, the process is interrupted with an exception.
     **/
    public UnitizedStudyWrapper(List<InputStream> filenames) {
        String firstContinuum = null;

        for (InputStream file : filenames) {
            try {
                AnnotationExtractionStaxHandler handler = new AnnotationExtractionStaxHandler(TOP_LEVEL_ANNOTATION_DEFAULT_TAGS,
                        ANNOTATION_DEFAULT_TAGS);

                XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(file);
                StaxUtils.traverse(reader, handler);

                this.filenames.add(file);
                this.continuums.add(handler.getContinuum());

                if (firstContinuum != null) {
                    if (handler.getContinuum().length() != firstContinuum.length()) {
                        throw new RuntimeException("Continuum between different annotators are not matching, please fix it before re-trying. " +
                                firstContinuum.length() + " vs " + handler.getContinuum().length() + "\n\n" +
                                "1: " + firstContinuum + "\n\n" + "2: " + handler.getContinuum());
                    }

                    handler.getData().forEach(annotation -> {
                        String annotationName = annotation.getLeft();
                        Integer start = annotation.getMiddle();
                        Integer length = annotation.getRight();

                        study.addUnit(start, length, filenames.indexOf(file), annotationName);
                    });

                } else {
                    firstContinuum = handler.getContinuum();
                    study = new UnitizingAnnotationStudy(filenames.size(), firstContinuum.length());

                    handler.getData().forEach(annotation -> {
                        String annotationName = annotation.getLeft();
                        Integer start = annotation.getMiddle();
                        Integer length = annotation.getRight();

                        study.addUnit(start, length, filenames.indexOf(file), annotationName);
                    });
                }

            } catch (XMLStreamException e) {
                throw new RuntimeException("Annotation XML not well formed, fix it before re-trying. ", e);
            }
        }


    }

    public static double getAgreement(UnitizingAnnotationStudy study) {
        KrippendorffAlphaUnitizingAgreement agreement = new KrippendorffAlphaUnitizingAgreement(study);

        return agreement.calculateAgreement();
    }


    public double getAgreement() {
        return getAgreement(this.study);
    }

    /**
     * Return a map with the agreement by category for this study
     **/
    public Map<String, Double> getAgreementByCategory() {
        return getAgreementByCategory(this.study);
    }

    /**
     * Return a map with the agreement by cateogory for this study
     **/
    public static Map<String, Double> getAgreementByCategory(IUnitizingAnnotationStudy study) {
        Map<String, Double> agreementsByCategory = new HashMap<>();
        KrippendorffAlphaUnitizingAgreement agreement = new KrippendorffAlphaUnitizingAgreement(study);

        study.getCategories().forEach(category -> {
            agreementsByCategory.put((String) category, agreement.calculateCategoryAgreement(category));
        });

        return agreementsByCategory;
    }

    /**
     * Returns matrices by pairwise agreement between annotators
     */
    public List<InterAnnotationAgreementPairwiseComparisonEntry> getPairwiseRaterAgreementMatrices() {

        List<InterAnnotationAgreementPairwiseComparisonEntry> pairwiseRaterAgreement = new ArrayList<>();

        for (int idx1 = 0; idx1 < this.filenames.size(); idx1++) {
            for (int idx2 = idx1 + 1; idx2 < this.filenames.size(); idx2++) {
                UnitizingAnnotationStudy localStudy = new UnitizingAnnotationStudy(2, (int) study.getContinuumLength());

                // this is a workaround !!
                for (IUnitizingAnnotationUnit annotation : study.getUnits()) {
                    if (annotation.getRaterIdx() == idx1) {
                        localStudy.addUnit(annotation.getOffset(), annotation.getLength(), 0, annotation.getCategory());
                    } else if (annotation.getRaterIdx() == idx2) {
                        localStudy.addUnit(annotation.getOffset(), annotation.getLength(), 1, annotation.getCategory());
                    }
                }

                InterAnnotationAgreementPairwiseComparisonEntry interAnnotationAgreementPairwiseComparisonEntry
                        = new InterAnnotationAgreementPairwiseComparisonEntry(idx1, idx2);

                Map<String, Double> agreementByCategory = getAgreementByCategory(localStudy);

                for (String key : agreementByCategory.keySet()) {
                    interAnnotationAgreementPairwiseComparisonEntry.addAgreementFigures(key, agreementByCategory.get(key));
                }
                interAnnotationAgreementPairwiseComparisonEntry.setAgreementAverage(getAgreement(localStudy));

                pairwiseRaterAgreement.add(interAnnotationAgreementPairwiseComparisonEntry);

            }
        }

        return pairwiseRaterAgreement;
    }

    public UnitizingAnnotationStudy getStudy() {
        return study;
    }

    public List<String> getContinuums() {
        return continuums;
    }
}
