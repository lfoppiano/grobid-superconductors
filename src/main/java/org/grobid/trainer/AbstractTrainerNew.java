package org.grobid.trainer;

import org.grobid.core.GrobidModel;

import java.io.File;

public abstract class AbstractTrainerNew extends AbstractTrainer {
    public AbstractTrainerNew(GrobidModel model) {
        super(model);
    }

    public abstract int createCRFPPDataSingle(File inputFile, File outputDirectory);
}
