package org.grobid.core.features;

import org.grobid.core.layout.LayoutToken;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class FeaturesVectorNanostructureTest {

    @Test
    public void testFeatures() throws Exception {
        String vector = FeaturesVectorNanostructure.addFeatures(new LayoutToken("双性イオン部位を有する"), "material", "").printVector();

        assertThat(vector.split(" ").length, is(20));
        assertThat(vector, is("双性イオン部位を有する 双 双性 双性イ 双性イオ る する 有する を有する NODIGIT 0 NOPUNCT 双性イオン部位を有する kkykhkhh kykhkh false false BASELINE  material"));
    }

    @Test
    public void testGetCharacterNature_Kanji() throws Exception {
        assertThat(FeaturesVectorNanostructure.getCharacterNature('井'), is('k'));
    }

    @Test
    public void testGetCharacterNature_hiragana() throws Exception {
        assertThat(FeaturesVectorNanostructure.getCharacterNature('ご'), is('h'));
    }

    @Test
    public void testGetCharacterNature_katakana() throws Exception {
        assertThat(FeaturesVectorNanostructure.getCharacterNature('ピ'), is('y'));
    }

    @Test
    public void testGetCharacterNature_r() throws Exception {
        assertThat(FeaturesVectorNanostructure.getCharacterNature('Ｅ'), is('r'));
    }

}