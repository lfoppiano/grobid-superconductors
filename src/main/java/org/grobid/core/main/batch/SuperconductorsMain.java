package org.grobid.core.main.batch;

import org.apache.commons.lang3.StringUtils;
import org.grobid.core.engines.QuantityParser;
import org.grobid.core.engines.training.QuantityParserTrainingData;
import org.grobid.core.engines.training.SuperconductorsParserTrainingData;
import org.grobid.core.main.GrobidHomeFinder;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.utilities.GrobidProperties;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class SuperconductorsMain {

    private static List<String> availableCommands = Arrays.asList("createTrainingSuperconductors");

    private static GrobidMainArgs gbdArgs;


    protected final static String getPath2GbdProperties(final String pPath2GbdHome) {
        return pPath2GbdHome + File.separator + "config" + File.separator + "grobid.properties";
    }

    protected static void initProcess() {
        try {
            LibraryLoader.load();
        } catch (final Exception exp) {
            System.err.println("Grobid initialisation failed: " + exp);
        }
        GrobidProperties.getInstance();
    }

    protected static void initProcess(String grobidHome) {
        try {
            GrobidHomeFinder grobidHomeFinder = new GrobidHomeFinder(Arrays.asList(grobidHome));
            LibraryLoader.load();
            GrobidProperties.getInstance(grobidHomeFinder);
        } catch (final Exception exp) {
            System.err.println("Grobid initialisation failed: " + exp);
        }
    }

    protected static String getHelp() {
        final StringBuffer help = new StringBuffer();
        help.append("HELP GROBID\n");
        help.append("-h: displays help\n");
        help.append("-gH: gives the path to grobid home directory.\n");
        help.append("-dIn: gives the path to the directory where the files to be processed are located, to be used only when the called method needs it.\n");
        help.append("-dOut: gives the path to the directory where the result files will be saved. The default output directory is the curent directory.\n");
        help.append("-s: is the parameter used for process using string as input and not file.\n");
        help.append("-r: recursive directory processing, default processing is not recursive.\n");
        help.append("-exe: gives the command to execute. The value should be one of these:\n");
        help.append("\t" + availableCommands + "\n");
        return help.toString();
    }

    protected static boolean processArgs(final String[] pArgs) {
        boolean result = true;
        if (pArgs.length == 0) {
            System.out.println(getHelp());
            result = false;
        } else {
            String currArg;
            for (int i = 0; i < pArgs.length; i++) {
                currArg = pArgs[i];
                if (currArg.equals("-h")) {
                    System.out.println(getHelp());
                    result = false;
                    break;
                }
                if (currArg.equals("-gH")) {
                    gbdArgs.setPath2grobidHome(pArgs[i + 1]);
                    if (pArgs[i + 1] != null) {
                        gbdArgs.setPath2grobidProperty(getPath2GbdProperties(pArgs[i + 1]));
                    }
                    i++;
                    continue;
                }
                if (currArg.equals("-dIn")) {
                    if (pArgs[i + 1] != null) {
                        gbdArgs.setPath2Input(pArgs[i + 1]);
                        gbdArgs.setPdf(true);
                    }
                    i++;
                    continue;
                }
                if (currArg.equals("-s")) {
                    if (pArgs[i + 1] != null) {
                        gbdArgs.setInput(pArgs[i + 1]);
                        gbdArgs.setPdf(false);
                    }
                    i++;
                    continue;
                }
                if (currArg.equals("-dOut")) {
                    if (pArgs[i + 1] != null) {
                        gbdArgs.setPath2Output(pArgs[i + 1]);
                    }
                    i++;
                    continue;
                }
                if (currArg.equals("-exe")) {
                    final String command = pArgs[i + 1];
                    if (availableCommands.contains(command)) {
                        gbdArgs.setProcessMethodName(command);
                        i++;
                        continue;
                    } else {
                        System.err.println("-exe value should be one value from this list: " + availableCommands);
                        result = false;
                        break;
                    }
                }
                if (currArg.equals("-r")) {
                    gbdArgs.setRecursive(true);
                    continue;
                }
            }
        }
        return result;
    }

    /**
     * Starts Grobid from command line using the following parameters:
     *
     * @param args The arguments
     */
    public static void main(final String[] args) throws Exception {
        gbdArgs = new GrobidMainArgs();

        if (processArgs(args) && (gbdArgs.getProcessMethodName() != null)) {
            if (StringUtils.isEmpty(gbdArgs.getPath2grobidHome())) {
                initProcess();
            } else {
                initProcess(gbdArgs.getPath2grobidHome());
            }

            int nb = 0;
//            QuantityParser quantityParser = QuantityParser.getInstance();
//            QuantityParserTrainingData quantityParserTrainingData = new QuantityParserTrainingData(quantityParser);

            long time = System.currentTimeMillis();

//            if (gbdArgs.getProcessMethodName().equals("processQuantities")) {
//                nb = quantityParser.batchProcess(gbdArgs.getPath2Input(), gbdArgs.getPath2Output(), gbdArgs.isRecursive());
//            } else if (gbdArgs.getProcessMethodName().equals("createTrainingQuantities")) {
//                nb = quantityParserTrainingData.createTrainingBatch(gbdArgs.getPath2Input(), gbdArgs.getPath2Output(), -1);
//            } else if (gbdArgs.getProcessMethodName().equals("generateTrainingUnits")) {
//                UnitTrainingDataGenerator unitTrainingDataGenerator = new UnitTrainingDataGenerator();
//                unitTrainingDataGenerator.generateData(gbdArgs.getPath2Input(), gbdArgs.getPath2Output());
//            } else
            if (gbdArgs.getProcessMethodName().equals("createTrainingSuperconductors")) {
                new SuperconductorsParserTrainingData().createTrainingBatch(gbdArgs.getPath2Input(), gbdArgs.getPath2Output(), -1);
            } else {
                getHelp();
            }
            System.out.println(nb + " files processed in " + (System.currentTimeMillis() - time) + " milliseconds");
        }
    }
}
