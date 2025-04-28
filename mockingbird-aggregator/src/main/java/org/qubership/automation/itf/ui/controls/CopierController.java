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

import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.COPY_OBJECT_IS_SMART;
import static org.qubership.automation.itf.core.util.constants.ProjectSettingsConstants.COPY_OBJECT_IS_SMART_DEFAULT_VALUE;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.automation.itf.core.exceptions.operation.OperationException;
import org.qubership.automation.itf.core.hibernate.spring.managers.base.ObjectManager;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.environment.Environment;
import org.qubership.automation.itf.core.model.jpa.folder.ChainFolder;
import org.qubership.automation.itf.core.model.jpa.folder.Folder;
import org.qubership.automation.itf.core.model.jpa.folder.SystemFolder;
import org.qubership.automation.itf.core.model.jpa.message.parser.OperationParsingRule;
import org.qubership.automation.itf.core.model.jpa.message.parser.ParsingRule;
import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.model.jpa.server.Server;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.jpa.system.operation.Operation;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.util.copier.OriginalCopyMap;
import org.qubership.automation.itf.core.util.copier.SmartCopier;
import org.qubership.automation.itf.core.util.copier.StorableCopier;
import org.qubership.automation.itf.core.util.db.TxExecutor;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.executor.service.ProjectSettingsService;
import org.qubership.automation.itf.ui.controls.util.ControllerHelper;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.qubership.automation.itf.ui.messages.objects.UITreeElement;
import org.qubership.automation.itf.ui.messages.objects.copy.UICopyMove;
import org.qubership.automation.itf.ui.util.UIHelper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Transactional(readOnly = true)
@RestController
@RequestMapping(value = "/copier")
@RequiredArgsConstructor
@Slf4j
public class CopierController extends ControllerHelper {

    private static final String WARNING_UNEXPECTED_OPERATION = "Unexpected operation for copy/move object";
    private static final int OPERATION_COPY = 1;
    private static final int OPERATION_MOVE = 2;

    private final ProjectSettingsService projectSettingsService;

    /**
     * Originally, the method did the following:
     * - Assume the name is like "Simple business name"
     * - Return value is "Simple business name [2]"
     * - The name is like "Simple business name [2]"
     * - Return value is "Simple business name [3]" (extract integer inside [] and increment with 1)
     * But, due to History presentation needs,
     * some object names become like "Step[9164972305322218543]" (it's a callchain step)
     * We face NumberFormatException for them because 9164972305322218543 is out of Integer range.
     * For such names, extra parameter 'newId' is used, and
     * - Return value is "Step[9164972305322218544]" where number inside [] is replaced with a 'newId' value.
     */
    private static String appendIndexToField(String field, Object newId) {
        if (StringUtils.isBlank(field)) {
            return field;
        }
        Pattern indexPattern = Pattern.compile("\\[(\\d*)]$");
        Matcher matcher = indexPattern.matcher(field);
        StringBuilder result = new StringBuilder(field);
        if (matcher.find()) {
            String newIndexStr;
            try {
                int newIndex = Integer.parseInt(matcher.group(1)) + 1;
                newIndexStr = String.valueOf(newIndex);
            } catch (NumberFormatException ex) {
                newIndexStr = newId.toString();
            }
            result.replace(matcher.start(1), matcher.end(1), newIndexStr);
        } else {
            result.append(" [2]");
        }
        return result.toString();
    }

