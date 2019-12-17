package org.grobid.service.command;

import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.grobid.core.engines.Engine;
import org.grobid.core.engines.SuperconductorsModels;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.main.GrobidHomeFinder;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;
import org.grobid.trainer.AbstractTrainer;
import org.grobid.trainer.SuperconductorsTrainer;
import org.grobid.trainer.Trainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class RunTrainingCommand extends ConfiguredCommand<GrobidSuperconductorsConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RunTrainingCommand.class);
    private final static String ACTION = "action";
    private final static String PRINT = "print";
    private final static String RECURSIVE = "recursive";
    private final static String FOLD_TYPE = "foldType";
    private final static String MODEL_NAME = "model";
    private final static String MAX_PAPER_NUMBER = "maxPaperNumber";

    private final static List<String> ACTIONS = Arrays.asList("train", "10fold", "train_eval", "holdout");


    public RunTrainingCommand() {
        super("training", "Training / Evaluate the model ");
    }

    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);

        subparser.addArgument("-a", "--action")
            .dest(ACTION)
            .type(String.class)
            .required(false)
            .choices(ACTIONS)
            .setDefault("train")
            .help("Actions to the training command. ");

        subparser.addArgument("-m", "--model")
            .dest(MODEL_NAME)
            .type(String.class)
            .required(false)
            .setDefault("superconductors")
            .help("Model to train");

        subparser.addArgument("-op", "--onlyPrint")
            .dest(PRINT)
            .type(Boolean.class)
            .required(false)
            .setDefault(Boolean.FALSE)
            .help("Print on screen instead of writing on a log file");

        subparser.addArgument("-n", "--max-paper-number")
            .dest(MAX_PAPER_NUMBER)
            .type(Integer.class)
            .required(false)
            .help("Limit the training to a certain number of papers (useful to record training improvement when increasing training data)");

        /*subparser.addArgument("-ft", "--fold-type")
            .dest(FOLD_TYPE)
            .choices(Arrays.asList(FOLD_TYPE_PARAGRAPH, FOLD_TYPE_DOCUMENT))
            .type(String.class)
            .required(false)
            .setDefault(FOLD_TYPE_PARAGRAPH)
            .help("Specify if the fold (how a training sample is defined) should be by paragraph or by document. ");*/
    }

    @Override
    protected void run(Bootstrap bootstrap, Namespace namespace, GrobidSuperconductorsConfiguration configuration) throws Exception {
        try {
            GrobidProperties.set_GROBID_HOME_PATH(new File(configuration.getGrobidHome()).getAbsolutePath());
            String grobidHome = configuration.getGrobidHome();
            if (grobidHome != null) {
                GrobidProperties.setGrobidPropertiesPath(new File(grobidHome, "/config/grobid.properties").getAbsolutePath());
            }

            GrobidHomeFinder grobidHomeFinder = new GrobidHomeFinder(Arrays.asList(configuration.getGrobidHome()));
            GrobidProperties.getInstance(grobidHomeFinder);
            Engine.getEngine(true);
            LibraryLoader.load();
        } catch (final Exception exp) {
            System.err.println("Grobid initialisation failed, cannot find Grobid Home. Maybe you forget to specify the config.yml in the command launch?");
            exp.printStackTrace();

            System.exit(-1);
        }

        String modelName = namespace.get(MODEL_NAME);
        String action = namespace.get(ACTION);
        Boolean print = namespace.get(PRINT);
        Integer maxPaperNumber = namespace.get(MAX_PAPER_NUMBER);
        String foldType = namespace.get(FOLD_TYPE);

        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");

        if (SuperconductorsModels.SUPERCONDUCTORS.getModelName().equals(modelName)) {
            Trainer trainer = null;

//            if (foldType.equals(FOLD_TYPE_PARAGRAPH)) {
                trainer = new SuperconductorsTrainer();
//            } else {
//                trainer = new SuperconductorsTrainerByDocuments();
//            }

            String report = null;
            switch (action) {
                case "train":
                    AbstractTrainer.runTraining(trainer);
                    break;
                case "10fold":
                    report = AbstractTrainer.runNFoldEvaluation(trainer, 10, true);
                    break;
                case "train_eval":
                    report = AbstractTrainer.runSplitTrainingEvaluation(trainer, 0.8);
                    break;
                case "holdout":
                    AbstractTrainer.runTraining(trainer);
                    report = AbstractTrainer.runEvaluation(trainer, true);
                    break;
                default:
                    System.out.println("No correct action were supplied. Please provide beside " + Arrays.toString(ACTIONS.toArray()));
                    break;

            }
            if (report != null) {
                if (!print) {
                    if (!Files.exists(Paths.get("logs"))) {
                        Files.createDirectory(Paths.get("logs"));
                    }

                    try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("logs/superconductors-evaluation-" + formatter.format(date) + ".txt"))) {
                        writer.write(report);
                        writer.write("\n");
                    } catch (IOException e) {
                        throw new GrobidException("Error when saving evaluation results into files. ", e);
                    }
                } else {
                    System.out.println(report);
                }
            }
        } else {
            System.out.println(super.getDescription());
        }

    }
}