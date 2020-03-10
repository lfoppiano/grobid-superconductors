package org.grobid.service.command;

import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.grobid.core.engines.SuperconductorsModels;
import org.grobid.core.main.GrobidHomeFinder;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;
import org.grobid.trainer.SuperconductorsTrainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import static shadedwipo.org.apache.commons.lang3.StringUtils.isNotEmpty;

public class PrepareDelftTraining extends ConfiguredCommand<GrobidSuperconductorsConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrepareDelftTraining.class);

    private final static String MODEL_NAME = "model";
    private final static String DELFT_PATH = "delft_path";
    private final static String OUTPUT_PATH = "output_path";
    private final static String INPUT_PATH = "input_path";

    public PrepareDelftTraining() {
        super("prepare-delft-training", "Prepare training data for Delft.");
    }

    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);

        subparser.addArgument("-d", "--delft")
            .dest(DELFT_PATH)
            .type(String.class)
            .required(false)
            .help("Location of delft (the root directory is enough. If provided a value, the data will be saved in data/sequenceLabelling/grobid/{model_name}/{model_name}-{date}.train, else will be saved as {model_name}.train in the local directory. ");

        subparser.addArgument("-o", "--output")
            .dest(OUTPUT_PATH)
            .type(String.class)
            .required(false)
            .help("Output path directory. ");

//        subparser.addArgument("-i", "--input")
//            .dest(INPUT_PATH)
//            .type(String.class)
//            .required(false)
//            .setDefault("superconductors")
//            .help("Name of the model for which generate the training data. ");

    }

    @Override
    protected void run(Bootstrap<GrobidSuperconductorsConfiguration> bootstrap, Namespace namespace, GrobidSuperconductorsConfiguration configuration) throws Exception {
        try {
            GrobidProperties.set_GROBID_HOME_PATH(new File(configuration.getGrobidHome()).getAbsolutePath());
            String grobidHome = configuration.getGrobidHome();
            if (grobidHome != null) {
                GrobidProperties.setGrobidPropertiesPath(new File(grobidHome, "/config/grobid.properties").getAbsolutePath());
            }

            GrobidHomeFinder grobidHomeFinder = new GrobidHomeFinder(Arrays.asList(configuration.getGrobidHome()));
            GrobidProperties.getInstance(grobidHomeFinder);
            LibraryLoader.load();
        } catch (final Exception exp) {
            System.err.println("Grobid initialisation failed, cannot find Grobid Home. Maybe you forget to specify the config.yml in the command launch?");
            exp.printStackTrace();

            System.exit(-1);
        }

//        String modelName = namespace.get(MODEL_NAME);
        String delftPath = namespace.get(DELFT_PATH);
        String outputPath = namespace.get(OUTPUT_PATH);

        if (delftPath == null && outputPath == null) {
            System.out.println("Both Delft path (--delft/-d option) and output path (--output/-o) are empty. Aborting. ");
            System.exit(-1);
        }

        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyMMdd");

        String destination = "";
        if (isNotEmpty(outputPath)) {
            destination = outputPath;
        } else if (isNotEmpty(delftPath)) {
            destination = delftPath + File.separator + "data" + File.separator + "sequenceLabelling"
                + File.separator + "grobid" + File.separator
                + SuperconductorsModels.SUPERCONDUCTORS.getModelName();
        } else {
            System.out.println("Both Delft path (--delft/-d option) and output path (--output/-o) are are selected." +
                "Please select only one of them. ");
            System.exit(-1);
        }

        Path destinationPath = Paths.get(destination);
        if (!Files.exists(destinationPath)) {
            Files.createDirectories(destinationPath);
        }

        String filename = File.separator + SuperconductorsModels.SUPERCONDUCTORS.getModelName()
            + "-" + formatter.format(date) + ".train";

        destinationPath = Paths.get(destination + filename);

        SuperconductorsTrainer trainer = new SuperconductorsTrainer();
        trainer.createCRFPPData(
            GrobidProperties.getCorpusPath(new File("/"), SuperconductorsModels.SUPERCONDUCTORS),
            destinationPath.toFile());

        System.out.println("Writing training data for delft to " + destinationPath.toString());
    }
}
