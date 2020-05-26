package org.grobid.core.engines;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.grobid.core.GrobidModels;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.Material;
import org.grobid.core.features.FeaturesVectorMaterial;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.main.LibraryLoader;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

public class MaterialParserTest {

    private MaterialParser target;

    @Before
    public void setUp() throws Exception {
        LibraryLoader.load();
        target = new MaterialParser(GrobidModels.DUMMY);
    }

    @Test
    public void testExtractResults_formula_name_shouldWork() throws Exception {

        String result = "La la L La La La a La La La INITCAP NODIGIT 0 NOPUNCT La Xx Xx I-<formula>\n" +
            "2 2 2 2 2 2 2 2 2 2 NOCAPS ALLDIGIT 1 NOPUNCT X d d <formula>\n" +
            "− − − − − − − − − − ALLCAPS NODIGIT 1 HYPHEN − − − <formula>\n" +
            "xSrxCuO xsrxcuo x xS xSr xSrx O uO CuO xCuO NOCAPS NODIGIT 0 NOPUNCT xSrxCuO xXxXxX xXxXxX <formula>\n" +
            "4 4 4 4 4 4 4 4 4 4 NOCAPS ALLDIGIT 1 NOPUNCT X d d <formula>\n" +
            "( ( ( ( ( ( ( ( ( ( ALLCAPS NODIGIT 1 OPENBRACKET ( ( ( <other>\n" +
            "LSCO lsco L LS LSC LSCO O CO SCO LSCO ALLCAPS NODIGIT 0 NOPUNCT LSCO XXXX X I-<name>\n" +
            ") ) ) ) ) ) ) ) ) ) ALLCAPS NODIGIT 1 ENDBRACKET ) ) ) <other>";

        String text = "La2−xSrxCuO4 (LSCO)";

        List<LayoutToken> tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);
        List<Material> output = target.extractResults(tokens, result);

        System.out.println(output);

