package org.grobid.service.command;

import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.dkpro.statistics.agreement.coding.FleissKappaAgreement;
import org.dkpro.statistics.agreement.coding.ICodingAnnotationStudy;
import org.dkpro.statistics.agreement.coding.KrippendorffAlphaAgreement;
import org.dkpro.statistics.agreement.coding.PercentageAgreement;
import org.dkpro.statistics.agreement.distance.NominalDistanceFunction;
import org.dkpro.statistics.agreement.visualization.ReliabilityMatrixPrinter;
import org.dkpro.statistics.agreement.visualization.UnitizingStudyPrinter;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;
import org.grobid.trainer.annotationAgreement.InterAnnotationAgreementCodingProcessor;
import org.grobid.trainer.annotationAgreement.InterAnnotationAgreementUnitizingProcessor;
import org.grobid.trainer.annotationAgreement.data.InterAnnotationAgreementPairwiseComparisonEntry;
import org.grobid.trainer.annotationAgreement.data.InterAnnotationAgreementType;
import org.grobid.trainer.annotationAgreement.data.UnitizedStudyWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InterAnnotationAgreementCommand extends ConfiguredCommand<GrobidSuperconductorsConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(InterAnnotationAgreementCommand.class);
    private final static String INPUT_DIRECTORY = "Input directory";
    private final static String OUTPUT_DIRECTORY = "Output directory";
    private final static String MODE = "Method of calculation";
    public static final List<String> TOP_LEVEL_ANNOTATION_DEFAULT_TAGS = Arrays.asList("p");
    public static final List<String> ANNOTATION_DEFAULT_TAGS = Arrays.asList("supercon", "tc", "substitution", "propertyValue");


    public InterAnnotationAgreementCommand() {
        super("iaa", "Inter annotation agreement measures ");
    }

    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);

        subparser.addArgument("-dIn")
                .dest(INPUT_DIRECTORY)
                .type(String.class)
                .required(true)
                .help("Input directory");

        subparser.addArgument("-m")
                .dest(MODE)
                .type(String.class)
                .choices(InterAnnotationAgreementType.CODING, InterAnnotationAgreementType.UNITIZING)
                .setDefault(InterAnnotationAgreementType.UNITIZING)
                .required(false)
                .help("Method of calculation.");
    }

    @Override
    protected void run(Bootstrap bootstrap, Namespace namespace, GrobidSuperconductorsConfiguration configuration) throws Exception {
        String inputDirectory = namespace.get(INPUT_DIRECTORY);
        File[] directories = new File(inputDirectory).listFiles(File::isDirectory);

        InterAnnotationAgreementType mode = namespace.get(MODE);

        System.out.println("Calculating IAA between the following directories: " +
                Arrays.stream(directories).map(f -> f.getAbsolutePath()).collect(Collectors.joining(", \n")));

        if (mode.equals(InterAnnotationAgreementType.CODING)) {

            ICodingAnnotationStudy study = new InterAnnotationAgreementCodingProcessor().extractAnnotationsCoding(Arrays.asList(directories));

            FleissKappaAgreement kappa = new FleissKappaAgreement(study);
            System.out.println("Fleiss k: " + kappa.calculateAgreement());
            System.out.println("Fleiss k expected agreement: " + kappa.calculateExpectedAgreement());

            PercentageAgreement pa = new PercentageAgreement(study);
            System.out.println("Percentage agreement k: " + pa.calculateAgreement());

            KrippendorffAlphaAgreement alpha = new KrippendorffAlphaAgreement(study, new NominalDistanceFunction());
            System.out.println("=== Krippendorf alpha agreements === ");
            System.out.println("Observer disagreement: " + alpha.calculateObservedDisagreement());
            System.out.println("Expected disagreement: " + alpha.calculateExpectedDisagreement());
            System.out.println("Calculated agreement: " + alpha.calculateAgreement());

            System.out.println("Reliability matrix");
            new ReliabilityMatrixPrinter().print(System.out, study);
        } else {
            InterAnnotationAgreementUnitizingProcessor iiaProcessor
                    = new InterAnnotationAgreementUnitizingProcessor(TOP_LEVEL_ANNOTATION_DEFAULT_TAGS,
                    ANNOTATION_DEFAULT_TAGS);

            List<UnitizedStudyWrapper> studies = iiaProcessor.extractAnnotations(Arrays.asList(directories));

            // General evaluation
            System.out.println("== General evaluation (considering all the annotators) ==");
            double averageAlpha = studies.stream()
                    .mapToDouble(UnitizedStudyWrapper::getAgreement).summaryStatistics().getAverage();
            System.out.println("Krippendorf alpha agreements: " + averageAlpha);

            // Evaluation by category
            System.out.println("Krippendorf alpha agreement by category: ");

            Map<String, Double> averageByCategory = studies.stream()
                    .map(UnitizedStudyWrapper::getAgreementByCategory)
                    .flatMap(m -> m.entrySet().stream())
                    .collect(Collectors.groupingBy(Map.Entry::getKey,
                            Collectors.averagingDouble(Map.Entry::getValue)));

            averageByCategory.forEach((c, d) -> {
                System.out.println(c + ": " + d);
            });

            // Pairwise evaluation
            Map<Pair<Integer, Integer>, Double> pairwiseAverage = studies
                    .stream()
                    .map(UnitizedStudyWrapper::getPairwiseRaterAgreementMatrices)
                    .flatMap(Collection::stream)
                    .collect(Collectors.groupingBy(a -> ImmutablePair.of(a.getRater0(), a.getRater1()), Collectors.averagingDouble(InterAnnotationAgreementPairwiseComparisonEntry::getAgreementAverage)));

            // Pairwise matrix
            Map<Pair<Integer, Integer>, List<InterAnnotationAgreementPairwiseComparisonEntry>> collect = studies
                    .stream()
                    .map(UnitizedStudyWrapper::getPairwiseRaterAgreementMatrices)
                    .flatMap(Collection::stream)
                    .collect(Collectors.groupingBy(a -> ImmutablePair.of(a.getRater0(), a.getRater1()), Collectors.toList()));

            collect.forEach((k, v) -> {
                System.out.println();

                int column = k.getRight();
                int row = k.getLeft();

                System.out.println(row + " vs " + column);
                System.out.println("General Agreement: " + pairwiseAverage.get(k));

                System.out.println("Agreement by categories: ");
                Map<String, Double> collect1 = v
                        .stream()
                        .map(InterAnnotationAgreementPairwiseComparisonEntry::getAgreementByCategory)
                        .flatMap(m -> m.entrySet().stream())
                        .collect(Collectors.groupingBy(Map.Entry::getKey,
                                Collectors.averagingDouble(Map.Entry::getValue)));

                collect1.keySet().forEach(a -> {
                    System.out.println(a + ": " + collect1.get(a));
                });


            });

            // Debug information
            System.out.println();
            System.out.println();

            UnitizingStudyPrinter printer = new UnitizingStudyPrinter();
            studies.forEach(s -> {
                System.out.println("\t\t\t\t" + s.getContinuums().get(0));
                s.getStudy().getCategories().forEach(c -> {
                    printer.printUnitsForCategory(System.out, s.getStudy(), c, String.format("%1$" + 12 + "s", c.toString()));
                });
            });
        }

    }


    public long calculateTotalCombination(int n) {
        long sum = 0;
        for (int i = n - 1; i > 0; i--) {
            sum += i;
        }
        return sum;
    }
}