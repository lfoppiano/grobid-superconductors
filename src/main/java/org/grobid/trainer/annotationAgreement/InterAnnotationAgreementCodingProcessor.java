package org.grobid.trainer.annotationAgreement;

import com.ctc.wstx.stax.WstxInputFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.stax2.XMLStreamReader2;
import org.dkpro.statistics.agreement.coding.CodingAnnotationStudy;
import org.dkpro.statistics.agreement.coding.ICodingAnnotationStudy;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.trainer.stax.StaxUtils;
import org.grobid.trainer.stax.handler.AnnotationValuesTEIStaxHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class InterAnnotationAgreementCodingProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(InterAnnotationAgreementCodingProcessor.class);

    private WstxInputFactory inputFactory = new WstxInputFactory();

    public InterAnnotationAgreementCodingProcessor() {

    }


    private void processFilesForCodingStudy(List<File> directories, List<File> refFiles, ICodingAnnotationStudy study) throws FileNotFoundException {
        List<InputStream> filesToBeProcessed = new ArrayList<>();

        for (final File file : refFiles) {
            String fileNameWithoutInputDirectory = file.getAbsolutePath().replace(directories.get(0).getAbsolutePath(), "");

            filesToBeProcessed.add(new FileInputStream(file));
            for (int j = 1; j < directories.size(); j++) {
                File fileB = Paths.get(directories.get(j).getAbsolutePath(), fileNameWithoutInputDirectory).toFile();
                if (!fileB.exists()) {
                    LOGGER.warn("The file " + fileNameWithoutInputDirectory + " cannot be found in the " + directories.get(j).getAbsolutePath() + ". Skipping!");
                    break;
                }
                filesToBeProcessed.add(new FileInputStream(fileB));
            }

            try {
                LOGGER.info("Processing " + file.getParentFile().getName() + File.separator + file.getName() + " with corresponding annotatiions.");

                processFileForCodingStudy(filesToBeProcessed, (CodingAnnotationStudy) study);

            } catch (final Exception exp) {
                LOGGER.error("An error occured while processing the following pdf: " + file.getPath(), exp);
            }
        }
    }

    public CodingAnnotationStudy processFileForCodingStudy(List<InputStream> input, CodingAnnotationStudy study) {
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();

            List<List<String>> labelsOfLabels = input
                .stream()
                .map(inputStream -> {
                    AnnotationValuesTEIStaxHandler handler = new AnnotationValuesTEIStaxHandler();

                    try {
                        XMLStreamReader2 reader = (XMLStreamReader2) inputFactory.createXMLStreamReader(inputStream);
                        StaxUtils.traverse(reader, handler);
                        List<Pair<String, String>> labeled1 = handler.getLabeledEntities();


                        return labeled1.stream().map(Pair::getRight).collect(Collectors.toList());

                    } catch (XMLStreamException e) {
                        throw new GrobidException("Error parsing XML ", e);
                    }
                })
                .collect(Collectors.toList());

            //Iteration of different items
            for (int j = 0; j < labelsOfLabels.get(0).size(); j++) {

                Object[] array = new String[labelsOfLabels.size()];
                for (int i = 0; i < labelsOfLabels.size(); i++) {
                    array[i] = labelsOfLabels.get(i).get(j);
                }
                study.addItem(array);
            }
        } catch (Exception e) {

        }

        return study;

    }

    public ICodingAnnotationStudy extractAnnotationsCoding(List<File> directories) {
        ICodingAnnotationStudy study = null;
        try {
            if (CollectionUtils.isEmpty(directories) || directories.size() < 2) {
                throw new GrobidException("Cannot compute metrics with a single directory");
            }

            List<File> collect = directories.stream().filter(f -> !f.exists()).collect(Collectors.toList());
            if (collect.size() > 0) {
                throw new GrobidException("Input directory 1 cannot be accessed: "
                    + collect.stream().map(File::getAbsolutePath).collect(Collectors.joining(" \n")));
            }

            // I iterate list one and find (if available the same files in list 2)
            List<File> refFiles = Arrays
                .stream(Objects.requireNonNull(directories.get(0).listFiles()))
                .filter(file -> StringUtils.endsWithIgnoreCase(file.getName(), ".xml"))
                .collect(Collectors.toList());

            LOGGER.info(refFiles.size() + " files to be processed.");

            study = new CodingAnnotationStudy(directories.size());
            processFilesForCodingStudy(directories, refFiles, study);

        } catch (final Exception exp) {
            throw new GrobidException("An exception occured while running Grobid batch.", exp);
        }

        return study;
    }

}
