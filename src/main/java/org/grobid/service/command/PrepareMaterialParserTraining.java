package org.grobid.service.command;

import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.grobid.core.engines.MaterialParser;
import org.grobid.core.engines.SuperconductorsModels;
import org.grobid.core.main.GrobidHomeFinder;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;
import org.grobid.trainer.MaterialTrainer;
import org.grobid.trainer.SuperconductorsTrainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static shadedwipo.org.apache.commons.lang3.StringUtils.isNotEmpty;

public class PrepareMaterialParserTraining extends ConfiguredCommand<GrobidSuperconductorsConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrepareMaterialParserTraining.class);

    private final static String OUTPUT_PATH = "output_path";
    private final static String INPUT_PATH = "input_path";

    public PrepareMaterialParserTraining() {
        super("prepare-material-training", "Prepare the training data for the Material Parser using already annotated XML files. ");
    }

    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);

        subparser.addArgument("-o", "--output")
            .dest(OUTPUT_PATH)
            .type(String.class)
            .required(true)
            .help("Output path directory. ");

        subparser.addArgument("-i", "--input")
            .dest(INPUT_PATH)
            .type(String.class)
            .required(true)
            .help("Input path directory. ");

    }

    @Override
    protected void run(Bootstrap<GrobidSuperconductorsConfiguration> bootstrap, Namespace namespace, GrobidSuperconductorsConfiguration configuration) throws Exception {
        try {
            GrobidProperties.set_GROBID_HOME_PATH(new File(configuration.getGrobidHome()).getAbsolutePath());
            String grobidHome = configuration.getGrobidHome();
            if (grobidHome != null) {
                GrobidProperties.setGrobidPropertiesPath(new File(grobidHome, "/config/grobid.properties").getAbsolutePath());
            }

            GrobidHomeFinder grobidHomeFinder = new GrobidHomeFinder(Collections.singletonList(configuration.getGrobidHome()));
            GrobidProperties.getInstance(grobidHomeFinder);
            LibraryLoader.load();
        } catch (final Exception exp) {
            System.err.println("Grobid initialisation failed, cannot find Grobid Home. Maybe you forget to specify the config.yml in the command launch?");
            exp.printStackTrace();

            System.exit(-1);
        }

        String inputPath = namespace.get(INPUT_PATH);
        String outputPath = namespace.get(OUTPUT_PATH);

        File input = GrobidProperties.getCorpusPath(new File("/"), SuperconductorsModels.SUPERCONDUCTORS);
        if (isNotEmpty(inputPath)) {
            input = Paths.get(inputPath).toFile();
        }

        Path sourcePath = Paths.get(input.getAbsolutePath());

        Path destinationPath = Paths.get(outputPath);
        if (!Files.exists(destinationPath)) {
            Files.createDirectories(destinationPath);
        }

        new MaterialTrainer().createTrainingDataFromSuperconductors(sourcePath.toString(), destinationPath.toString(), true);
    }
}