    /**
     * This method for copy core object. You must prepare JSON for request.
     * Example for request body:
     * {"srcId":"12636f3d-bc48-40a1-bbcb-5d3c60fa6053",
     * "dstId":"6d739a8b-2560-42c6-9468-10b67e8fb0ad",
     * "srcClassName":"org.qubership.automation.itf.transport.rest.outbound.RESTOutboundTransport",
     * "dstClassName":"org.qubership.automation.itf.core.system.System"
     * }
     *
     * @param request contain variables (srcId, dstId, srcClassName, dstClassName)
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"UPDATE\")")
    @RequestMapping(value = "/copyobject", method = RequestMethod.POST)
    @AuditAction(auditAction = "Copy selected objects in the project {{#projectUuid}}")
    public UIObject copyObject(@RequestBody UICopyMove request,
                               @SuppressWarnings("unused") @RequestParam(value = "projectUuid") UUID projectUuid) {
        try {
            return doCopyMoveOperationInTransaction(request, OPERATION_COPY);
        } catch (Exception e) {
            throw new OperationException("Copy", e.getMessage());
        }
    }

    /**
     * This method for move core object. You must prepare JSON for request.
     * Example for request body:
     * {"srcId":"12636f3d-bc48-40a1-bbcb-5d3c60fa6053",
     * "dstId":"6d739a8b-2560-42c6-9468-10b67e8fb0ad",
     * "srcClassName":"org.qubership.automation.itf.transport.rest.outbound.RESTOutboundTransport",
     * "dstClassName":"org.qubership.automation.itf.core.system.System"
     * }
     *
     * @param request contain variables (srcId, dstId, srcClassName, dstClassName)
     */
    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"UPDATE\")")
    @RequestMapping(value = "/moveobject", method = RequestMethod.POST)
    @AuditAction(auditAction = "Move selected objects in the project {{#projectUuid}}")
    public UIObject moveObject(@RequestBody UICopyMove request,
                               @SuppressWarnings("unused") @RequestParam(value = "projectUuid") UUID projectUuid) {
        try {
            return doCopyMoveOperationInTransaction(request, OPERATION_MOVE);
        } catch (Exception e) {
            throw new OperationException("Move", e.getMessage());
        }
    }

    @Transactional
    @PreAuthorize("@entityAccess.checkAccess(#projectUuid, \"READ\")")
    @RequestMapping(value = "/issmart", method = RequestMethod.GET)
    @AuditAction(auditAction = "Check if SmartCopy is turned on for project {{#projectId}}/{{#projectUuid}}")
    public boolean getIsSmartValue(@RequestParam(value = "projectId") String projectId,
                                   @SuppressWarnings("unused") @RequestParam(value = "projectUuid") UUID projectUuid) {
        return isSmartCopier(projectId);
    }

    private boolean isSmartCopier(String projectId) {
        return Boolean.parseBoolean(projectSettingsService.get(projectId, COPY_OBJECT_IS_SMART,
                COPY_OBJECT_IS_SMART_DEFAULT_VALUE));
    }

    private UIObject doCopyMoveOperationInTransaction(UICopyMove request, int operation) throws Exception {
        ObjectManager<StubProject> manager = CoreObjectManager.getInstance().getManager(StubProject.class);
        return TxExecutor.execute(() -> {
            try {
                manager.setReplicationRole("replica");
                return doCopyMoveOperation(request, operation);
            } catch (Exception e) {
                throw new IllegalArgumentException(getTopStackTrace(e));
            } finally {
                manager.setReplicationRole("origin");
            }
        }, TxExecutor.defaultWritableTransaction());
    }

    private UIObject doCopyMoveOperation(UICopyMove request, int operation) throws Exception {
        performPreValidationSteps(request);
        String error = validateRequest(request, operation);
        if (StringUtils.isEmpty(error)) {
            Storable destinationObject = CoreObjectManager.getInstance()
                    .getManager(Class.forName(request.getDestination().getClassName()).asSubclass(Storable.class))
                    .getById(request.getDestination().getId());
            throwExceptionIfNull(destinationObject,
                    request.getDestination().getName(),
                    request.getDestination().getId(),
                    Class.forName(request.getDestination().getClassName()).asSubclass(Storable.class),
                    "copy/move");
            List<Storable> copiedMovedObjects = Lists.newArrayList();
            String sessionId = UUID.randomUUID().toString();
            StorableCopier.setKeyAndValueMapCopyFlag(sessionId, request.getCopyFlag());
            List<? extends Storable> sourceObjects = UIHelper.initializeObjects(request.getSources());
            for (Storable source : sourceObjects) {
                Storable copiedMovedSource = performCopyMoveOperation(source, destinationObject, operation,
                        request.getOther(), sessionId, request.getProjectId());
                performPostActions(sessionId, operation, source, copiedMovedSource, request.getProjectId());
                copiedMovedObjects.add(copiedMovedSource);
            }
            destinationObject.store();
            destinationObject.flush();
            OriginalCopyMap.getInstance().clear(sessionId);
            return UIHelper.getUIPresentationByStorable(destinationObject,
                    Class.forName(request.getSources().get(0).getClassName()), copiedMovedObjects);
        } else {
            throw new IllegalArgumentException(error);
        }
    }

    private void performPostActions(String sessionId, int copyMoveOperation, Storable sourceObject,
                                    Storable copiedMovedSource, String projectId) {
        // Perform some post-actions against the copy of object itself and/or its subordinates, according to object type
        if (copyMoveOperation == OPERATION_COPY) {
            if (sourceObject instanceof Situation) {
                performPostCopyActions_Situation(sessionId, (Situation) sourceObject, (Situation) copiedMovedSource);
            } else if (sourceObject instanceof Operation) {
                for (Situation situation : ((Operation) sourceObject).getSituations()) {
                    Situation copiedSituation = (Situation) (OriginalCopyMap.getInstance().get(sessionId,
                            situation.getID()));
                    performPostCopyActions_Situation(sessionId, situation, copiedSituation);
                }
            } else if (sourceObject instanceof System) {
                for (Operation operation : ((System) sourceObject).getOperations()) {
                    for (Situation situation : operation.getSituations()) {
                        Situation copiedSituation = (Situation) (OriginalCopyMap.getInstance().get(sessionId,
                                situation.getID()));
                        performPostCopyActions_Situation(sessionId, situation, copiedSituation);
                    }
                }
            } else if (sourceObject instanceof Environment) {
                performPostCopyActions_Server(copiedMovedSource, projectId);
            }
        } else /* MOVE */ {
            if (sourceObject instanceof Situation) {
                performPostMoveActions_Situation((Situation) copiedMovedSource);
            } else if (sourceObject instanceof Operation) {
                for (Situation situation : ((Operation) sourceObject).getSituations()) {
                    performPostMoveActions_Situation(situation);
                }
            } else if (sourceObject instanceof System) {
                for (Operation operation : ((System) sourceObject).getOperations()) {
                    for (Situation situation : operation.getSituations()) {
                        performPostMoveActions_Situation(situation);
                    }
                }
            }
        }
    }

    private void performPostMoveActions_Situation(Situation movedSituation) {
        // Iterate through situation parsing rules.
        // Remove entry in case parent of situation is not the same as parent of parsing rule
        movedSituation.getParsingRules().removeIf(
                parsingRule -> !(parsingRule.getParent().getID().equals(movedSituation.getParent().getID()))
        );
    }

    private void performPostCopyActions_Situation(String sessionId, Situation sourceSituation,
                                                  Situation copiedSituation) {
        if (copiedSituation == null) {
            return;
        }
        if (sourceSituation.getParent().getID().equals(copiedSituation.getParent().getID())) {
            return;
        }
        if (sourceSituation.getParsingRules().isEmpty()) {
            return;
        }
        Set<OperationParsingRule> changedOperationParsingRules = new HashSet<>();
        for (ParsingRule parsingRule : sourceSituation.getParsingRules()) {
            OperationParsingRule copiedParsingRule = (OperationParsingRule) (OriginalCopyMap.getInstance()
                    .get(sessionId, parsingRule.getID()));
            if (copiedParsingRule != null) {
                changedOperationParsingRules.add(copiedParsingRule);
            }
        }
        copiedSituation.setParsingRules(changedOperationParsingRules);
    }

    private void performPostCopyActions_Server(Storable copiedSource, String projectId) {
        fillProjectIdFromServer(((Environment) copiedSource).getOutbound(), projectId);
        fillProjectIdFromServer(((Environment) copiedSource).getInbound(), projectId);
    }

    private void fillProjectIdFromServer(Map<System, Server> entrySource, String projectId) {
        for (Map.Entry<System, Server> entry : entrySource.entrySet()) {
            Server serverHB = entry.getValue();
            if (serverHB.getProjectId() == null) {
                serverHB.setProjectId(new BigInteger(projectId));
            }
        }
    }

    private Storable performCopyMoveOperation(Storable source, Storable destinationObject, int operation,
                                              JSONObject other, String sessionId, String projectId) throws Exception {
        ObjectManager<? extends Storable> objectManager =
                CoreObjectManager.getInstance().getManager(source.getClass().asSubclass(Storable.class));
        switch (operation) {
            case OPERATION_COPY: {
                Storable copiedSource = objectManager.copy(destinationObject, source, projectId, sessionId);
                if (isSmartCopier(projectId) && source.getClass().getName().endsWith("Environment")) {
                    if (Objects.nonNull(other)) {
                        copiedSource = SmartCopier.setAllValuesOnCopyStorable(copiedSource, source, other, projectId);
                    } else {
                        copiedSource.setName(appendIndexToField(copiedSource.getName(), copiedSource.getID()));
                    }
                } else {
                    copiedSource.setName(appendIndexToField(copiedSource.getName(), copiedSource.getID()));
                }
                return copiedSource;
            }
            case OPERATION_MOVE: {
                objectManager.move(destinationObject, source, sessionId);
                break;
            }
            default: {
                log.warn(WARNING_UNEXPECTED_OPERATION);
                break;
            }
        }
        return source;
    }

    private void performPreValidationSteps(UICopyMove copyMoveRequest) throws ClassNotFoundException {
        UIObject firstElement = copyMoveRequest.getSources().get(0);
        Storable firstSource = CoreObjectManager.getInstance()
                .getManager(Class.forName(firstElement.getClassName()).asSubclass(Storable.class))
                .getById(firstElement.getId());
        if (firstSource instanceof CallChain || firstSource instanceof ChainFolder || firstSource instanceof System
                || firstSource instanceof SystemFolder) {
            // Copy/move to rootFolder are allowed for callChains, chainFolders, systems and systemFolders.
            UIObject dest = copyMoveRequest.getDestination();
            Storable destinationObject;
            try {
                destinationObject = (dest.getId() == null) ? null : CoreObjectManager.getInstance()
                        .getManager(Class.forName(dest.getClassName()).asSubclass(Storable.class))
                        .getById(dest.getId());
            } catch (Exception ignore) {
                dest = null;
                destinationObject = null;
            }
            if (dest == null || !(destinationObject instanceof ChainFolder
                    || destinationObject instanceof SystemFolder)) {
                // Set dest to firstSource.parent if not null; otherwise - to root folder
                if (firstSource.getParent() != null) {
                    copyMoveRequest.setDestination(new UITreeElement(firstSource.getParent()));
                } else {
                    Storable rootFolder = null;
                    StubProject project = CoreObjectManager.getInstance().getManager(StubProject.class)
                            .getById(copyMoveRequest.getProjectId());
                    if (firstSource instanceof CallChain) {
                        rootFolder = project.getCallchains();
                    } else if (firstSource instanceof System) {
                        rootFolder = project.getSystems();
                    }
                    UITreeElement uiRootFolder = new UITreeElement(rootFolder);
                    uiRootFolder.setRoot(true);
                    copyMoveRequest.setDestination(uiRootFolder);
                }
            }
        } else if (firstSource instanceof Environment) {
            // Copy/move to rootFolder are allowed for Environments.
            UIObject dest = copyMoveRequest.getDestination();
            Storable destinationObject;
            try {
                destinationObject = (dest.getId() == null) ? null : CoreObjectManager.getInstance()
                        .getManager(Class.forName(dest.getClassName()).asSubclass(Storable.class))
                        .getById(dest.getId());
            } catch (Exception ignore) {
                dest = null;
                destinationObject = null;
            }
            if (dest == null || !(destinationObject instanceof Folder)) {
                // Set dest to firstSource.parent if not null; otherwise - to root folder
                if (firstSource.getParent() != null) {
                    copyMoveRequest.setDestination(new UITreeElement(firstSource.getParent()));
                } else {
                    Folder<Environment> rootFolder = CoreObjectManager.getInstance()
                            .getManager(StubProject.class).getById(copyMoveRequest.getProjectId()).getEnvironments();
                    UITreeElement uiRootFolder = new UITreeElement(rootFolder);
                    uiRootFolder.setRoot(true);
                    copyMoveRequest.setDestination(uiRootFolder);
                }
            }
        }
    }

    private String validateRequest(UICopyMove copyMoveRequest, int operationType) {
        String error = destinationIsNotNull(copyMoveRequest, operationType);
        destinationIsTheSameAsSource(copyMoveRequest);
        return error;
    }

    private String destinationIsNotNull(UICopyMove copyMoveRequest, int operationType) {
        if (copyMoveRequest.getDestination() == null) {
            return "Destination for " + ((operationType == OPERATION_COPY) ? "copied" : "moved")
                    + " objects wasn't selected. Please, select the destination.";
        }
        return StringUtils.EMPTY;
    }

    private void destinationIsTheSameAsSource(UICopyMove copyMoveRequest) {
        UIObject destination = copyMoveRequest.getDestination();
        for (UIObject source : copyMoveRequest.getSources()) {
            if (source.getId().equals(destination.getId()) && destination.getParent() != null) {
                copyMoveRequest.setDestination(destination.getParent());
            }
        }
    }
}
