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

package org.qubership.automation.itf.ui.controls;

import static org.qubership.automation.itf.core.util.constants.InstanceSettingsConstants.LOG_APPENDER_DATE_FORMAT;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Locale;
import java.util.UUID;

import javax.annotation.Nullable;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.model.jpa.context.TcContext;
import org.qubership.automation.itf.core.util.config.Config;
import org.qubership.automation.itf.core.util.constants.Status;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.qubership.automation.itf.ui.messages.objects.UITCContext;
import org.qubership.automation.itf.ui.messages.objects.UITCContextList;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

@RestController
public class TCContextController extends ControllerHelper {

    private static final String DATE_FORMAT = Config.getConfig().getString(LOG_APPENDER_DATE_FORMAT);
    private static final DateTimeFormatter dateTimeFormatter =
            DateTimeFormatter.ofPattern(DATE_FORMAT).withZone(ZoneId.systemDefault()).withLocale(Locale.ENGLISH);

    /*  This method uses unbelievably incorrect way of retrieving ALL tcContexts with status != IN_PROGRESS:
     *    .getAll(), then filter
     *    It is a normal way only for developers' small databases.
     *    Moreover, I can not imagine for what purposes one may want to retrieve such a list.
     *    TODO: check if this endpoint is used from anywhere. If YES - refactor it, otherwise - remove it...
     */
    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/starter/tccontexts", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get TCContexts in the project {{#projectId}} [Deprecated, to be deleted]")
    public UITCContextList getTcContexts(@RequestParam(value = "projectUuid") UUID projectUuid) {
        UITCContextList uiContexts = new UITCContextList();
        Collection<? extends TcContext> stoppedItems =
                Collections2.filter(CoreObjectManager.getInstance().getManager(TcContext.class).getAll(),
                        (Predicate<TcContext>) input -> input != null && !Status.IN_PROGRESS.equals(input.getStatus()));
        Collection<UITCContext> contexts = Collections2.transform(stoppedItems, new Function<TcContext, UITCContext>() {
            @Nullable
            @Override
            public UITCContext apply(TcContext input) {
                UITCContext uiTcContext = new UITCContext();
                uiTcContext.setName(String.format("%s %s [%s]", input.getName(),
                        input.getStartTime().toInstant().atZone(ZoneId.systemDefault()).format(dateTimeFormatter),
                        input.getStatus().toString()));
                uiTcContext.setId(input.getID().toString());
                return uiTcContext;
            }
        });
        uiContexts.setTccontexts(Lists.newArrayList(contexts));
        return uiContexts;
    }
}
