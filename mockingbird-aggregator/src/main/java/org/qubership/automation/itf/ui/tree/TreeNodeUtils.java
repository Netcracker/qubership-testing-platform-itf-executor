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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;

import org.qubership.automation.itf.core.report.ReportObject;

import com.google.common.collect.Lists;

public class TreeNodeUtils {

    /**
     * To do conversion for list {@link TreeNodeUtils#getReportObject(java.lang.Object[])}.
     *
     * @return {@code {@link List<ReportObject>}}
     */
    public static List<ReportObject> getReportObjects(@Nonnull List<Object[]> objects) {
        ArrayList<ReportObject> list = Lists.newArrayList();
        objects.forEach(object -> list.add(getReportObject(object)));
        return list;
    }

    public static List<TreeNode> createTreeNodesByReport(List<ReportObject> reportObjects, String... neededTypes) {
        ArrayList<TreeNode> nodes = Lists.newArrayList();
        reportObjects = cutExcess(reportObjects, neededTypes);
        fillingTree(reportObjects, nodes);
        return nodes;
    }

    /**
     * This method converts an object with 8 fields into a normal POJO.
     * Unfortunately, my {@link ReportObject} class is not automatically filled
     * when I receive a native object request from the database,
     * so I made this method on WA
     * SqlResultSetMapping does not work. solve this problem if you know how
     *
     * @param object an object that has 8 fields
     * @return {@link ReportObject}
     */
    static ReportObject getReportObject(@Nonnull Object[] object) {
        if (object.length != 10) {
            throw new IllegalArgumentException("Object isn't correct: it has " + object.length + " elements, but 10 "
                    + "are required!");
        }
        //TODO It's hardcode. Need Use @SqlResultSetMapping in ReportObject
        ReportObject reportObject = new ReportObject();
        reportObject.setId((BigInteger) object[0]);
        reportObject.setParent((BigInteger) object[1]);
        reportObject.setType((String) object[2]);
        reportObject.setDescription((String) object[3]);
        reportObject.setPath((String) object[4]);
        reportObject.setLevel((Integer) object[5]);
        reportObject.setStatus((String) object[6]);
        reportObject.setDuration((String) object[7]);
        reportObject.setStartTime((String) object[8]);
        reportObject.setEndTime((String) object[9]);
        return reportObject;
    }

    static TreeNode buildNodeByReportObject(List<ReportObject> children, ReportObject reportObject,
                                            Integer parentLevel) {
        return new TreeNodeBuilder(reportObject.getId())
                .addText(reportObject.getDescription())
                .addStatus(reportObject.getStatus()).findAndAddChildNode(children, parentLevel + 1)
                .addTags(reportObject.getDuration()).addStartTime(reportObject.getStartTime())
                .addEndTime(reportObject.getEndTime()).build();
    }

    static void fillingTree(List<ReportObject> reportObjects, ArrayList<TreeNode> nodes) {
        Iterator<ReportObject> iterator = reportObjects.iterator();
        while (iterator.hasNext()) {
            ReportObject reportObject = iterator.next();
            iterator.remove();
            Integer parentLevel = reportObject.getLevel();
            if (parentLevel == 1) {
                nodes.add(buildNodeByReportObject(Lists.newArrayList(reportObjects), reportObject, parentLevel));
            }
        }
    }

    static List<ReportObject> cutExcess(List<ReportObject> reportObjects, String... correctTypes) {
        ArrayList<ReportObject> savedReportObjects = Lists.newArrayList();
        moveToSavedCollectionIfHasType(reportObjects, savedReportObjects, Lists.newArrayList(correctTypes));
        return savedReportObjects;
    }

    static void setCorrectParent(List<ReportObject> reportObjects, ReportObject oldParent) {
        for (ReportObject object : reportObjects) {
            if (object.getParent().equals(oldParent.getId())) {
                object.setParent(oldParent.getParent());
            }
            setCorrectLevel(object);
        }
    }

    static void moveToSavedCollectionIfHasType(List<ReportObject> reportObjects,
                                               List<ReportObject> savedReportObjects, List<String> types) {
        for (ReportObject reportObject : reportObjects) {
            if (types.contains(reportObject.getType())) {
                savedReportObjects.add(reportObject);
                continue;
            }
            setCorrectParent(reportObjects, reportObject);
        }
    }

    //TODO: I haven't more time and I do this is hardcode. If you need more element types refactor this. I create
    // ticket for refactoring it
    private static void setCorrectLevel(ReportObject object) {
        if (object.getType().equals(TreeNodeTypes.SITUATION_INSTANCE.toString())) {
            object.setLevel(1);
        } else if (object.getType().equals(TreeNodeTypes.STEP_INSTANCE.toString())) {
            object.setLevel(2);
        } else if (object.getType().equals(TreeNodeTypes.INCOMING_MESSAGE.toString())
                || object.getType().equals(TreeNodeTypes.OUTGOING_MESSAGE.toString())) {
            object.setLevel(3);
        }
    }
}