        assertThat(output, hasSize(1));
        assertThat(output.get(0).getName(), is("LSCO"));
        assertThat(output.get(0).getFormula(), is("La2−xSrxCuO4"));
    }

    @Test
    public void testExtractResults_doping_formula_variables_shouldWork() throws Exception {

        String result = "Under under U Un Und Unde r er der nder INITCAP NODIGIT 0 NOPUNCT Under Xxxx Xx I-<doping>\n" +
            "- - - - - - - - - - ALLCAPS NODIGIT 1 HYPHEN - - - <doping>\n" +
            "doped doped d do dop dope d ed ped oped NOCAPS NODIGIT 0 NOPUNCT doped xxxx x <doping>\n" +
            "La la L La La La a La La La INITCAP NODIGIT 0 NOPUNCT La Xx Xx I-<formula>\n" +
            "2 2 2 2 2 2 2 2 2 2 NOCAPS ALLDIGIT 1 NOPUNCT X d d <formula>\n" +
            "− − − − − − − − − − ALLCAPS NODIGIT 1 HYPHEN − − − <formula>\n" +
            "x x x x x x x x x x NOCAPS NODIGIT 1 NOPUNCT x x x <formula>\n" +
            "Sr sr S Sr Sr Sr r Sr Sr Sr INITCAP NODIGIT 0 NOPUNCT Sr Xx Xx <formula>\n" +
            "x x x x x x x x x x NOCAPS NODIGIT 1 NOPUNCT x x x <formula>\n" +
            "CuO cuo C Cu CuO CuO O uO CuO CuO INITCAP NODIGIT 0 NOPUNCT CuO XxX XxX <formula>\n" +
            "4 4 4 4 4 4 4 4 4 4 NOCAPS ALLDIGIT 1 NOPUNCT X d d <formula>\n" +
            "with with w wi wit with h th ith with NOCAPS NODIGIT 0 NOPUNCT with xxxx x <other>\n" +
            "x x x x x x x x x x NOCAPS NODIGIT 1 NOPUNCT x x x I-<variable>\n" +
            "= = = = = = = = = = ALLCAPS NODIGIT 1 NOPUNCT = = = <other>\n" +
            "0 0 0 0 0 0 0 0 0 0 NOCAPS ALLDIGIT 1 NOPUNCT X d d I-<value>\n" +
            ". . . . . . . . . . ALLCAPS NODIGIT 1 DOT . . . <value>\n" +
            "063 063 0 06 063 063 3 63 063 063 NOCAPS ALLDIGIT 0 NOPUNCT XXX ddd d <value>\n" +
            "- - - - - - - - - - ALLCAPS NODIGIT 1 HYPHEN - - - <value>\n" +
            "0 0 0 0 0 0 0 0 0 0 NOCAPS ALLDIGIT 1 NOPUNCT X d d <value>\n" +
            ". . . . . . . . . . ALLCAPS NODIGIT 1 DOT . . . <value>\n" +
            "125 125 1 12 125 125 5 25 125 125 NOCAPS ALLDIGIT 0 NOPUNCT XXX ddd d <value>";

        String text = "Under-doped La 2−x Sr x CuO 4 with x = 0.063 -0.125";

        List<LayoutToken> tokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);
        List<Material> output = target.extractResults(tokens, result);

        System.out.println(output);

        assertThat(output, hasSize(1));
        assertThat(output.get(0).getName(), is(nullValue()));
        assertThat(output.get(0).getFormula(), is("La 2−x Sr x CuO 4"));
        assertThat(output.get(0).getDoping(), is("Under-doped"));
        assertThat(output.get(0).getVariables().get("x"), containsInAnyOrder("0.063 -0.125"));

        assertThat(output.get(0).getRawTaggedValue(), is("<doping>Under-doped</doping> <formula>La 2−x Sr x CuO 4</formula> with <variable>x</variable> = <value>0.063 -0.125</value>"));
    }


    @Test
    public void testExtractResults_singleMaterial() {
        String text = "Polycrystalline Hydrated sulfide";
        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);

        // These triples made in following way: label, starting index (included), ending index (excluded)
        List<Triple<String, Integer, Integer>> labels = Arrays.asList(
            Triple.of("<shape>", 0, 1),
            Triple.of("<name>", 1, 3)
        );

        String result = getWapitiResult(layoutTokens, labels);


        List<Material> materials = target.extractResults(layoutTokens, result);

        assertThat(materials, hasSize(1));

        assertThat(materials.get(0).getName(), is("Hydrated sulfide"));
        assertThat(materials.get(0).getShape(), is("Polycrystalline"));
    }

    @Test
    public void testExtractResults_formulaWithVariables() throws Exception {
        //        String text = "graphene, the atomic monolayer modification of graphite, Li-or Ca-decoration";
        String text = "CeM 2 X 2 (M = Cu, Ni, Ru, Rh, Pd, Au, .; X = Si, Ge) ";

        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);

        List<LayoutToken> layoutTokensWithoutSpaces = layoutTokens.stream()
            .filter(token -> StringUtils.isNotBlank(token.getText()))
            .collect(Collectors.toList());

        List<Triple<String, Integer, Integer>> labels = Arrays.asList(
            Triple.of("<formula>", 0, 4),
            Triple.of("<variable>", 5, 6),
            Triple.of("<value>", 7, 18),
            Triple.of("<variable>", 21, 22),
            Triple.of("<value>", 23, 26)
        );
        String results = getWapitiResult(layoutTokens, labels);

        List<Material> materials = target.extractResults(layoutTokens, results);

        assertThat(materials, hasSize(1));
        assertThat(materials.get(0).getFormula(), is("CeM 2 X 2"));

        assertThat(materials.get(0).getVariables().keySet(), hasSize(2));
        assertThat(materials.get(0).getVariables().get("X"), containsInAnyOrder("Si", "Ge"));
        assertThat(materials.get(0).getVariables().get("M"), containsInAnyOrder("Cu", "Ni", "Ru", "Rh", "Pd", "Au"));
    }

    @Test
    public void testExtractResults_formulaWithVariables_2() throws Exception {
        String text = "CeM m In 3+2m (M = Ir or Co; m = 0, 1) ";

        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);

        List<LayoutToken> layoutTokensWithoutSpaces = layoutTokens.stream()
            .filter(token -> StringUtils.isNotBlank(token.getText()))
            .collect(Collectors.toList());

        List<Triple<String, Integer, Integer>> labels = Arrays.asList(
            Triple.of("<formula>", 0, 7),
            Triple.of("<variable>", 8, 9),
            Triple.of("<value>", 10, 13),
            Triple.of("<variable>", 14, 15),
            Triple.of("<value>", 16, 19)
        );
        String results = getWapitiResult(layoutTokens, labels);

        List<Material> materials = target.extractResults(layoutTokens, results);

        assertThat(materials, hasSize(1));
        assertThat(materials.get(0).getFormula(), is("CeM m In 3+2m"));
        assertThat(materials.get(0).getVariables().keySet(), hasSize(2));
        assertThat(materials.get(0).getVariables().get("M"), containsInAnyOrder("Ir", "Co"));
        assertThat(materials.get(0).getVariables().get("m"), containsInAnyOrder("0", "1"));
    }

    @Test
    public void testExtractResults_multipleMaterialsWithSharedShape() throws Exception {
        String text = "Bi-2212 or Bi-2223 powder";
        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);

        List<Triple<String, Integer, Integer>> labels = Arrays.asList(
            Triple.of("<formula>", 0, 3),
            Triple.of("<formula>", 4, 7),
            Triple.of("<shape>", 7, 8)
        );
        String results = getWapitiResult(layoutTokens, labels);

        List<Material> materials = target.extractResults(layoutTokens, results);

        assertThat(materials, hasSize(2));
        assertThat(materials.get(0).getShape(), is("powder"));
        assertThat(materials.get(0).getFormula(), is("Bi-2212"));

        assertThat(materials.get(1).getShape(), is("powder"));
        assertThat(materials.get(1).getFormula(), is("Bi-2223"));
    }


    /**
     * Utility method to generate a hypotetical result from wapiti.
     * Useful for testing the extraction of the sequence labeling.
     *
     * @param layoutTokens layout tokens of the initial text
     * @param labels       label maps. A list of Tripels, containing label (left), start_index (middle) and end_index exclusive (right)
     * @return a string containing the resulting features + labels returned by wapiti
     */
    public static String getWapitiResult(List<LayoutToken> layoutTokens, List<Triple<String, Integer, Integer>> labels) {

        List<String> features = layoutTokens.stream()
            .filter(token -> StringUtils.isNotBlank(token.getText()))
            .map(token -> FeaturesVectorMaterial.addFeatures(token.getText(), null).printVector())
            .collect(Collectors.toList());

        List<String> labeled = new ArrayList<>();
        int idx = 0;

        for (Triple<String, Integer, Integer> label : labels) {

            if (idx < label.getMiddle()) {
                for (int i = idx; i < label.getMiddle(); i++) {
                    labeled.add("<other>");
                    idx++;
                }
            }

            for (int i = label.getMiddle(); i < label.getRight(); i++) {
                labeled.add(label.getLeft());
                idx++;
            }
        }

        if (idx < features.size()) {
            for (int i = idx; i < features.size(); i++) {
                labeled.add("<other>");
                idx++;
            }
        }

        assertThat(features, hasSize(labeled.size()));

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < features.size(); i++) {
            sb.append(features.get(i)).append(" ").append(labeled.get(i)).append("\n");
        }

        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public static org.hamcrest.Matcher<java.util.Map<String, Object>> hasListEntry(org.hamcrest.Matcher<String> keyMatcher, org.hamcrest.Matcher<java.lang.Iterable<?>> valueMatcher) {
        Matcher mapMatcher = org.hamcrest.collection.IsMapContaining.<String, List<?>>hasEntry(keyMatcher, valueMatcher);
        return mapMatcher;
    }

}