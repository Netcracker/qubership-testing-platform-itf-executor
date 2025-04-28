/*
 * # Copyright 2024-2025 NetCracker Technology Corporation
 * #
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * # you may not use this file except in compliance with the License.
 * # You may obtain a copy of the License at
 * #
 * #      http://www.apache.org/licenses/LICENSE-2.0
 * #
 * # Unless required by applicable law or agreed to in writing, software
 * # distributed under the License is distributed on an "AS IS" BASIS,
 * # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * # See the License for the specific language governing permissions and
 * # limitations under the License.
 *
 */

package org.qubership.automation.itf.ui.controls.service.export;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TemplateExtractor.class})
public class TemplateExtractorTest {

    @Autowired
    private TemplateExtractor templateExtractor;

    public TemplateExtractorTest() {
    }

    @Test
    public void findLoadPart() {
        String input = "" + "#set($tc.saved.env=${___environment___.name})" + "#load_part(\"Rebase\")"
                + "#set($chechReg='(?s)(^(.*'+$tc.saved.env+'.*)$)')" + "#if($envCheck.matches($chechReg))"
                + "#set($tc.saved.isRebase = 1)" + "#else" + "#set($tc.saved.isRebase = 0)" + "#end"
                + "#set($accountNum = $tc.Account.AccountNum)" + "#load_part(\"Rebase2\")"
                + "##set($startDtm = $date.format('yyyy-MM-dd''T''HH:mm:ss', $date))"
                + "#if($tc.disconnected != \"true\")"
                + "#set($startDtm = $date.format('yyyy-MM-dd''T''HH:mm:ss', $date))" + "#else"
                + "#set($x = $date.calendar)" + "#load_part(\"Rebase3\")#set($aaa = $x.add(12,-5))"
                + "#set($startDtm = $date.format('yyyy-MM-dd''T''HH:mm:ss', $x))\\n#load_part(\"Rebase4\")"
                + "   #load_part(\"1235\")#end "
                + "#load_part(\"Rebase\")";// the same name of template - won't be added
        Set<String> loadPartTemplates = templateExtractor.findLoadPartTemplates(input);
        Assert.assertNotNull(loadPartTemplates);
        Assert.assertFalse(loadPartTemplates.isEmpty());
        String s = loadPartTemplates.toString();
        Assert.assertEquals(s, "[Rebase3, 1235, Rebase2, Rebase4, Rebase]");
    }
}
