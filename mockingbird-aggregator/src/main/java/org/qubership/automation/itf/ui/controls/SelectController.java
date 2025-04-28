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

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.util.db.TxExecutor;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.qubership.automation.itf.ui.messages.UIList;
import org.qubership.automation.itf.ui.messages.UIObjectList;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@RestController
public class SelectController extends ControllerHelper {

    private static final LoadingCache<String, UIList<UIObject>> CACHE =
            CacheBuilder.newBuilder()
                    .expireAfterWrite(30, TimeUnit.SECONDS).build(new CacheLoader<String, UIList<UIObject>>() {
        @Override
        public UIList<UIObject> load(@Nonnull String className) throws Exception {
            UIList<UIObject> uiObjects = new UIObjectList();
            Class<? extends Storable> aClass = Class.forName(className).asSubclass(Storable.class);
            for (Storable storable : CoreObjectManager.getInstance().getManager(aClass).getAll()) {
                if (aClass.isAssignableFrom(storable.getClass())) {
                    uiObjects.addObject(new UIObject(storable));
                }
            }
            return uiObjects;
        }
    });

    //todo It seems don't using...check it and delete this controller
    @Transactional(readOnly = true)
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/select/options", method = RequestMethod.GET)
    @AuditAction(auditAction = "Get list of '{{#className}}' objects from project {{#projectId}}")
    public UIList getList(
            @RequestParam(value = "className", defaultValue = "") final String className,
            @RequestParam(value = "projectUuid") UUID projectUuid) throws Exception {
        return TxExecutor.execute((Callable<UIList>) () -> {
            UIList<UIObject> uiList;
            uiList = CACHE.get(className);
            return uiList == null ? new UIObjectList() : uiList;
        });
    }
}
