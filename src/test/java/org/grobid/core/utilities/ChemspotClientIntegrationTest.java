package org.grobid.core.utilities;

import org.grobid.core.data.chemspot.Mention;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class ChemspotClientIntegrationTest {

    @Test
    public void test() {
        ChemspotClient client = new ChemspotClient("http://falcon.nims.go.jp/chemspot");



        List<Mention> mentions = client.processText("The abilities of LHRH and a potent LHRH agonist ([D-Ser-(But),6,des-Gly-NH210]LHRH ethylamide) inhibit FSH responses by rat granulosa cells and Sertoli cells in vitro have been compared.");

        System.out.println(mentions);
    }

}