package org.grobid.service.command;

import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.grobid.core.engines.SuperconductorsModels;
import org.grobid.core.engines.linking.CRFBasedLinker;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.main.GrobidHomeFinder;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;
import org.grobid.trainer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static net.sourceforge.argparse4j.impl.Arguments.storeTrue;

public class RunTrainingCommand extends ConfiguredCommand<GrobidSuperconductorsConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RunTrainingCommand.class);

    private final static String ACTION = "action";
    private final static String PRINT = "print";
    private final static String RECURSIVE = "recursive";
    private final static String FOLD_COUNT = "foldCount";
    private final static String MODEL_NAME = "model";
    private static final String SPLIT = "split";

    private final static List<String> ACTIONS = Arrays.asList("train", "nfold", "train_eval", "holdout");


    public RunTrainingCommand() {
        super("training", "Training / Evaluate the model using different approaches. ");
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
            .required(true)
            .choices(Arrays.asList("superconductors", 
                "material", 
                "entityLinker-" + CRFBasedLinker.MATERIAL_TCVALUE_ID,
                "entityLinker-" + CRFBasedLinker.TCVALUE_ME_METHOD_ID, 
                "entityLinker-" + CRFBasedLinker.TCVALUE_PRESSURE_ID,
                "superconductors-no-features", 
                "superconductors-no-features-abstracts"))
            .help("Model to train");

        subparser.addArgument("-op", "--onlyPrint")
            .dest(PRINT)
            .type(Boolean.class)
            .required(false)
            .setDefault(Boolean.FALSE)
            .action(storeTrue())
            .help("Print on screen instead of writing on a log file");

//        subparser.addArgument("-r", "--recursive")
//            .dest(RECURSIVE)
//            .type(Integer.class)
//            .required(false)
//            .help("Limit the training to a certain number of papers (useful to record training improvement when increasing training data)");

        subparser.addArgument("-fc", "--fold-count")
            .dest(FOLD_COUNT)
            .type(Integer.class)
            .required(false)
            .setDefault(10)
            .help("Specify if the number of fold in n-fold cross-validation. ");

        subparser.addArgument("-s", "--split")
            .dest(SPLIT)
            .type(Double.class)
            .required(false)
            .setDefault(0.8f)
            .help("Specify the split rate between training and evaluation data. Used only in case of train_eval. Default: 0.8. ");
    }

    @Override
    protected void run(Bootstrap bootstrap, Namespace namespace, GrobidSuperconductorsConfiguration configuration) throws Exception {
        try {
            GrobidHomeFinder grobidHomeFinder = new GrobidHomeFinder(Arrays.asList(configuration.getGrobidHome()));
            GrobidProperties.getInstance(grobidHomeFinder);
            configuration.getModels().stream().forEach(GrobidProperties::addModel);
            LibraryLoader.load();
        } catch (final Exception exp) {
            System.err.println("Grobid initialisation failed, cannot find Grobid Home. Maybe you forget to specify the config.yml in the command launch?");
            exp.printStackTrace();

            System.exit(-1);
        }

        String modelName = namespace.get(MODEL_NAME);
        String action = namespace.get(ACTION);
        Boolean print = namespace.get(PRINT);
        Float split = namespace.get(SPLIT);
        Integer foldCount = namespace.get(FOLD_COUNT);

        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        Trainer trainer = null;

        if (SuperconductorsModels.SUPERCONDUCTORS.getModelName().equals(modelName)) {
            trainer = new SuperconductorsTrainer();
        } else if ("superconductors-no-features".equals(modelName)) {
            trainer = new SuperconductorsNoFeaturesTrainer();
        } else if ("superconductors-no-features-abstracts".equals(modelName)) {
            trainer = new SuperconductorsNoFeaturesOnlyAbstractsTrainer();
        } else if (SuperconductorsModels.MATERIAL.getModelName().equals(modelName)) {
            trainer = new MaterialTrainer();
        } else if (SuperconductorsModels.ENTITY_LINKER_MATERIAL_TC.getModelName().equals(modelName)) {
            trainer = new EntityLinkerMaterialTcTrainer();
        } else if (SuperconductorsModels.ENTITY_LINKER_TC_PRESSURE.getModelName().equals(modelName)) {
            trainer = new EntityLinkerTcValuePressureTrainer();
        } else if (SuperconductorsModels.ENTITY_LINKER_TC_ME_METHOD.getModelName().equals(modelName)) {
            trainer = new EntityLinkerTcValueMeMethodTrainer();
        } else {
            System.out.println("The model name " + modelName + " does not correspond to any model. ");
            System.out.println(super.getDescription());
            System.exit(-1);
        }

        String name = "";
        String report = null;
        switch (action) {
            case "train":
                AbstractTrainer.runTraining(trainer);
                break;
            case "nfold":
                report = AbstractTrainer.runNFoldEvaluation(trainer, foldCount, true);
                name = foldCount + "-fold-cross-validation";
                break;
            case "train_eval":
                report = AbstractTrainer.runSplitTrainingEvaluation(trainer, Double.valueOf(split));
                name = "train_eval-with-split-" + split;
                break;
            case "holdout":
                report = AbstractTrainer.runEvaluation(trainer, true);
                name = "holdout-evaluation";
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

                try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("logs/" + modelName + "-" + name
                    + "-" + formatter.format(date) + ".txt"))) {
                    writer.write(report);
                    writer.write("\n");
                } catch (IOException e) {
                    throw new GrobidException("Error when saving evaluation results into files. ", e);
                }
            } else {
                System.out.println(report);
            }
        }


    }
}