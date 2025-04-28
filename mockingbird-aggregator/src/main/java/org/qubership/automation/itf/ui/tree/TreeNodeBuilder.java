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
import java.util.Iterator;
import java.util.List;

import org.qubership.automation.itf.core.report.ReportObject;

import com.google.common.collect.Lists;

public class TreeNodeBuilder {

    private BigInteger id;
    private String text;
    private String color; //based on status
    private List<TreeNode> nodes = Lists.newArrayList();
    private List<String> tags = Lists.newArrayList();
    private String startTime;
    private String endTime;

    public TreeNodeBuilder(BigInteger nodeId) {
        this.id = nodeId;
    }

    public TreeNode build() {
        TreeNode node = new TreeNode();
        node.setId(this.id.toString());
        node.setText(this.text);
        node.setBackColor(this.color);
        node.setNodes(this.nodes);
        node.setTags(this.tags);
        node.setStartTime(this.startTime);
        node.setEndTime(this.endTime);
        return node;
    }

    TreeNodeBuilder addText(String text) {
        this.text = text;
        return this;
    }

    TreeNodeBuilder findAndAddChildNode(List<ReportObject> children, int level) {
        Iterator<ReportObject> iterator = children.iterator();
        while (iterator.hasNext()) {
            ReportObject reportObject = iterator.next();
            if (reportObject.getParent().equals(this.id) && level == reportObject.getLevel()) {
                iterator.remove();
                setChildren(Lists.newArrayList(children), reportObject, level);
            }
        }
        return this;
    }

    private void setChildren(List<ReportObject> children, ReportObject reportObject, int level) {
        this.nodes.add(TreeNodeUtils.buildNodeByReportObject(Lists.newArrayList(children), reportObject, level));
    }

    TreeNodeBuilder addTags(String... tags) {
        this.tags.addAll(Lists.newArrayList(tags));
        return this;
    }

    TreeNodeBuilder addStartTime(String startTime) {
        this.startTime = startTime;
        return this;
    }

    TreeNodeBuilder addEndTime(String endTime) {
        this.endTime = endTime;
        return this;
    }

    TreeNodeBuilder addStatus(String status) {
        if ("PASSED".equals(status)) {
            this.color = "#dff0d8";
        } else if ("FAILED".equals(status)) {
            this.color = "#f2dede";
        }
        return this;
    }
}
