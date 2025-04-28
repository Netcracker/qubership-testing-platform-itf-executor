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

package org.qubership.automation.itf.core.util;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.hibernate.spring.managers.base.ObjectManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.ByProject;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.LabeledObjectManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.custom.SearchByProjectIdManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.executor.OperationTemplateObjectManager;
import org.qubership.automation.itf.core.hibernate.spring.managers.executor.SystemTemplateObjectManager;
import org.qubership.automation.itf.core.model.jpa.message.template.OperationTemplate;
import org.qubership.automation.itf.core.model.jpa.message.template.SystemTemplate;
import org.qubership.automation.itf.core.model.jpa.message.template.Template;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.jpa.system.operation.Operation;
import org.qubership.automation.itf.core.util.converter.IdConverter;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.provider.TemplateProvider;

public class TemplateHelper {

    public static Collection<Template<? extends TemplateProvider>> getByProjectId(BigInteger projectId) {
        Collection<Template<? extends TemplateProvider>> templates = CoreObjectManager.getInstance()
                .getSpecialManager(SystemTemplate.class, SearchByProjectIdManager.class)
                .getByProjectId(projectId);
        templates.addAll(
                CoreObjectManager.getInstance()
                        .getSpecialManager(OperationTemplate.class, SearchByProjectIdManager.class)
                        .getByProjectId(projectId)
        );
        return templates;
    }

    public static Template<? extends TemplateProvider> getById(Object id) {
        OperationTemplate template = CoreObjectManager.getInstance()
                .getSpecialManager(OperationTemplate.class, OperationTemplateObjectManager.class)
                .getByIdOnly(IdConverter.toBigInt(id));
        return template != null ? template : CoreObjectManager.getInstance()
                .getSpecialManager(SystemTemplate.class, SystemTemplateObjectManager.class)
                .getByIdOnly(IdConverter.toBigInt(id));
    }

    public static Template<? extends TemplateProvider> getById(Object id, String parentClazz) {
        if (StringUtils.isEmpty(parentClazz)) {
            return getById(id);
        } else if (parentClazz.endsWith(".Operation")) {
            return CoreObjectManager.getInstance()
                    .getSpecialManager(OperationTemplate.class, OperationTemplateObjectManager.class)
                    .getByIdOnly(IdConverter.toBigInt(id));
        } else if (parentClazz.endsWith(".System")) {
            return CoreObjectManager.getInstance()
                    .getSpecialManager(SystemTemplate.class, SystemTemplateObjectManager.class)
                    .getByIdOnly(IdConverter.toBigInt(id));
        } else {
            throw new IllegalArgumentException("Unknown parent type of the Template [" + id + "]: " + parentClazz);
        }
    }

    public static Collection<Template<? extends TemplateProvider>> getByPieceOfNameAndProject(String name,
                                                                                              BigInteger projectId) {
        Collection<Template<? extends TemplateProvider>> templates = CoreObjectManager.getInstance()
                .getSpecialManager(SystemTemplate.class, ByProject.class)
                .getByPieceOfNameAndProject(name, projectId);
        templates.addAll(
                CoreObjectManager.getInstance().getSpecialManager(OperationTemplate.class, ByProject.class)
                        .getByPieceOfNameAndProject(name, projectId)
        );
        return templates;
    }

    /**
     * Search templates by name (exact search) under project (by id).
     * The 1st search is made against OperationTemplates,
     * the 2nd search is made against SystemTemplates.
     * Combined collection is returned.
     *
     * @param name      - name of template to search,
     * @param projectId - id of project to search.
     * @return collection of templates found.
     */
    public static Collection<Template<? extends TemplateProvider>> getByNameAndProjectId(String name,
                                                                                         BigInteger projectId) {
        Collection<Template<? extends TemplateProvider>> templates = CoreObjectManager.getInstance()
                .getSpecialManager(SystemTemplate.class, ByProject.class)
                .getByNameAndProjectId(name, projectId);
        templates.addAll(
                CoreObjectManager.getInstance().getSpecialManager(OperationTemplate.class, ByProject.class)
                        .getByNameAndProjectId(name, projectId)
        );
        return templates;
    }

    /**
     * Search templates by name (exact search) under project (by id).
     * Contrary to 'getByNameAndProjectId' above method,
     * search is stopped if the 1st part (OperationTemplates) is found successfully.
     * It's enough for #load_part Velocity directive.
     *
     * @param name      - name of template to search,
     * @param projectId - id of project to search.
     * @return collection of templates found.
     */
    public static Collection<Template<? extends TemplateProvider>> getFirstPartByNameAndProjectId(
            String name,
            BigInteger projectId) {
        Collection<Template<? extends TemplateProvider>> templates = CoreObjectManager.getInstance()
                .getSpecialManager(OperationTemplate.class, ByProject.class)
                .getByNameAndProjectId(name, projectId);
        return templates.isEmpty()
                ? CoreObjectManager.getInstance()
                .getSpecialManager(SystemTemplate.class, ByProject.class)
                .getByNameAndProjectId(name, projectId)
                : templates;
    }

    public static Collection<Template<? extends TemplateProvider>> getByParentNameAndProject(String parentName,
                                                                                             BigInteger projectId) {
        Collection<Template<? extends TemplateProvider>> templates = CoreObjectManager.getInstance()
                .getSpecialManager(SystemTemplate.class, ByProject.class).getByParentNameAndProject(parentName,
                        projectId);
        templates.addAll(
                CoreObjectManager.getInstance().getSpecialManager(OperationTemplate.class, ByProject.class)
                        .getByParentNameAndProject(parentName, projectId)
        );
        return templates;
    }

    public static TemplateProvider getParent(Object parentId) {
        TemplateProvider parent = CoreObjectManager.getInstance().getManager(System.class).getById(parentId);
        if (Objects.isNull(parent)) {
            parent = CoreObjectManager.getInstance().getManager(Operation.class).getById(parentId);
        }
        return parent;
    }

    public static ObjectManager<? extends Template<? extends TemplateProvider>> getManagerByParent(
            TemplateProvider parent) {
        return parent instanceof System
                ? CoreObjectManager.getInstance().getManager(SystemTemplate.class)
                : CoreObjectManager.getInstance().getManager(OperationTemplate.class);
    }

    public static Set<String> getAllLabels(BigInteger projectId) {
        return CoreObjectManager.getInstance().getSpecialManager(SystemTemplate.class,
                LabeledObjectManager.class).getAllLabels(projectId);
    }
}
