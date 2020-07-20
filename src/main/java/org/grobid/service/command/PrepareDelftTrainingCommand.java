package org.grobid.service.command;

import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.grobid.core.GrobidModel;
import org.grobid.core.engines.SuperconductorsModels;
import org.grobid.core.main.GrobidHomeFinder;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;
import org.grobid.trainer.MaterialTrainer;
import org.grobid.trainer.SuperconductorsTrainer;
import org.grobid.trainer.Trainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class PrepareDelftTrainingCommand extends ConfiguredCommand<GrobidSuperconductorsConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrepareDelftTrainingCommand.class);

    private final static String MODEL_NAME = "model";
    private final static String DELFT_PATH = "delft_path";
    private final static String OUTPUT_PATH = "output_path";
    private final static String INPUT_PATH = "input_path";

    public PrepareDelftTrainingCommand() {
        super("prepare-delft-training", "Prepare training data for Delft.");
    }

    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);

        subparser.addMutuallyExclusiveGroup("output")
            .addArgument("-d", "--delft")
            .dest(DELFT_PATH)
            .type(Arguments.fileType().verifyCanRead())
            .required(false)
            .help("Location of delft (the root directory is enough. If provided a value, the data will be saved in data/sequenceLabelling/grobid/{model_name}/{model_name}-{date}.train, else will be saved as {model_name}.train in the local directory. ");

        subparser.addMutuallyExclusiveGroup("output")
            .addArgument("-o", "--output")
            .dest(OUTPUT_PATH)
            .type(Arguments.fileType().verifyNotExists().verifyCanCreate().or().verifyIsDirectory().verifyCanWrite())
            .required(false)
            .help("Output path directory. ");

        subparser.addMutuallyExclusiveGroup()
            .addArgument("-i", "--input")
            .dest(INPUT_PATH)
            .type(Arguments.fileType().verifyCanRead())
            .required(false)
            .help("Input path directory. ");

        subparser.addMutuallyExclusiveGroup()
            .addArgument("-m", "--model")
            .dest(MODEL_NAME)
            .type(String.class)
            .choices(Arrays.asList("superconductors", "material"))
            .required(false)
            .setDefault("superconductors")
            .help("Model data to use. ");

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

        File inputPath = namespace.get(INPUT_PATH);
        File delftPath = namespace.get(DELFT_PATH);
        File outputPath = namespace.get(OUTPUT_PATH);
        String modelName = namespace.get(MODEL_NAME);

        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyMMdd");

        GrobidModel model = SuperconductorsModels.SUPERCONDUCTORS;
        Trainer trainer =  new SuperconductorsTrainer();

        if (SuperconductorsModels.MATERIAL.getModelName().equals(modelName)) {
            model = SuperconductorsModels.MATERIAL;
            trainer = new MaterialTrainer();
        }

        String filename = File.separator + modelName + "-" + formatter.format(date) + ".train";

        File destination = null;
        if (outputPath != null) {
            destination = Paths.get(outputPath.getAbsolutePath(), filename).toFile();
        } else {
            destination = Paths.get(delftPath.getAbsolutePath(), "data", "sequenceLabelling",
                "grobid", modelName, filename).toFile();
        }

        if (inputPath == null) {
            inputPath = GrobidProperties.getCorpusPath(new File("/"), model);
            System.out.println("Input directory was not provided, getting the training data from " + inputPath.getAbsolutePath());
        }
        trainer.createCRFPPData(inputPath, destination);

        System.out.println("Writing training data for delft to " + destination.toString());
    }
}
