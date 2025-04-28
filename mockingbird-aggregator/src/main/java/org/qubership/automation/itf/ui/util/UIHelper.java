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

package org.qubership.automation.itf.ui.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.hibernate.spring.managers.base.ObjectManager;
import org.qubership.automation.itf.core.model.common.LabeledStorable;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.context.JsonContext;
import org.qubership.automation.itf.core.model.jpa.folder.ChainFolder;
import org.qubership.automation.itf.core.model.jpa.folder.EnvFolder;
import org.qubership.automation.itf.core.model.jpa.folder.SystemFolder;
import org.qubership.automation.itf.core.model.jpa.message.parser.ParsingRule;
import org.qubership.automation.itf.core.model.jpa.message.template.Template;
import org.qubership.automation.itf.core.model.jpa.project.StubProject;
import org.qubership.automation.itf.core.model.jpa.step.EmbeddedStep;
import org.qubership.automation.itf.core.model.jpa.step.IntegrationStep;
import org.qubership.automation.itf.core.model.jpa.step.SituationStep;
import org.qubership.automation.itf.core.model.jpa.step.Step;
import org.qubership.automation.itf.core.model.jpa.system.System;
import org.qubership.automation.itf.core.model.jpa.system.operation.Operation;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.model.jpa.transport.TransportConfiguration;
import org.qubership.automation.itf.core.util.TemplateHelper;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.ui.messages.UIList;
import org.qubership.automation.itf.ui.messages.UIListImpl;
import org.qubership.automation.itf.ui.messages.UIObjectList;
import org.qubership.automation.itf.ui.messages.UITreeElementList;
import org.qubership.automation.itf.ui.messages.UITypeList;
import org.qubership.automation.itf.ui.messages.exception.UIException;
import org.qubership.automation.itf.ui.messages.objects.UIDataSet;
import org.qubership.automation.itf.ui.messages.objects.UIDataSetParametersGroup;
import org.qubership.automation.itf.ui.messages.objects.UIKey;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.qubership.automation.itf.ui.messages.objects.UIOperation;
import org.qubership.automation.itf.ui.messages.objects.UIParsingRule;
import org.qubership.automation.itf.ui.messages.objects.UISituation;
import org.qubership.automation.itf.ui.messages.objects.UIStep;
import org.qubership.automation.itf.ui.messages.objects.UISystem;
import org.qubership.automation.itf.ui.messages.objects.UITreeElement;
import org.qubership.automation.itf.ui.messages.objects.UITypedObject;
import org.qubership.automation.itf.ui.messages.objects.callchain.UICallChain;
import org.qubership.automation.itf.ui.messages.objects.callchain.step.UIEmbeddedChainStep;
import org.qubership.automation.itf.ui.messages.objects.callchain.step.UISituationStep;
import org.qubership.automation.itf.ui.messages.objects.environment.UIEnvironmentFolder;
import org.qubership.automation.itf.ui.messages.objects.parents.UIIdentifiedObject;
import org.qubership.automation.itf.ui.messages.objects.transport.UITransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class UIHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(UIHelper.class);
    private static final Function<Storable, UIObject> STORABLE_TO_UIOBJ = new Function<Storable, UIObject>() {
        @Nullable
        @Override
        public UIObject apply(@Nullable Storable input) {
            return input == null ? null : toUIObj(input);
        }
    };
    private static final Function<Storable, UITreeElement> STORABLE_TO_UITREE =
            new Function<Storable, UITreeElement>() {
                @Nullable
                @Override
                public UITreeElement apply(@Nullable Storable input) {
                    return input == null ? null : toUITree(input);
                }
            };
    private static final Map<Class<? extends Storable>, Class<? extends UIObject>> storableUIMapping;

    static {
        storableUIMapping = new HashMap<>();
        storableUIMapping.put(Situation.class, UISituation.class);
        storableUIMapping.put(SituationStep.class, UISituationStep.class);
        storableUIMapping.put(EmbeddedStep.class, UIEmbeddedChainStep.class);
        storableUIMapping.put(TransportConfiguration.class, UITransport.class);
        storableUIMapping.put(System.class, UISystem.class);
        storableUIMapping.put(CallChain.class, UICallChain.class);
        storableUIMapping.put(Template.class, UITreeElement.class);
        storableUIMapping.put(ParsingRule.class, UIParsingRule.class);
        storableUIMapping.put(Operation.class, UIOperation.class);
        storableUIMapping.put(ChainFolder.class, UITreeElement.class);
        storableUIMapping.put(SystemFolder.class, UITreeElement.class);
        storableUIMapping.put(EnvFolder.class, UIEnvironmentFolder.class);
    }

    public static void conversionOfTree(Collection<UITreeElement> list, Class<? extends Storable> clazz) {
        UIObjectList deleteObjects = new UIObjectList();
        for (UITreeElement object : list) {
            object.setParent(null);
            if (!clazz.getName().equals(object.getClassName())) {
                object.setIsFolder(true);
            }
            object.addListChildrenIfExists(list, clazz);
            if (object.getFolder() != null) {
                if (!object.getFolder().getClassName().equals(StubProject.class.getCanonicalName())) {
                    deleteObjects.addObject(object);
                }
            }
        }
        if (deleteObjects.getObjects() != null) {
            list.removeAll(deleteObjects.getObjects());
        }
    }

    @Nullable
    public static ImmutableList isNotNullCopyOfImmutableList(Collection collection) {
        return (collection == null) ? null : ImmutableList.copyOf(collection);
    }

    public static void saveSteps(List<UIStep> steps, String id) {
        if (steps == null) {
            return;
        }
        ObjectManager<IntegrationStep> stepObjectManager =
                CoreObjectManager.getInstance().getManager(IntegrationStep.class);
        ObjectManager<System> systemObjectManager = CoreObjectManager.getInstance().getManager(System.class);
        ObjectManager<Operation> operationObjectManager = CoreObjectManager.getInstance().getManager(Operation.class);
        List<Step> situationSteps = new LinkedList<>();
        for (UIStep entry : steps) {
            Step step = stepObjectManager.create(null, IntegrationStep.TYPE);
            step.setName(entry.getName());
            step.setDelay(entry.getDelay());
            step.setUnit(entry.getUnit());
            step.setEnabled(!"No".equalsIgnoreCase(entry.getEnabled()));
            step.setManual(!"No".equalsIgnoreCase(entry.getManual()));
            String objectId = (entry.getSender() == null) ? null : entry.getSender().getId();
            ((IntegrationStep) step).setSender((objectId == null) ? null : systemObjectManager.getById(objectId));
            objectId = (entry.getReceiver() == null) ? null : entry.getReceiver().getId();
            ((IntegrationStep) step).setReceiver((objectId == null) ? null : systemObjectManager.getById(objectId));
            objectId = (entry.getOperation() == null) ? null : entry.getOperation().getId();
            ((IntegrationStep) step).setOperation((objectId == null) ? null : operationObjectManager.getById(objectId));
            objectId = (entry.getTemplate() == null) ? null : entry.getTemplate().getId();
            ((IntegrationStep) step).setTemplate((objectId == null) ? null : TemplateHelper.getById(objectId));
            situationSteps.add(step);
        }
        Situation situation = CoreObjectManager.getInstance().getManager(Situation.class).getById(id);
        if (situation != null) {
            setParent(situation, situationSteps);
            situation.fillSteps(situationSteps);
            situation.store();
        }
    }

    private static void setParent(Storable parent, List<Step> steps) {
        for (Step step : steps) {
            step.setParent(parent);
        }
    }

    @Nullable
    protected static List<UIKey> getUIKeys(@Nullable Collection<String> keys) {
        if (keys == null) {
            return null;
        }
        List<UIKey> uiKeys = Lists.newArrayListWithExpectedSize(keys.size());
        for (String key : keys) {
            UIKey uiKey = new UIKey();
            uiKey.setKey(key);
            uiKeys.add(uiKey);
        }
        return uiKeys;
    }

    public static String getDefinitionValue(String keyDefinition) {
        return (keyDefinition == null) ? StringUtils.EMPTY : keyDefinition;
    }

    /**
     * Creates UIObjectList with provided storables.
     *
     * @param from storables
     * @return created UIObjectList with storables
     */
    @Nonnull
    public static UIObjectList getObjectList(@Nonnull Collection<? extends Storable> from) {
        return fillObjectList(from, new UIObjectList());
    }

    /**
     * Appends storables into existing UIObjectList.
     *
     * @param from storables
     * @param to   existing UIObjectList
     * @return the same UIObjectList
     */
    @Nonnull
    public static UIObjectList fillObjectList(@Nonnull Collection<? extends Storable> from, @Nonnull UIObjectList to) {
        fillUIList(from, to, toUIObj());
        return to;
    }

    /**
     * Creates UITreeElementList with provided storables.
     *
     * @param from storables
     * @return created UITreeElementList with storables
     */
    @Nonnull
    public static UITreeElementList getTreeElementList(@Nonnull Collection<? extends Storable> from) {
        return fillTreeElementList(from, new UITreeElementList());
    }

    /**
     * Apappends storables into existing UITreeElementList.
     *
     * @param from storables
     * @param to   existing UITreeElementList
     * @return the same UITreeElementList
     */
    @Nonnull
    protected static UITreeElementList fillTreeElementList(
            @Nonnull Collection<? extends Storable> from, @Nonnull UITreeElementList to) {
        fillUIList(from, to, toUITree());
        return to;
    }

    /**
     * Creates UIList with provided content using func to transform values.
     *
     * @see #fillUIList(Collection, UIList, Function)
     */
    @Nonnull
    protected static <I extends Storable, O extends UIObject> UIList<O> getUIList(
            @Nonnull Collection<I> from, @Nonnull Function<? super I, O> func) {
        return fillUIList(from, new UIListImpl<O>(), func);
    }

    /**
     * puts storables from collection into UIList.
     * transforms storables using provided func;
     *
     * @param from collection of storables or its subclasses
     * @param to   UIList of UIObjects or its subclasses
     * @param func transforms provided storables into UIObjects
     * @param <I>  actual type of storables
     * @param <O>  actual type of UIObjects
     * @return
     */
    @Nonnull
    protected static <I extends Storable, O extends UIObject> UIList<O> fillUIList(
            @Nonnull Collection<I> from, @Nonnull UIList<O> to, @Nonnull Function<? super I, O> func) {
        to.defineObjects(Collections2.transform(from, func));
        return to;
    }

    //region privateUIObjCreators
    //TODO SZ: is it necessary creation of method instead of use the constant?
    private static Function<Storable, UIObject> toUIObj() {
        return STORABLE_TO_UIOBJ;
    }

    //TODO SZ: is it necessary creation of method instead of use the constant?
    private static Function<Storable, UITreeElement> toUITree() {
        return STORABLE_TO_UITREE;
    }

    @Nonnull
    private static UIObject toUIObj(@Nonnull Storable input) {
        UIObject uiObject = new UIObject(input);
        uiObject.setClassName(null);
        // We should send to UI the information about parent object (if any) but... we should be aware of the amount
        // of data sent,
        //  which can be very big in case when we place the whole object here
        if (input.getParent() == null) {
            uiObject.setParent(null);
        } else {
            UIObject uiParentObject = new UIObject();
            uiParentObject.setName(input.getParent().getName());
            uiParentObject.setClassName(input.getParent().getClass().getName());
            uiParentObject.setId(input.getParent().getID().toString());
            uiObject.setParent(uiParentObject);
        }
        return uiObject;
    }

    //TODO SZ: is it necessary creation of method instead of just call new UITreeElement?
    @Nonnull
    private static UITreeElement toUITree(@Nonnull Storable input) {
        //TODO SZ: so, why we can't do that in function.apply()?
        return new UITreeElement(input);
    }

    public static UIObject getUIPresentationByStorable(Storable storable, Class childClass, List<Storable> children) {
        Class<? extends UIObject> uiClass = storableUIMapping.get(storable.getClass());
        if (uiClass != null) {
            String error;
            Exception exception;
            try {
                Constructor<? extends UIObject> constructor;
                try {
                    constructor = uiClass.getConstructor(storable.getClass());
                } catch (NoSuchMethodException e) {
                    constructor = uiClass.getConstructor(Storable.class);
                }
                UIObject uiPresentation = constructor.newInstance(storable);
                if (childClass != null) {
                    uiPresentation.loadChildrenByClass(childClass, children);
                }
                return uiPresentation;
            } catch (NoSuchMethodException e) {
                error = String.format("Cannot find the constructor(Storable storable) for the %s",
                        storable.getClass().getName());
                exception = e;
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                error = String.format("Cannot create the UI-presentation for the %s", storable.getName());
                exception = e;
            }
            LOGGER.error(error, exception);
            throw new UIException(error);
        } else {
            String error = String.format("UI Mapping for the %s is not set", storable.getName());
            LOGGER.error(error);
            throw new UIException(error);
        }
    }

    /**
     * Update basic field on Storable.
     *
     * @param object   has current fields
     * @param storable in which you want to update the fields
     */
    public static void updateObject(UIObject object, Storable storable) {
        storable.setDescription(object.getDescription());
        storable.setName(object.getName());
        if (storable instanceof LabeledStorable) {
            LabeledStorable labeledStorable = (LabeledStorable) storable;
            labeledStorable.getLabels().clear();
            if (object.getLabels() != null) {
                labeledStorable.getLabels().addAll(object.getLabels());
            }
        }
    }

    /**
     * Convert map to UITypeList.
     *
     * @param map target map
     * @return UITypeList
     */
    public static UITypeList convertMapOfTypeToUITypeList(Map<String, String> map) {
        UITypeList uiTypeList = new UITypeList();
        uiTypeList.defineTypes(Collections2.transform(map.entrySet(), new Function<Map.Entry<String, String>,
                UITypedObject>() {
            @Nonnull
            @Override
            public UITypedObject apply(Map.Entry<String, String> input) {
                UITypedObject uiTypedObject = new UITypedObject();
                uiTypedObject.setType(input.getKey());
                uiTypedObject.setName(input.getValue());
                return uiTypedObject;
            }
        }));
        return uiTypeList;
    }

    /**
     * Initializes objects from UI views.
     *
     * @param sources UI objects
     * @return list of storables
     */
    public static List<Storable> initializeObjects(Collection<UIObject> sources) throws Exception {
        List<Storable> result = new ArrayList<>();
        for (UIIdentifiedObject source : sources) {
            String sourceId = source.getId();
            String sourceClassName = source.getClassName();
            result.add(CoreObjectManager.getInstance()
                    .getManager(Class.forName(sourceClassName).asSubclass(Storable.class)).getById(sourceId));
        }
        return result;
    }

    @Nullable
    protected JsonContext toJSONContext(UIDataSet dataSet) {
        final JsonContext context = new JsonContext();
        Set<UIDataSetParametersGroup> groups = dataSet.getDataSetParametersGroup();
        if (groups == null) {
            return null;
        }
        // This method should be modified if we implement support of:
        //  1) parameters without groups
        //  2) nested groups
        //  and extra: currently we add one group programmatically in the
        //  /org/qubership/automation/itf/ui/controls/DatasetController.java#readDataset
        //      this group name is "Autogenerated_No_Group". It is needed to UI purposes (Run callchain window,
        //      pencil button)
        //  may be it should be modified too and/or this group should be removed here
        //
        //  Alexander Kapustin, 2017-12-27
        groups.forEach(group -> {
            if (!StringUtils.isBlank(group.getName())) {
                context.put(group.getName().trim(), new JsonContext());
                group.getDataSetParameter().forEach(parameter -> {
                    if (!StringUtils.isBlank(parameter.getDisplayedName())) {
                        context.put(group.getName().trim() + '.' + parameter.getDisplayedName().trim(),
                                parameter.getDisplayedValue());
                    }
                });
            }
        });
        return context;
    }
}
