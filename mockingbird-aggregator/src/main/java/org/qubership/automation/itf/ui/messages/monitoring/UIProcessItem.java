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

import java.util.Date;

import org.qubership.automation.itf.core.model.jpa.instance.AbstractInstance;
import org.qubership.automation.itf.core.util.constants.Status;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.qubership.automation.itf.ui.service.TimeService;

public class UIProcessItem extends UIObject {

    private Status status = Status.NOT_STARTED;
    private Date startTime;
    private Date endTime;
    private long duration = 0L;

    public UIProcessItem() {
    }

    public UIProcessItem(AbstractInstance instance) {
        super(instance);
        AbstractInstance a = instance.getParent();
        this.status = instance.getStatus();
        this.startTime = instance.getStartTime();
        this.endTime = instance.getEndTime();
        if (this.startTime != null && this.endTime != null) {
            this.duration = this.endTime.getTime() - this.startTime.getTime();
        }
    }

    public String getStatus() {
        return status.toString();
    }

    public String getStartTime() {
        return TimeService.getFormattedDate(startTime);
    }

    public String getEndTime() {
        return TimeService.getFormattedDate(this.endTime);
    }

    public String getDuration() {
        return String.format("%.3f (s)", duration / 1000.0);
    }

    @Override
    public UIProcessItem getParent() {
        return (UIProcessItem) super.getParent();
    }
}
