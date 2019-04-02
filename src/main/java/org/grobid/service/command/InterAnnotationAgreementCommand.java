package org.grobid.service.command;

import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.dkpro.statistics.agreement.coding.FleissKappaAgreement;
import org.dkpro.statistics.agreement.coding.ICodingAnnotationStudy;
import org.dkpro.statistics.agreement.coding.KrippendorffAlphaAgreement;
import org.dkpro.statistics.agreement.coding.PercentageAgreement;
import org.dkpro.statistics.agreement.distance.NominalDistanceFunction;
import org.dkpro.statistics.agreement.unitizing.IUnitizingAnnotationStudy;
import org.dkpro.statistics.agreement.unitizing.KrippendorffAlphaUnitizingAgreement;
import org.dkpro.statistics.agreement.visualization.ReliabilityMatrixPrinter;
import org.grobid.trainer.InterAnnotationAgreementUtils;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class InterAnnotationAgreementCommand extends ConfiguredCommand<GrobidSuperconductorsConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(InterAnnotationAgreementCommand.class);
    private final static String INPUT_DIRECTORY = "Input directory";
    private final static String OUTPUT_DIRECTORY = "Output directory";
    private final static String MODE = "Method of calculation";


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
                .choices(InterAnnotationAgreementUtils.Type.CODING, InterAnnotationAgreementUtils.Type.UNITIZING)
                .setDefault(InterAnnotationAgreementUtils.Type.UNITIZING)
                .required(false)
                .help("Method of calculation.");
    }

    @Override
    protected void run(Bootstrap bootstrap, Namespace namespace, GrobidSuperconductorsConfiguration configuration) throws Exception {
        String inputDirectory = namespace.get(INPUT_DIRECTORY);
        File[] directories = new File(inputDirectory).listFiles(File::isDirectory);

        InterAnnotationAgreementUtils interAnnotationAgreementUtils = new InterAnnotationAgreementUtils();

        InterAnnotationAgreementUtils.Type mode = namespace.get(MODE);

        System.out.println("Calculating IAA between the following directories: " +
                Arrays.stream(directories).map(f -> f.getAbsolutePath()).collect(Collectors.joining(", \n")));

        if (mode.equals(InterAnnotationAgreementUtils.Type.CODING)) {

            ICodingAnnotationStudy study = interAnnotationAgreementUtils.extractAnnotationsCoding(Arrays.asList(directories));

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
            List<IUnitizingAnnotationStudy> studies = interAnnotationAgreementUtils.extractAnnotationsUnitizing(Arrays.asList(directories));

            double[] agreements = new double[studies.size()];
            Map<Object, List<Double>> agreementsByCategory = new HashMap<>();

            int i = 0;
            for (IUnitizingAnnotationStudy study : studies) {

                if (i == 0) {
                    study.getCategories().forEach(c -> agreementsByCategory.put(c, new ArrayList<>()));
                }

                KrippendorffAlphaUnitizingAgreement krippendorffAlphaUnitizingAgreement = new KrippendorffAlphaUnitizingAgreement(study);
                agreements[i] = krippendorffAlphaUnitizingAgreement.calculateAgreement();

                study.getCategories().forEach(c -> {
                    agreementsByCategory.get(c).add(krippendorffAlphaUnitizingAgreement.calculateCategoryAgreement(c));
                });
                i++;
            }
            Mean mean = new Mean();

            System.out.println("Krippendorf alpha agreements: " + mean.evaluate(agreements));
            System.out.println("Krippendorf alpha agreement by category: ");

            agreementsByCategory.forEach((c, doubles) -> {
                double[] measures = new double[doubles.size()];
                for (int b = 0; b < doubles.size(); b++) {
                    measures[b] = doubles.get(b);
                }

                System.out.println(c.toString() + ": " + mean.evaluate(measures));
            });

        }

    }
}