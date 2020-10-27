package org.grobid.service.command;

import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
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
import org.grobid.trainer.stax.StackTags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isEmpty;

public class InterAnnotationAgreementCommand extends ConfiguredCommand<GrobidSuperconductorsConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(InterAnnotationAgreementCommand.class);
    private final static String INPUT_DIRECTORY = "Input directory";
    private final static String OUTPUT_DIRECTORY = "Output directory";
    private final static String VERBOSE_OUTPUT = "verbose output";
    private final static String ONE_VS_ALL = "one-vs-all";
    private final static String MODE = "Method of calculation";

    public static final List<StackTags> TOP_LEVEL_ANNOTATION_DEFAULT_PATHS = Arrays.asList(
        StackTags.from("/tei/teiHeader/fileDesc/titleStmt/title"),
        StackTags.from("/tei/teiHeader/profileDesc/abstract/p"),
        StackTags.from("/tei/teiHeader/profileDesc/ab"),
        StackTags.from("/tei/text/body/p"),
        StackTags.from("/tei/text/body/ab")
    );
    public static final List<String> ANNOTATION_DEFAULT_TAG_TYPES = Arrays.asList("material", "tc",
        "tcValue", "pressure", "me_method", "class");

    public static final List<String> TOP_LEVEL_ANNOTATION_DEFAULT_TAGS = Arrays.asList("p");
    public static final List<String> ANNOTATION_DEFAULT_TAGS = Arrays.asList("material", "tc",
        "tcValue", "pressure", "me_method", "class");
    public static final List<String> ANNOTATION_EXTRA_TAGS = Arrays.asList("sample", "magnetisation", "shape");


    public InterAnnotationAgreementCommand() {
        super("iaa", "Inter annotation agreement measures ");
    }

    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);

        subparser.addArgument("--input", "-i")
            .dest(INPUT_DIRECTORY)
            .type(Arguments.fileType().verifyCanRead().verifyIsDirectory())
            .required(true)
            .help("Input directory");

        subparser.addArgument("--output", "-o")
            .dest(OUTPUT_DIRECTORY)
            .type(Arguments.fileType().verifyCanWrite().verifyIsDirectory())
            .required(false)
            .help("Output directory. If not specified, the output is printed. ");

        //https://github.com/dropwizard/dropwizard/issues/2814#issue-460688964
        subparser.addArgument("--verbose", "-v")
            .dest(VERBOSE_OUTPUT)
            .type(Boolean.class)
            .required(false)
            .setDefault(false)
            .action(Arguments.storeTrue())
            .help("Output the detailed comparison. ");

        subparser.addArgument("-m")
            .dest(MODE)
            .type(String.class)
            .choices(InterAnnotationAgreementType.CODING, InterAnnotationAgreementType.UNITIZING)
            .setDefault(InterAnnotationAgreementType.UNITIZING)
            .required(false)
            .help("Method of calculation.");

        subparser.addArgument("--one-vs-all")
            .dest(ONE_VS_ALL)
            .type(String.class)
            .required(false)
            .help("Define the subdirectory to be evaluated as curators annotations. This single subdirectory will compared with all the others in order to have a comparision 1-vs-all");
    }

    @Override
    protected void run(Bootstrap bootstrap, Namespace namespace, GrobidSuperconductorsConfiguration configuration) throws Exception {
        File inputDirectory = namespace.get(INPUT_DIRECTORY);
        File[] directories = inputDirectory.listFiles(File::isDirectory);

        InterAnnotationAgreementType mode = namespace.get(MODE);

        Boolean isVerbose = namespace.get(VERBOSE_OUTPUT);

        System.out.println("Calculating IAA between the following directories: \n" +
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
                = new InterAnnotationAgreementUnitizingProcessor(TOP_LEVEL_ANNOTATION_DEFAULT_PATHS,
                ANNOTATION_DEFAULT_TAG_TYPES);

            String pivotDirectory = namespace.get(ONE_VS_ALL);

            if (isEmpty(pivotDirectory)) {
                List<UnitizedStudyWrapper> studies = iiaProcessor.extractAnnotations(Arrays.asList(directories));
                printEvaluation(studies);

                if (directories.length > 2) {
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
                        System.out.println("");

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
                        System.out.println("");

                    });
                } else {
                    System.out.println("IAA ran only on two annotators. Ignoring the pairwise comparison. ");
                }

                // Debug information
                if (isVerbose.equals(true)) {
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
            } else {
                Optional<File> first = Arrays.stream(directories)
                    .filter(f -> f.getName().equals(pivotDirectory))
                    .findFirst();

                File pivot = first.orElseThrow(() -> new RuntimeException("Cannot find the pivot directory " + pivotDirectory));
                List<File> others = Arrays.stream(directories)
                    .filter(f -> !f.getName().equals(pivotDirectory))
                    .collect(Collectors.toList());

                for (File dir : others) {
                    List<UnitizedStudyWrapper> studies = iiaProcessor.extractAnnotations(Arrays.asList(dir, pivot));
                    System.out.println("Evaluating " + dir.getName() + " vs " + pivot.getName());
                    printEvaluation(studies);

                    // Debug information
                    if (isVerbose.equals(true)) {
                        System.out.println();
                        System.out.println();

                        UnitizingStudyPrinter printer = new UnitizingStudyPrinter();
                        studies.forEach(s -> {
//                            printer.printContinuum(System.out, s.getStudy(), "");
                            System.out.println("\t\t\t\t" + s.getContinuums().get(0));
                            s.getStudy().getCategories().forEach(c -> {
                                printer.printUnitsForCategory(System.out, s.getStudy(), c, String.format("%1$" + 12 + "s", c.toString()));
                            });
                        });
                    }
                }
            }
        }

    }

    private void printEvaluation(List<UnitizedStudyWrapper> studies) {
        // General evaluation
        System.out.println("== General evaluation (considering all the annotators) ==");
        double averageAlpha = studies.stream()
            .mapToDouble(UnitizedStudyWrapper::getAgreement).summaryStatistics().getAverage();
        System.out.println("Krippendorf alpha agreements: " + averageAlpha);
        System.out.println(" ");

        // Evaluation by category
        System.out.println("Krippendorf alpha agreement by category: ");

        Map<String, Double> averageByCategory = studies.stream()
            .map(UnitizedStudyWrapper::getAgreementByCategory)
            .flatMap(m -> m.entrySet().stream())
            .collect(Collectors.groupingBy(Map.Entry::getKey,
                Collectors.averagingDouble(Map.Entry::getValue)));

        averageByCategory.forEach((c, d) -> {
            System.out.println(c + ": \t" + d);
        });
        System.out.println(" ");
    }


    public long calculateTotalCombination(int n) {
        long sum = 0;
        for (int i = n - 1; i > 0; i--) {
            sum += i;
        }
        return sum;
    }
}