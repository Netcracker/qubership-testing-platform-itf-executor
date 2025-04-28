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

package org.qubership.automation.itf.ui.tree;

import static org.junit.Assert.assertEquals;

import java.math.BigInteger;
import java.util.List;

import org.junit.Test;
import org.qubership.automation.itf.core.report.ReportObject;
import org.springframework.util.Assert;

import com.google.common.collect.Lists;

public class TreeNodeBuilderTest {

    @Test
    public void findAndAddChildNodeTest() {
        List<ReportObject> objects = Lists.newArrayList();
        ReportObject reportObject0 = new ReportObject(new BigInteger("60420003267"), new BigInteger("0"),
                "TcContext", "REST test [No Data Set]", "", 1, "PASSED", "01.069");
        objects.add(reportObject0);
        ReportObject reportObject1 = new ReportObject(new BigInteger("60420003271"), new BigInteger("60420003267"),
                "SituationInstance", "send", "", 2, "PASSED", "01.069");
        objects.add(reportObject1);
        ReportObject reportObject2 = new ReportObject(new BigInteger("60420003272"), new BigInteger("60420003271"),
                "InstanceContext", "send", "", 3, "PASSED", "01.069");
        objects.add(reportObject2);
        ReportObject reportObject3 = new ReportObject(new BigInteger("60420003273"), new BigInteger("60420003271"),
                "StepInstance", "send", "", 3, "PASSED", "01.069");
        objects.add(reportObject3);
        ReportObject reportObject4 = new ReportObject(new BigInteger("60420003274"), new BigInteger("60420003273"),
                "InstanceContext", "send", "", 4, "PASSED", "01.069");
        objects.add(reportObject4);
        ReportObject reportObject5 = new ReportObject(new BigInteger("60420003275"), new BigInteger("60420003274"),
                "SpContext", "send", "", 5, "PASSED", "01.069");
        objects.add(reportObject5);
        ReportObject reportObject6 = new ReportObject(new BigInteger("1133"), new BigInteger("60420003275"),
                "incoming message", "send", "", 6, "PASSED", "01.069");
        objects.add(reportObject6);
        ReportObject reportObject7 = new ReportObject(new BigInteger("1134"), new BigInteger("60420003275"),
                "outgoing message", "send", "", 6, "PASSED", "01.069");
        objects.add(reportObject7);
        TreeNode treeNode = new TreeNodeBuilder(new BigInteger("60420003267")).findAndAddChildNode(objects, 2).build();
        Assert.notEmpty(treeNode.getNodes());
        assertEquals("60420003271", treeNode.getNodes().stream().findFirst().get().getId());
        Assert.notEmpty(treeNode.getNodes().stream().findFirst().get().getNodes());
        assertEquals("60420003272",
                treeNode.getNodes().stream().findFirst().get().getNodes().stream().findFirst().get().getId());
        assertEquals(0,
                treeNode.getNodes().stream().findFirst().get().getNodes().stream().findFirst().get().getNodes().size());
    }

    @Test
    public void addStatus() {
    }
}
