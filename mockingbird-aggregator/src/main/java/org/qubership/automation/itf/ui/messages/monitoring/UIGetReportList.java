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

package org.qubership.automation.itf.ui.messages.monitoring;

import java.util.List;

import org.qubership.automation.itf.ui.messages.objects.UIReportItem;

import com.google.common.collect.Lists;

public class UIGetReportList {

    private int totalPages = 0;   // totalPages - not in the 'reportItems' but in the query results
    private long totalItems = 0L; // totalItems - not in the 'reportItems' but in the query results
    private List<UIReportItem> reportItems = Lists.newArrayList();

    public List<UIReportItem> getReportItems() {
        return reportItems;
    }

    public void setReportItems(List<UIReportItem> reportItems) {
        this.reportItems = reportItems;
    }

    public int getTotalPages() {
        return this.totalPages;
    }

    public void setTotalPages(int pagesCount) {
        this.totalPages = pagesCount;
    }

    public long getTotalItems() {
        return this.totalItems;
    }

    public void setTotalItems(long itemsCount) {
        this.totalItems = itemsCount;
    }
}
