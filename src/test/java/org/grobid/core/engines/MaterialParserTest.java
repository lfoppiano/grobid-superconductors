package org.grobid.core.engines;

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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.grobid.core.utilities.GrobidTestUtils.getWapitiResult;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

public class MaterialParserTest {

    private MaterialParser target;

    @Before
    public void setUp() throws Exception {
        LibraryLoader.load();
        target = new MaterialParser(GrobidModels.DUMMY);
    }

    private List<String> generateFeatures(List<LayoutToken> layoutTokens) {
        return layoutTokens.stream()
            .map(token -> FeaturesVectorMaterial.addFeatures(token.getText(), null).printVector())
            .collect(Collectors.toList());
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

//        System.out.println(output);

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

//        System.out.println(output);

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
            Triple.of("<name>", 2, 5)
        );

        List<String> features = generateFeatures(layoutTokens);
        String results = getWapitiResult(features, labels);

        List<Material> materials = target.extractResults(layoutTokens, results);

        assertThat(materials, hasSize(1));
        assertThat(materials.get(0).getName(), is("Hydrated sulfide"));
        assertThat(materials.get(0).getShape(), is("Polycrystalline"));
    }

    @Test
    public void testExtractResults_formulaWithVariables() throws Exception {
        //        String text = "graphene, the atomic monolayer modification of graphite, Li-or Ca-decoration";
        String text = "CeM 2 X 2 (M = Cu, Ni, Ru, Rh, Pd, Au, .; X = Si, Ge) ";
        //MNX (M = Zr, Hf; X = Cl, Br, I) 

        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);

        List<Triple<String, Integer, Integer>> labels = Arrays.asList(
            Triple.of("<formula>", 0, 7),
            Triple.of("<variable>", 9, 10),
            Triple.of("<value>", 13, 29),
            Triple.of("<variable>", 34, 35),
            Triple.of("<value>", 38, 42)
        );
        List<String> features = generateFeatures(layoutTokens);

        String results = getWapitiResult(features, labels);

        List<Material> materials = target.extractResults(layoutTokens, results);

        assertThat(materials, hasSize(1));
        assertThat(materials.get(0).getFormula(), is("CeM 2 X 2"));

        assertThat(materials.get(0).getVariables().keySet(), hasSize(2));
        assertThat(materials.get(0).getVariables().get("X"), containsInAnyOrder("Si", "Ge"));
        assertThat(materials.get(0).getVariables().get("M"), containsInAnyOrder("Cu", "Ni", "Ru", "Rh", "Pd", "Au"));
    }

    @Test
    public void testExtractResults_formulaWithDopants() throws Exception {
        String text = "(Sr,K)Fe2As2 films";

        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);

        List<Triple<String, Integer, Integer>> labels = Arrays.asList(
            Triple.of("<formula>", 0, 9),
            Triple.of("<shape>", 10, 11)
        );
        List<String> features = generateFeatures(layoutTokens);

        String results = getWapitiResult(features, labels);

        List<Material> materials = target.extractResults(layoutTokens, results);

        assertThat(materials, hasSize(1));
        assertThat(materials.get(0).getFormula(), is("(Sr,K)Fe2As2"));
        assertThat(materials.get(0).getShape(), is("films"));
        assertThat(materials.get(0).getResolvedFormulas(), hasSize(2));
        assertThat(materials.get(0).getResolvedFormulas().get(0), is("SrFe2As2"));
        assertThat(materials.get(0).getResolvedFormulas().get(1), is("KFe2As2"));

    }

    @Test
    public void testExtractResults_formulaWithVariables_2() throws Exception {
        String text = "CeM m In 3+2m (M = Ir or Co; m = 0, 1) ";

        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);

        List<Triple<String, Integer, Integer>> labels = Arrays.asList(
            Triple.of("<formula>", 0, 10),
            Triple.of("<variable>", 12, 13),
            Triple.of("<value>", 15, 21),
            Triple.of("<variable>", 23, 24),
            Triple.of("<value>", 27, 31)
        );

        List<String> features = generateFeatures(layoutTokens);

        String results = getWapitiResult(features, labels);

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
            Triple.of("<formula>", 6, 9),
            Triple.of("<shape>", 10, 11)
        );

        List<String> features = generateFeatures(layoutTokens);

        String results = getWapitiResult(features, labels);

        List<Material> materials = target.extractResults(layoutTokens, results);

        assertThat(materials, hasSize(2));
        assertThat(materials.get(0).getShape(), is("powder"));
        assertThat(materials.get(0).getFormula(), is("Bi-2212"));

        assertThat(materials.get(1).getShape(), is("powder"));
        assertThat(materials.get(1).getFormula(), is("Bi-2223"));
    }

    @Test
    public void testExtractResults_singleName_multipleDopings_shouldGenerate2Objects() {
        String text = "Zn-doped and Cu-doped MgB2";
        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);

        // These triples made in following way: label, starting index (included), ending index (excluded)
        List<Triple<String, Integer, Integer>> labels = Arrays.asList(
            Triple.of("<doping>", 0, 1),
            Triple.of("<doping>", 6, 7),
            Triple.of("<formula>", 10, 12)
        );

        List<String> features = generateFeatures(layoutTokens);
        String results = getWapitiResult(features, labels);

        List<Material> materials = target.extractResults(layoutTokens, results);

        assertThat(materials, hasSize(2));
        assertThat(materials.get(0).getFormula(), is("MgB2"));
        assertThat(materials.get(0).getDoping(), is("Zn"));
        assertThat(materials.get(1).getFormula(), is("MgB2"));
        assertThat(materials.get(1).getDoping(), is("Cu"));
    }

    @Test
    public void testExtractResults_singleName_multipleShapes_shouldMergeShapeAndReturn1Object() {
        String text = "polycrystalline MgB2 thin film";
        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);

        // These triples made in following way: label, starting index (included), ending index (excluded)
        List<Triple<String, Integer, Integer>> labels = Arrays.asList(
            Triple.of("<shape>", 0, 1),
            Triple.of("<formula>", 2, 4),
            Triple.of("<shape>", 5, 8)
            );

        List<String> features = generateFeatures(layoutTokens);
        String results = getWapitiResult(features, labels);

        List<Material> materials = target.extractResults(layoutTokens, results);

        assertThat(materials, hasSize(1));
        assertThat(materials.get(0).getFormula(), is("MgB2"));
        assertThat(materials.get(0).getShape(), is("polycrystalline, thin film"));
    }

    @Test
    public void testExtractResults_doubleName_multipleShapes_shouldMergeShapeAndAppplyToEachMaterials() {
        String text = "polycrystalline MgB2 and LaFeO2 thin film";
        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);

        // These triples made in following way: label, starting index (included), ending index (excluded)
        List<Triple<String, Integer, Integer>> labels = Arrays.asList(
            Triple.of("<shape>", 0, 1),
            Triple.of("<formula>", 2, 4),
            Triple.of("<formula>", 7, 9),
            Triple.of("<shape>", 10, 13)
        );

        List<String> features = generateFeatures(layoutTokens);
        String results = getWapitiResult(features, labels);

        List<Material> materials = target.extractResults(layoutTokens, results);

        assertThat(materials, hasSize(2));
        assertThat(materials.get(0).getFormula(), is("MgB2"));
        assertThat(materials.get(0).getShape(), is("polycrystalline, thin film"));
        assertThat(materials.get(1).getFormula(), is("LaFeO2"));
        assertThat(materials.get(1).getShape(), is("polycrystalline, thin film"));
    }

    @Test
    public void testExtractResults_doubleName_multipleShapes_multipleSubstrates_shouldMergeShapeAndSubstratesAndAppplyToEachMaterials() {
        String text = "polycrystalline MgB2 and LaFeO2 thin film grown on StrO3 and Al";
        List<LayoutToken> layoutTokens = DeepAnalyzer.getInstance().tokenizeWithLayoutToken(text);

        // These triples made in following way: label, starting index (included), ending index (excluded)
        List<Triple<String, Integer, Integer>> labels = Arrays.asList(
            Triple.of("<shape>", 0, 1),
            Triple.of("<formula>", 2, 4),
            Triple.of("<formula>", 7, 9),
            Triple.of("<shape>", 10, 13),
            Triple.of("<substrate>", 18, 20),
            Triple.of("<substrate>", 23, 24)
        );

        List<String> features = generateFeatures(layoutTokens);
        String results = getWapitiResult(features, labels);

        List<Material> materials = target.extractResults(layoutTokens, results);

        assertThat(materials, hasSize(2));
        assertThat(materials.get(0).getFormula(), is("MgB2"));
        assertThat(materials.get(0).getShape(), is("polycrystalline, thin film"));
        assertThat(materials.get(0).getSubstrate(), is("StrO3, Al"));
        assertThat(materials.get(1).getFormula(), is("LaFeO2"));
        assertThat(materials.get(1).getShape(), is("polycrystalline, thin film"));
        assertThat(materials.get(1).getSubstrate(), is("StrO3, Al"));
    }

    @Test
    public void testExpandFormula() throws Exception {
        String formula = "(Sr,K)Fe2As2";

        List<String> expandFormulas = Material.expandFormula(formula);

        assertThat(expandFormulas, hasSize(2));
        assertThat(expandFormulas.get(0), is("SrFe2As2"));
        assertThat(expandFormulas.get(1), is("KFe2As2"));
    }

    @Test
    public void testExpandFormula2() throws Exception {
        String formula = "(Sr , K ) Fe2As2";

        List<String> expandFormulas = Material.expandFormula(formula);

        assertThat(expandFormulas, hasSize(2));
        assertThat(expandFormulas.get(0), is("SrFe2As2"));
        assertThat(expandFormulas.get(1), is("KFe2As2"));
    }

    @Test
    public void testExpandName() throws Exception {
        String formula = "(Sr,K)-2222";

        List<String> expandFormulas = Material.expandFormula(formula);

        assertThat(expandFormulas, hasSize(2));
        assertThat(expandFormulas.get(0), is("Sr-2222"));
        assertThat(expandFormulas.get(1), is("K-2222"));
    }

    @SuppressWarnings("unchecked")
    public static org.hamcrest.Matcher<java.util.Map<String, Object>> hasListEntry(org.hamcrest.Matcher<String> keyMatcher, org.hamcrest.Matcher<java.lang.Iterable<?>> valueMatcher) {
        Matcher mapMatcher = org.hamcrest.collection.IsMapContaining.<String, List<?>>hasEntry(keyMatcher, valueMatcher);
        return mapMatcher;
    }

}