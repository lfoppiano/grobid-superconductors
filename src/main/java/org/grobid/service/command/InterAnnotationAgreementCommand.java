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
import org.grobid.trainer.stax.SuperconductorsStackTags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
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

    public static final List<SuperconductorsStackTags> TOP_LEVEL_ANNOTATION_DEFAULT_PATHS = Arrays.asList(
        SuperconductorsStackTags.from("/tei/teiHeader/fileDesc/titleStmt/title"),
        SuperconductorsStackTags.from("/tei/teiHeader/profileDesc/abstract/p/s"),
        SuperconductorsStackTags.from("/tei/teiHeader/profileDesc/ab/s"),
        SuperconductorsStackTags.from("/tei/text/body/p/s"),
        SuperconductorsStackTags.from("/tei/text/p/s"),
        SuperconductorsStackTags.from("/tei/text/body/ab/s")
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

        File outputDirectory = namespace.get(OUTPUT_DIRECTORY);

        InterAnnotationAgreementType mode = namespace.get(MODE);

        Boolean isVerbose = namespace.get(VERBOSE_OUTPUT);

        final PrintStream printStream;

        if (outputDirectory == null) {
            printStream = System.out;
        } else {
            Date date = new Date();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");

            String outputFileName = "iaa-result-" + formatter.format(date);
            printStream = new PrintStream(Paths.get(outputDirectory.getAbsolutePath(), outputFileName).toFile());
        }


        printStream.println("Calculating IAA between the following directories: \n" +
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
                printEvaluation(studies, printStream);

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
                        printStream.println();

                        int column = k.getRight();
                        int row = k.getLeft();

                        printStream.println(row + " vs " + column);
                        printStream.println("General Agreement: " + pairwiseAverage.get(k));
                        printStream.println("");

                        printStream.println("Agreement by categories: ");
                        Map<String, Double> collect1 = v
                            .stream()
                            .map(InterAnnotationAgreementPairwiseComparisonEntry::getAgreementByCategory)
                            .flatMap(m -> m.entrySet().stream())
                            .collect(Collectors.groupingBy(Map.Entry::getKey,
                                Collectors.averagingDouble(Map.Entry::getValue)));

                        collect1.keySet().forEach(a -> {
                            printStream.println(a + ": " + collect1.get(a));
                        });
                        printStream.println("");

                    });
                } else {
                    printStream.println("IAA ran only on two annotators. Ignoring the pairwise comparison. ");
                }

                // Debug information
                if (isVerbose.equals(true)) {
                    printStream.println();
                    printStream.println();

                    UnitizingStudyPrinter printer = new UnitizingStudyPrinter();
                    studies.forEach(s -> {
                        printStream.println("\t\t\t\t" + s.getContinuums().get(0));
                        s.getStudy().getCategories().forEach(c -> {
                            printer.printUnitsForCategory(printStream, s.getStudy(), c, String.format("%1$" + 12 + "s", c.toString()));
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
                    printStream.println("Evaluating " + dir.getName() + " vs " + pivot.getName());
                    printEvaluation(studies, printStream);

                    // Debug information
                    if (isVerbose.equals(true)) {
                        printStream.println();
                        printStream.println();

                        Map<String, List<String>> mismatchesByCategory = new HashMap<>();
                        Map<String, Long> correctByCategory = new HashMap<>();

                        UnitizingStudyPrinter printer = new UnitizingStudyPrinter();
                        studies.forEach(s -> {
//                            printer.printContinuum(printStream, s.getStudy(), "");
                            printStream.println("\t\t\t\t" + s.getContinuums().get(0));
                            s.getStudy().getCategories().forEach(c -> {
                                printer.printUnitsForCategory(printStream, s.getStudy(), c, String.format("%1$" + 12 + "s", c.toString()));
                            });

                            printStream.println();
                            printStream.println();

                            for (Object category : s.getStudy().getCategories()) {
                                String categoryAsString = String.valueOf(category);
                                mismatchesByCategory.putIfAbsent(categoryAsString, new ArrayList<>());
                                correctByCategory.putIfAbsent(categoryAsString, 0L);
                                List<String> mismatches = s.getStudy().getUnits().stream()
                                    .filter(s_ -> s_.getCategory().equals(category))
                                    .collect(Collectors.groupingBy(b -> b.getLength() + '-' + b.getOffset()))
                                    .entrySet().stream().filter(e -> e.getValue().size() == 1)
                                    .flatMap(e -> e.getValue().stream())
                                    .map(s_ -> s_.getRaterIdx() + "-(" + s_.getOffset() + "-" + s_.getLength() + ") '" + s.getContinuums().get(0).substring((int) s_.getOffset(), (int) s_.getEndOffset()) + "'")
                                    .collect(Collectors.toList());
                                mismatchesByCategory.get(categoryAsString).addAll(mismatches);

                                Long correct = s.getStudy().getUnits().stream()
                                    .filter(s_ -> s_.getCategory().equals(category))
                                    .collect(Collectors.groupingBy(b -> b.getLength() + '-' + b.getOffset()))
                                    .entrySet().stream().filter(e -> e.getValue().size() == 2)
                                    .flatMap(e -> e.getValue().stream()).count();
                                correctByCategory.put(categoryAsString, correctByCategory.get(categoryAsString) + correct);
                            }
                        });

                        for (String category : mismatchesByCategory.keySet()) {
                            printStream.println("Mismatches in " + category + " (" + mismatchesByCategory.get(category).size() + ", correct: " + correctByCategory.get(category) + ")");
                            printStream.println(mismatchesByCategory.get(category));
                            printStream.println();
                        }
                    }
                }
            }
        }

    }

    private void printEvaluation(List<UnitizedStudyWrapper> studies, PrintStream printStream) {
        // General evaluation
        printStream.println("== General evaluation (considering all the annotators) ==");
        double averageAlpha = studies.stream()
            .mapToDouble(UnitizedStudyWrapper::getAgreement).summaryStatistics().getAverage();
        printStream.println("Krippendorf alpha agreements: " + averageAlpha);
        printStream.println(" ");

        // Evaluation by category
        printStream.println("Krippendorf alpha agreement by category: ");

        Map<String, Double> averageByCategory = studies.stream()
            .map(UnitizedStudyWrapper::getAgreementByCategory)
            .flatMap(m -> m.entrySet().stream())
            .collect(Collectors.groupingBy(Map.Entry::getKey,
                Collectors.averagingDouble(Map.Entry::getValue)));

        averageByCategory.forEach((c, d) -> {
            printStream.println(c + ": \t" + d);
        });
        printStream.println(" ");
    }


    public long calculateTotalCombination(int n) {
        long sum = 0;
        for (int i = n - 1; i > 0; i--) {
            sum += i;
        }
        return sum;
    }
}