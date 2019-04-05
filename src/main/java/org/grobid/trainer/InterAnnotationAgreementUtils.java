package org.grobid.trainer;

import com.ctc.wstx.stax.WstxInputFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.codehaus.stax2.XMLStreamReader2;
import org.dkpro.statistics.agreement.coding.CodingAnnotationStudy;
import org.dkpro.statistics.agreement.coding.ICodingAnnotationStudy;
import org.dkpro.statistics.agreement.unitizing.IUnitizingAnnotationStudy;
import org.dkpro.statistics.agreement.unitizing.UnitizingAnnotationStudy;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.trainer.sax.SuperconductorsAnnotationSaxHandler;
import org.grobid.trainer.stax.AnnotationExtractionStaxHandler;
import org.grobid.trainer.stax.StaxUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class InterAnnotationAgreementUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(InterAnnotationAgreementUtils.class);

    private WstxInputFactory inputFactory = new WstxInputFactory();

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
                        SuperconductorsAnnotationSaxHandler handler = new SuperconductorsAnnotationSaxHandler();

                        try {
                            SAXParser p = spf.newSAXParser();
                            p.parse(inputStream, handler);

                            List<Pair<String, String>> labeled1 = handler.getLabeledResult();


                            return labeled1.stream().map(Pair::getRight).collect(Collectors.toList());

                        } catch (ParserConfigurationException | IOException | SAXException e) {
                            throw new GrobidException("Error parsing XML ", e);
                        }
                    })
                    .collect(Collectors.toList());

            //Iteration of different items
            for (int j = 0; j < labelsOfLabels.get(0).size(); j++) {

                String[] array = new String[labelsOfLabels.size()];
                for (int i = 0; i < labelsOfLabels.size(); i++) {
                    array[i] = labelsOfLabels.get(i).get(j);
                }
                study.addItem(array);
            }
        } catch (Exception e) {

        }

        return study;

    }

    private List<IUnitizingAnnotationStudy> processFilesForUnitizingStudy(List<File> directories, List<File> refFiles) throws FileNotFoundException {
        List<Pair<File, Integer>> filesToBeProcessed = new ArrayList<>();

        List<IUnitizingAnnotationStudy> studies = new ArrayList<>();

        int rater = 0;
        for (final File file : refFiles) {
            String fileNameWithoutInputDirectory = file.getAbsolutePath().replace(directories.get(0).getAbsolutePath(), "");

            filesToBeProcessed.add(ImmutablePair.of(file, rater));
            rater++;
            for (int j = 1; j < directories.size(); j++) {
                File fileB = Paths.get(directories.get(j).getAbsolutePath(), fileNameWithoutInputDirectory).toFile();
                if (!fileB.exists()) {
                    LOGGER.warn("The file " + fileNameWithoutInputDirectory + " cannot be found in the " + directories.get(j).getAbsolutePath() + ". Skipping!");
                    break;
                }
                filesToBeProcessed.add(ImmutablePair.of(fileB, rater));
                rater++;
            }

            try {
                LOGGER.info("Start processing for " + file.getName() + ".");

                studies.add(processFileForUnitizingStudy(filesToBeProcessed, directories.size()));
                filesToBeProcessed = new ArrayList<>();
                rater = 0;

            } catch (final Exception exp) {
                LOGGER.error("An error occured while processing the following file: " + file.getPath(), exp);
            }
        }

        return studies;
    }

    public UnitizingAnnotationStudy processFileForUnitizingStudy(List<Pair<File, Integer>> input, int raters) {
        try {
            UnitizingAnnotationStudy study = null;
            String previousContinuoum = "";

            for (Pair<File, Integer> rating : input) {
                AnnotationExtractionStaxHandler handler = new AnnotationExtractionStaxHandler();

                try {
                    XMLStreamReader2 reader = inputFactory.createXMLStreamReader(rating.getKey());
                    StaxUtils.traverse(reader, handler);

                    int length1 = handler.getContinuoum().length();

                    if (study == null) {
                        study = new UnitizingAnnotationStudy(raters, length1);
                        LOGGER.info("Building study with " + raters + " raters and continuoum: " + length1);
                    } else {
                        //check
                        if (length1 != study.getContinuumLength()) {
                            LOGGER.warn("There is a mismatch between two continuum lengths referring to the same document.");
                            LOGGER.warn("Rater: " + rating.getValue() + " -> " + length1);
                            LOGGER.warn(previousContinuoum);
                            LOGGER.warn(handler.getContinuoum());
                        }

                    }
                    LOGGER.info("Processing rater: " + rating.getValue() + ", file: " + rating.getKey());

                    for (Triple<String, Integer, Integer> annotation : handler.getData()) {
                        String annotationName = annotation.getLeft();
                        Integer start = annotation.getMiddle();
                        Integer length = annotation.getRight();

                        study.addUnit(start, length, rating.getValue(), annotationName);
                    }

                    previousContinuoum = handler.getContinuoum();

                } catch (XMLStreamException e) {
                    throw new GrobidException("Error parsing XML ", e);
                }
            }

            return study;


        } catch (Exception e) {
            throw new GrobidException("Somethings' up with this", e);
        }

    }

    public List<IUnitizingAnnotationStudy> extractAnnotationsUnitizing(List<File> directories) {
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

            return processFilesForUnitizingStudy(directories, refFiles);

        } catch (final Exception exp) {
            throw new GrobidException("An exception occured while running Grobid batch.", exp);
        }
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

    public enum Type {
        CODING, UNITIZING
    }
}
