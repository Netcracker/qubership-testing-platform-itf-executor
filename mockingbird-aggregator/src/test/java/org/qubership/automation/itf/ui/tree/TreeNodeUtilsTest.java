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

import java.math.BigInteger;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.qubership.automation.itf.core.report.ReportObject;

import com.google.common.collect.Lists;

public class TreeNodeUtilsTest extends TreeNodeUtils {

    @Ignore
    @Test
    public void getReportObjectTest() {
        Object[] objects = new Object[8];
        objects[6] = "TEST";
        ReportObject reportObject = getReportObject(objects);
        Assert.assertEquals("TEST", reportObject.getStatus());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getReportObjectExceptionTest() {
        Object[] objects = new Object[7];
        getReportObject(objects);
    }

    @Test
    public void cutExcessTest() {
        List<ReportObject> objects = Lists.newArrayList();
        ReportObject reportObject0 = new ReportObject(new BigInteger("60420003267"), new BigInteger("0"),
                "TcContext", "REST test [No Data Set]", "", 1, "PASSED", "01.069");
        objects.add(reportObject0);
        ReportObject reportObject1 = new ReportObject(new BigInteger("60420003271"), new BigInteger("60420003267"),
                "SituationInstance", "send", "", 2, "PASSED", "01.069");
        objects.add(reportObject1);
        ReportObject reportObject2 = new ReportObject(new BigInteger("60420003272"), new BigInteger("60420003271"),
                "InstanceContext", "send", "", 2, "PASSED", "01.069");
        objects.add(reportObject2);
        ReportObject reportObject3 = new ReportObject(new BigInteger("60420003273"), new BigInteger("60420003271"),
                "StepInstance", "send", "", 2, "PASSED", "01.069");
        objects.add(reportObject3);
        ReportObject reportObject4 = new ReportObject(new BigInteger("60420003274"), new BigInteger("60420003273"),
                "InstanceContext", "send", "", 2, "PASSED", "01.069");
        objects.add(reportObject4);
        ReportObject reportObject5 = new ReportObject(new BigInteger("60420003275"), new BigInteger("60420003274"),
                "SpContext", "send", "", 2, "PASSED", "01.069");
        objects.add(reportObject5);
        ReportObject reportObject6 = new ReportObject(new BigInteger("1133"), new BigInteger("60420003275"),
                "incoming message", "send", "", 2, "PASSED", "01.069");
        objects.add(reportObject6);
        ReportObject reportObject7 = new ReportObject(new BigInteger("1134"), new BigInteger("60420003275"),
                "outgoing message", "send", "", 2, "PASSED", "01.069");
        objects.add(reportObject7);
        objects = cutExcess(objects, TreeNodeTypes.SITUATION_INSTANCE.toString(),
                TreeNodeTypes.STEP_INSTANCE.toString());
        Assert.assertEquals(2, objects.size());
    }

    @Test
    public void setCorrectMethod() {
        List<ReportObject> objects = Lists.newArrayList();
        ReportObject reportObject0 = new ReportObject(new BigInteger("60420003267"), new BigInteger("0"),
                "TcContext", "REST test [No Data Set]", "", 1, "PASSED", "01.069");
        objects.add(reportObject0);
        ReportObject reportObject1 = new ReportObject(new BigInteger("60420003271"), new BigInteger("60420003267"),
                "SituationInstance", "send", "", 2, "PASSED", "01.069");
        objects.add(reportObject1);
        ReportObject reportObject2 = new ReportObject(new BigInteger("60420003272"), new BigInteger("60420003271"),
                "InstanceContext", "send", "", 2, "PASSED", "01.069");
        objects.add(reportObject2);
        setCorrectParent(objects, reportObject1);
        Assert.assertEquals(new BigInteger("60420003267"), reportObject2.getParent());
    }

    @Test
    public void moveToSecondIfHasStatusTest() {
        List<ReportObject> objects = Lists.newArrayList();
        List<ReportObject> savedObjects = Lists.newArrayList();
        ReportObject reportObject0 = new ReportObject(new BigInteger("60420003267"), new BigInteger("0"),
                "TcContext", "REST test [No Data Set]", "", 1, "PASSED", "01.069");
        objects.add(reportObject0);
        ReportObject reportObject1 = new ReportObject(new BigInteger("60420003271"), new BigInteger("60420003267"),
                "SituationInstance", "send", "", 2, "PASSED", "01.069");
        objects.add(reportObject1);
        ReportObject reportObject2 = new ReportObject(new BigInteger("60420003272"), new BigInteger("60420003271"),
                "InstanceContext", "send", "", 2, "PASSED", "01.069");
        objects.add(reportObject2);
        ReportObject reportObject3 = new ReportObject(new BigInteger("60420003273"), new BigInteger("60420003272"),
                "StepInstance", "send", "", 2, "PASSED", "01.069");
        objects.add(reportObject3);
        moveToSavedCollectionIfHasType(objects, savedObjects,
                Lists.newArrayList(TreeNodeTypes.STEP_INSTANCE.toString()));
        Assert.assertEquals(1, savedObjects.size());
//        Assert.assertEquals(1, savedObjects.stream().findFirst().get().getParent());
    }

    @Test
    public void createTreeNodesByReportTest() {
        List<ReportObject> objects = Lists.newArrayList();
        ReportObject reportObject0 = new ReportObject(new BigInteger("60420003267"), new BigInteger("0"),
                "TcContext", "REST test [No Data Set]", "", 1, "PASSED", "01.069");
        objects.add(reportObject0);
        ReportObject reportObject1 = new ReportObject(new BigInteger("60420003271"), new BigInteger("60420003267"),
                "SituationInstance", "send1", "", 2, "PASSED", "01.069");
        objects.add(reportObject1);
        ReportObject reportObject2 = new ReportObject(new BigInteger("60420003272"), new BigInteger("60420003271"),
                "InstanceContext", "send2", "", 3, "PASSED", "01.069");
        objects.add(reportObject2);
        ReportObject reportObject3 = new ReportObject(new BigInteger("60420003273"), new BigInteger("60420003271"),
                "StepInstance", "send3", "", 3, "PASSED", "01.069");
        objects.add(reportObject3);
        ReportObject reportObject4 = new ReportObject(new BigInteger("60420003274"), new BigInteger("60420003273"),
                "InstanceContext", "send4", "", 4, "PASSED", "01.069");
        objects.add(reportObject4);
        ReportObject reportObject5 = new ReportObject(new BigInteger("60420003275"), new BigInteger("60420003274"),
                "SpContext", "send5", "", 5, "PASSED", "01.069");
        objects.add(reportObject5);
        ReportObject reportObject6 = new ReportObject(new BigInteger("1133"), new BigInteger("60420003275"),
                "incoming message", "send6", "", 6, "PASSED", "01.069");
        objects.add(reportObject6);
        ReportObject reportObject7 = new ReportObject(new BigInteger("1134"), new BigInteger("60420003275"),
                "outgoing message", "send7", "", 6, "PASSED", "01.069");
        objects.add(reportObject7);
        List<TreeNode> treeNodesByReport = TreeNodeUtils.createTreeNodesByReport(objects,
                TreeNodeTypes.SITUATION_INSTANCE.toString(), TreeNodeTypes.STEP_INSTANCE.toString(),
                TreeNodeTypes.OUTGOING_MESSAGE.toString());

        Assert.assertEquals(1, treeNodesByReport.size());
//        Assert.assertTrue(Objects.nonNull(treeNodesByReport.stream().findFirst().get().getNodes().stream()
//        .findFirst().get().getOutgoingMessage()));


    }


    @Test
    public void createTreeNodesByReportTest2() {
        List<ReportObject> objects = Lists.newArrayList();
        ReportObject reportObject0 = new ReportObject(new BigInteger("60420003267"), new BigInteger("0"),
                "TcContext", "REST test [No Data Set]", "", 1, "PASSED", "01.069");
        objects.add(reportObject0);
        ReportObject reportObject1 = new ReportObject(new BigInteger("60420003271"), new BigInteger("60420003267"),
                "SituationInstance", "send1", "", 2, "PASSED", "01.069");
        objects.add(reportObject1);
        ReportObject reportObject2 = new ReportObject(new BigInteger("60420003272"), new BigInteger("60420003271"),
                "InstanceContext", "send2", "", 3, "PASSED", "01.069");
        objects.add(reportObject2);
        ReportObject reportObject3 = new ReportObject(new BigInteger("60420003273"), new BigInteger("60420003271"),
                "StepInstance", "send3", "", 3, "PASSED", "01.069");
        objects.add(reportObject3);
        ReportObject reportObject4 = new ReportObject(new BigInteger("60420003274"), new BigInteger("60420003273"),
                "InstanceContext", "send4", "", 4, "PASSED", "01.069");
        objects.add(reportObject4);
        ReportObject reportObject5 = new ReportObject(new BigInteger("60420003275"), new BigInteger("60420003274"),
                "SpContext", "send5", "", 5, "PASSED", "01.069");
        objects.add(reportObject5);
        ReportObject reportObject6 = new ReportObject(new BigInteger("1133"), new BigInteger("60420003275"),
                "incoming message", "send6", "", 6, "PASSED", "01.069");
        objects.add(reportObject6);
        ReportObject reportObject7 = new ReportObject(new BigInteger("1134"), new BigInteger("60420003275"),
                "outgoing message", "send7", "", 6, "PASSED", "01.069");
        objects.add(reportObject7);
        ReportObject reportObject8 = new ReportObject(new BigInteger("62420003271"), new BigInteger("60420003267"),
                "SituationInstance", "send1", "", 2, "PASSED", "01.069");
        objects.add(reportObject8);
        ReportObject reportObject9 = new ReportObject(new BigInteger("62420003272"), new BigInteger("62420003271"),
                "InstanceContext", "send2", "", 3, "PASSED", "01.069");
        objects.add(reportObject9);
        ReportObject reportObject10 = new ReportObject(new BigInteger("62420003273"), new BigInteger("62420003271"),
                "StepInstance", "send3", "", 3, "PASSED", "01.069");
        objects.add(reportObject10);
        ReportObject reportObject11 = new ReportObject(new BigInteger("62420003274"), new BigInteger("62420003273"),
                "InstanceContext", "send4", "", 4, "PASSED", "01.069");
        objects.add(reportObject11);
        ReportObject reportObject12 = new ReportObject(new BigInteger("62420003275"), new BigInteger("62420003274"),
                "SpContext", "send5", "", 5, "PASSED", "01.069");
        objects.add(reportObject12);
        ReportObject reportObject13 = new ReportObject(new BigInteger("51133"), new BigInteger("62420003275"),
                "incoming message", "send6", "", 6, "PASSED", "01.069");
        objects.add(reportObject13);
        ReportObject reportObject14 = new ReportObject(new BigInteger("51134"), new BigInteger("62420003275"),
                "outgoing message", "send7", "", 6, "PASSED", "01.069");
        objects.add(reportObject14);
        List<TreeNode> treeNodesByReport = TreeNodeUtils.createTreeNodesByReport(objects,
                TreeNodeTypes.SITUATION_INSTANCE.toString(), TreeNodeTypes.STEP_INSTANCE.toString(),
                TreeNodeTypes.OUTGOING_MESSAGE.toString(), TreeNodeTypes.INCOMING_MESSAGE.toString());

        Assert.assertEquals(2, treeNodesByReport.size());
//        Assert.assertTrue(Objects.nonNull(treeNodesByReport.stream().findFirst().get().getNodes().stream()
//        .findFirst().get().getOutgoingMessage()));


    }

}
