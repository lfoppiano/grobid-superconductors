package org.grobid.service.command;

import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.apache.commons.lang3.StringUtils;
import org.grobid.core.engines.Engine;
import org.grobid.core.engines.SuperconductorsModels;
import org.grobid.core.engines.training.SuperconductorsParserTrainingData;
import org.grobid.core.engines.training.TrainingOutputFormat;
import org.grobid.core.main.GrobidHomeFinder;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.client.ChemDataExtractorClient;
import org.grobid.core.utilities.client.StructureIdentificationModuleClient;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;

public class TrainingGenerationCommand extends ConfiguredCommand<GrobidSuperconductorsConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrainingGenerationCommand.class);
    private final static String INPUT_DIRECTORY = "Input directory";
    private final static String OUTPUT_DIRECTORY = "Output directory";
    private final static String RECURSIVE = "recursive";
    private final static String MODEL_NAME = "model";
    private final static String OUTPUT_FORMAT = "outputFormat";


    public TrainingGenerationCommand() {
        super("create-training", "Generate training data ");
    }

    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);

        subparser.addArgument("-dIn", "--input", "-i")
            .dest(INPUT_DIRECTORY)
            .type(Arguments.fileType().verifyCanRead().verifyIsDirectory())
            .required(true)
            .help("Input directory");

        subparser.addArgument("-dOut", "--output", "-o")
            .dest(OUTPUT_DIRECTORY)
            .type(Arguments.fileType()
                .verifyNotExists().verifyCanCreate()
                .or()
                .verifyIsDirectory().verifyCanWrite())
            .required(true)
            .help("Output directory");

        subparser.addArgument("-m", "--model")
            .dest(MODEL_NAME)
            .type(String.class)
            .required(true)
            .help("Model for which to create training data");

        subparser.addArgument("-f", "--format")
            .dest(OUTPUT_FORMAT)
            .type(String.class)
            .required(false)
            .setDefault("xml")
            .help("Output format (TSV, XML)");

        subparser.addArgument("-r", "--recursive")
            .dest(RECURSIVE)
            .type(Boolean.class)
            .required(false)
            .setDefault(false)
            .help("Process recursively")
            .action(Arguments.storeTrue());
    }

    @Override
    protected void run(Bootstrap bootstrap, Namespace namespace, GrobidSuperconductorsConfiguration configuration) throws Exception {
        try {
            GrobidHomeFinder grobidHomeFinder = new GrobidHomeFinder(Arrays.asList(configuration.getGrobidHome()));
            GrobidProperties.getInstance(grobidHomeFinder);
            configuration.getModels().stream().forEach(GrobidProperties::addModel);
            Engine.getEngine(true);
            LibraryLoader.load();
        } catch (final Exception exp) {
            System.err.println("Grobid initialisation failed, cannot find Grobid Home. Maybe you forget to specify the config.yml in the command launch?");
            exp.printStackTrace();

            System.exit(-1);
        }

        File inputDirectory = namespace.get(INPUT_DIRECTORY);
        File outputDirectory = namespace.get(OUTPUT_DIRECTORY);
        String modelName = namespace.get(MODEL_NAME);
        String outputFormat = namespace.get(OUTPUT_FORMAT);
        Boolean recursive = namespace.get(RECURSIVE);

        ChemDataExtractorClient chemspotClient = new ChemDataExtractorClient(configuration);
        StructureIdentificationModuleClient structureIdentificationModuleClient = new StructureIdentificationModuleClient(configuration);

        if (SuperconductorsModels.SUPERCONDUCTORS.getModelName().equals(modelName)) {
            new SuperconductorsParserTrainingData(chemspotClient, structureIdentificationModuleClient).createTrainingBatch(inputDirectory.getAbsolutePath(),
                outputDirectory.getAbsolutePath(),
                TrainingOutputFormat.valueOf(StringUtils.upperCase(outputFormat)), recursive);
        } else {
            System.out.println(super.getDescription());
        }

    }
}