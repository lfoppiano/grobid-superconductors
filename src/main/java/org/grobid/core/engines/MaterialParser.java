package org.grobid.core.engines;


import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.GrobidModel;
import org.grobid.core.data.Superconductor;
import org.grobid.core.engines.tagging.GrobidCRFEngine;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.ChemDataExtractorClient;
import org.grobid.core.utilities.counters.CntManager;

import javax.inject.Inject;

import java.util.List;

import static org.grobid.core.engines.SuperconductorsModels.MATERIAL;

public class MaterialParser extends AbstractParser {

    private static MaterialParser instance;
    public static MaterialParser getInstance() {
        if (instance == null) {
            getNewInstance();
        }
        return instance;
    }

    private static synchronized void getNewInstance() {
        instance = new MaterialParser();
    }

    @Inject
    public MaterialParser() {
        super(MATERIAL);
    }

    protected MaterialParser(GrobidModel model) {
        super(model);
    }


    public Pair<String, List<Superconductor>> generateTrainingData(List<LayoutToken> layoutTokens) {

    }

}
