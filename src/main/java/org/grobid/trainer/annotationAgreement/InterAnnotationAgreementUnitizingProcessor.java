package org.grobid.trainer.annotationAgreement;

import com.ctc.wstx.stax.WstxInputFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.trainer.annotationAgreement.data.UnitizedStudyWrapper;
import org.grobid.trainer.stax.SuperconductorsStackTags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class InterAnnotationAgreementUnitizingProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(InterAnnotationAgreementUnitizingProcessor.class);

    private List<String> annotationTags = new ArrayList<>();
    private List<SuperconductorsStackTags> topLevelTags = new ArrayList<>();

    private WstxInputFactory inputFactory = new WstxInputFactory();

    public InterAnnotationAgreementUnitizingProcessor() {

    }

    public InterAnnotationAgreementUnitizingProcessor(List<SuperconductorsStackTags> topLevelTags, List<String> annotationTags) {
        this.topLevelTags = topLevelTags;
        this.annotationTags = annotationTags;
    }

    /**
     * Taking in input directories (containing files from each annotators) and the reference files (the list of files
     * per annotator) compute the annotation studies.
     */
    private List<UnitizedStudyWrapper> processFiles(List<File> directories, List<File> refFiles) {

        List<UnitizedStudyWrapper> studies = new ArrayList<>();

        LOGGER.info("Processing: ");
        for (final File file : refFiles) {
            String fileNameWithoutInputDirectory = file.getAbsolutePath().replace(directories.get(0).getAbsolutePath(), "");

            LOGGER.info(" > " + file.getAbsolutePath());
            List<InputStream> files = new ArrayList<>();
            String absolutePath = "";//For debugging in case of exception
            try {
                files.add(new FileInputStream(file));
                for (int j = 1; j < directories.size(); j++) {
                    absolutePath = directories.get(j).getAbsolutePath();
                    File fileB = Paths.get(absolutePath, fileNameWithoutInputDirectory).toFile();
                    files.add(new FileInputStream(fileB));
                }
            } catch (FileNotFoundException e) {
                LOGGER.warn("The file " + fileNameWithoutInputDirectory + " cannot be found in the " + absolutePath + ". Skipping!");
                continue;
            }
            
            UnitizedStudyWrapper wrappedStudy = new UnitizedStudyWrapper(files, this.topLevelTags, this.annotationTags);
            studies.add(wrappedStudy);
        }

        return studies;
    }


    /**
     * Extract annotation given the top level directories which separate each rater files
     */
    public List<UnitizedStudyWrapper> extractAnnotations(List<File> directories) {

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

            return processFiles(directories, refFiles);

        } catch (final Exception exp) {
            throw new GrobidException("An exception occurred while running Grobid batch.", exp);
        }
    }
}
