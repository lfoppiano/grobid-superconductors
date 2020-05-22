package org.grobid.core.engines;

import org.grobid.core.GrobidModels;
import org.grobid.core.analyzers.DeepAnalyzer;
import org.grobid.core.data.Material;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.main.LibraryLoader;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.*;

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
        assertThat(output.get(0).getVariables().get("x"), is("0.063 -0.125"));

        assertThat(output.get(0).getRawTaggedValue(), is("<doping>Under-doped</doping> <formula>La 2−x Sr x CuO 4</formula> with <variable>x</variable> = <value>0.063 -0.125</value>"));
    }


}