package org.grobid.service.command;

import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.grobid.core.engines.SuperconductorsModels;
import org.grobid.core.engines.training.MaterialParserTrainingData;
import org.grobid.core.main.GrobidHomeFinder;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;
import org.grobid.trainer.MaterialTrainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;

public class PrepareMaterialParserTrainingCommand extends ConfiguredCommand<GrobidSuperconductorsConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrepareMaterialParserTrainingCommand.class);

    private final static String OUTPUT_PATH = "output_path";
    private final static String INPUT_PATH = "input_path";
    private final static String INPUT_FORMAT = "input_format";

    public PrepareMaterialParserTrainingCommand() {
        super("prepare-material-training", "Prepare the training data for the Material Parser using data from already annotated XML or raw TXT files. ");
    }

    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);

        subparser.addArgument("-o", "--output")
            .dest(OUTPUT_PATH)
            .type(Arguments.fileType().verifyIsDirectory().verifyCanWrite().or().verifyNotExists().verifyCanCreate())
            .required(true)
            .help("Output path directory. ");

        subparser.addArgument("-i", "--input")
            .dest(INPUT_PATH)
            .type(Arguments.fileType().verifyCanRead().verifyIsDirectory())
            .required(false)
            .help("Input path directory. ");

        subparser.addArgument("--input-format")
            .dest(INPUT_FORMAT)
            .type(String.class)
            .required(false)
            .setDefault("xml")
            .choices("xml", "txt")
            .help("Format of the input file.");

    }

    @Override
    protected void run(Bootstrap<GrobidSuperconductorsConfiguration> bootstrap, Namespace namespace, GrobidSuperconductorsConfiguration configuration) throws Exception {
        try {
            GrobidHomeFinder grobidHomeFinder = new GrobidHomeFinder(Collections.singletonList(configuration.getGrobidHome()));
            GrobidProperties.getInstance(grobidHomeFinder);
            configuration.getModels().stream().forEach(GrobidProperties::addModel);
            LibraryLoader.load();
        } catch (final Exception exp) {
            System.err.println("Grobid initialisation failed, cannot find Grobid Home. Maybe you forget to specify the config.yml in the command launch?");
            exp.printStackTrace();

            System.exit(-1);
        }

        File inputPath = namespace.get(INPUT_PATH);
        File outputPath = namespace.get(OUTPUT_PATH);
        String inputFormat = namespace.get(INPUT_FORMAT);

        if (inputPath == null) {
            inputPath = GrobidProperties.getCorpusPath(new File("/"), SuperconductorsModels.SUPERCONDUCTORS);
        }

        switch (inputFormat) {
            case "txt":
                new MaterialParserTrainingData().createTrainingFromText(inputPath.toString(), outputPath.toString(), true);
                break;
            case "xml":
                new MaterialTrainer().createTrainingDataFromSuperconductors(inputPath.toString(), outputPath.toString(), true);
                break;
        }
    }
}
