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

package org.qubership.automation.itf.ui.controls.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.qubership.automation.itf.core.exceptions.common.ObjectNotFoundException;
import org.qubership.automation.itf.core.hibernate.spring.managers.base.ObjectManager;
import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.condition.parameter.ConditionParameter;
import org.qubership.automation.itf.core.model.interceptor.Interceptor;
import org.qubership.automation.itf.core.model.jpa.interceptor.InterceptorParams;
import org.qubership.automation.itf.core.model.jpa.interceptor.TemplateInterceptor;
import org.qubership.automation.itf.core.model.jpa.interceptor.TransportConfigurationInterceptor;
import org.qubership.automation.itf.core.model.jpa.message.parser.ParsingRule;
import org.qubership.automation.itf.core.model.jpa.message.template.Template;
import org.qubership.automation.itf.core.model.jpa.system.stub.EventTrigger;
import org.qubership.automation.itf.core.model.jpa.system.stub.OperationEventTrigger;
import org.qubership.automation.itf.core.model.jpa.system.stub.Situation;
import org.qubership.automation.itf.core.model.jpa.system.stub.SituationEventTrigger;
import org.qubership.automation.itf.core.model.jpa.transport.TransportConfiguration;
import org.qubership.automation.itf.core.util.TemplateHelper;
import org.qubership.automation.itf.core.util.constants.Condition;
import org.qubership.automation.itf.core.util.constants.Etc;
import org.qubership.automation.itf.core.util.holder.ActiveInterceptorHolder;
import org.qubership.automation.itf.core.util.manager.CoreObjectManager;
import org.qubership.automation.itf.core.util.provider.InterceptorProvider;
import org.qubership.automation.itf.ui.messages.objects.UIParsingRule;
import org.qubership.automation.itf.ui.messages.objects.eventtrigger.UICondition;
import org.qubership.automation.itf.ui.messages.objects.eventtrigger.UIEventTrigger;
import org.qubership.automation.itf.ui.messages.objects.parents.UIIdentifiedObject;
import org.qubership.automation.itf.ui.messages.objects.transport.UIProperty;
import org.qubership.automation.itf.ui.messages.objects.transport.interceptor.UIInterceptor;
import org.qubership.automation.itf.ui.util.EventTriggerHelper;
import org.qubership.automation.itf.ui.util.UIHelper;

import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ControllerHelper extends UIHelper {

    public static final Gson GSON = new GsonBuilder().create();

    /**
     * return object for id from repository.
     *
     * @param id     object identifier
     * @param uClazz type(class) object
     * @param <U>    generic object
     * @return object for id
     */
    public static <U extends Storable> U get(String id, Class<U> uClazz) {
        U object = getManager(uClazz).getById(id);
        throwExceptionIfNull(object, null, id, uClazz);
        return object;
    }

    public static <U extends Storable> Set<U> remove(Collection<U> source, List<? extends UIIdentifiedObject> ids) {
        return remove(source, Collections2.transform(ids, UIIdentifiedObject::getId));
    }

    public static <U extends Storable> Set<U> remove(Collection<U> source, String... ids) {
        return remove(source, Arrays.asList(ids));
    }

    public static <U extends Storable> Set<U> remove(Collection<U> source, Collection<String> ids) {
        if (ids == null) {
            return Collections.emptySet();
        }
        Set<U> objectToRemove = Sets.newHashSetWithExpectedSize(ids.size());
        for (U sourceObject : source) {
            for (String id : ids) {
                if (sourceObject.getID().toString().equals(id)) {
                    sourceObject.remove();
                    objectToRemove.add(sourceObject);
                }
            }
        }
        if (!objectToRemove.isEmpty()) {
            source.removeAll(objectToRemove);
        }
        return objectToRemove;
    }

    public static <U extends Storable> ObjectManager<U> getManager(Class<U> clazz) {
        return CoreObjectManager.managerFor(clazz);
    }


    public static void throwExceptionIfNull(Storable storable, String name, String id, final Class type) {
        throwExceptionIfNull(storable, name, id, type, null);
    }

    public static void throwExceptionIfNull(Storable storable, String name, String id, final Class type,
                                            String operation) {
        if (storable == null) {
            throw new ObjectNotFoundException(type.getSimpleName(), id, name, operation);
        }
    }

    public static void throwExceptionIfNull(Storable storable, String name, String id, String type) {
        //throwExceptionIfNull(storable, name, id, type, null);
    }

    public static String getName(Storable storable) {
        if (storable != null) {
            return storable.getName();
        }
        return "[Not Defined]";
    }

    public static Map<String, String> interceptorParamsToMap(UIInterceptor uiInterceptor) {
        Map<String, String> result = new HashMap<>();
        for (UIProperty param : uiInterceptor.getParameters()) {
            result.put(param.getName(), param.getValue());
        }
        return result;
    }

    public static InterceptorProvider getInterceptorProvider(String providerId) {
        InterceptorProvider interceptorProvider = (InterceptorProvider) TemplateHelper.getById(providerId);
        if (interceptorProvider == null) {
            interceptorProvider = CoreObjectManager.getInstance().getManager(TransportConfiguration.class)
                    .getById(providerId);
        }
        ControllerHelper.throwExceptionIfNull(interceptorProvider, "", providerId, InterceptorProvider.class);
        return interceptorProvider;
    }

    public static int getInterceptorsMaxOrder(Collection<Interceptor> interceptors) {
        int maxOrder = 0;
        for (Interceptor interceptor : interceptors) {
            int interceptorOrder = interceptor.getOrder();
            if (interceptorOrder > maxOrder) {
                maxOrder = interceptorOrder;
            }
        }
        return maxOrder;
    }

    public static Interceptor createInterceptorByProvider(InterceptorProvider interceptorProvider) {
        if (interceptorProvider instanceof Template) {
            return CoreObjectManager.getInstance().getManager(TemplateInterceptor.class).create();
        } else {
            return CoreObjectManager.getInstance().getManager(TransportConfigurationInterceptor.class).create();
        }
    }

    public static Interceptor findInterceptorByIdAndProvider(String id, InterceptorProvider provider) {
        if (provider instanceof Template) {
            return CoreObjectManager.getInstance().getManager(TemplateInterceptor.class).getById(id);
        } else {
            return CoreObjectManager.getInstance().getManager(TransportConfigurationInterceptor.class).getById(id);
        }
    }

    public static void fillInterceptorParams(Interceptor interceptor, InterceptorProvider interceptorProvider,
                                             UIInterceptor uiInterceptor) {
        interceptor.setName(uiInterceptor.getName());
        interceptor.setParent(interceptorProvider);
        interceptor.setTypeName(uiInterceptor.getClassName());
        interceptor.setActive(uiInterceptor.isActive());
        interceptor.setTransportName(uiInterceptor.getTransportName());
        interceptor.setInterceptorGroup(uiInterceptor.getInterceptorGroup());
    }

    public static void addInterceptorConfiguration(Interceptor interceptor, String transportName,
                                                   Map<String, String> configuration) {
        List<InterceptorParams> paramsList = interceptor.getInterceptorParams();
        InterceptorParams parameters = paramsList.isEmpty() ? new InterceptorParams() : paramsList.get(0);
        parameters.setParent(interceptor);
        parameters.setTransportName(transportName);
        parameters.update(configuration);
        if (paramsList.isEmpty()) {
            paramsList.add(parameters);
        }
    }

    public static boolean IsInGroup(Class<?> interceptor, String interceptorGroup) {
        Class<?> parentClass = interceptor.getSuperclass();
        return parentClass.getSimpleName().equals(interceptorGroup);
    }

    public static void reactivateInterceptor(Map<String, Interceptor> objectInterceptorMap, Interceptor interceptor,
                                             String providerId) {
        if (objectInterceptorMap == null) {
            objectInterceptorMap = new HashMap<>();
            objectInterceptorMap.put(interceptor.getID().toString(), interceptor);
            ActiveInterceptorHolder.getInstance().getActiveInterceptors().put(providerId, objectInterceptorMap);
        } else {
            objectInterceptorMap.put(interceptor.getID().toString(), interceptor);
        }
    }

    public static void addEventTriggers(Collection<UIEventTrigger> uiEventTriggers, Situation parent) {
        Collection<UIEventTrigger> triggersToAdd = Collections2.filter(uiEventTriggers,
                input -> StringUtils.isBlank(input.getId()));
        for (UIEventTrigger uiEventTrigger : triggersToAdd) {
            EventTrigger trigger = EventTriggerHelper.create(parent, uiEventTrigger.getType());
            uiEventTrigger.fillTrigger(trigger);
            if (trigger instanceof OperationEventTrigger) {
                parent.getOperationEventTriggers().add((OperationEventTrigger) trigger);
            } else {
                parent.getSituationEventTriggers().add((SituationEventTrigger) trigger);
            }
        }
    }

    public static List<ConditionParameter> toConditionParameters(List<UICondition> conditions) {
        int orderId = 1;
        List<ConditionParameter> conditionParameters = new ArrayList<>();
        for (UICondition condition : conditions) {
            ConditionParameter conditionParameter = new ConditionParameter();
            conditionParameter.setName(condition.getName());
            conditionParameter.setOrderId(orderId++);
            if (condition.getCondition() != null) {
                conditionParameter.setCondition(Condition.valueOf(condition.getCondition()));
            }
            conditionParameter.setValue(condition.getValue());
            if (StringUtils.isNotBlank(condition.getEtc())) {
                conditionParameter.setEtc(Etc.valueOf(condition.getEtc()));
            }
            conditionParameters.add(conditionParameter);
        }
        return conditionParameters;
    }

    public static void validateParsingRule(UIParsingRule uiParsingRule, ParsingRule parsingRule) {
        if (StringUtils.isBlank(uiParsingRule.getName()) || StringUtils.isBlank(uiParsingRule.getExpression())) {
            throw new IllegalArgumentException(
                    "Expression and Name of parsing rule must be filled. Old name: '"
                            + parsingRule.getName() + "', old expression: " + parsingRule.getExpression());
        }
    }

    /*
     *   Often we miss exception messages faced inside a method called via TxExecutor.
     *   As a rule, we cover them like throw new IllegalArgumentException(e.getMessage());
     *   It's too rood.
     *   This method is an attempt to transfer to UI enough information about exception.
     * */
    public String getTopStackTrace(Throwable ex) {
        String[] array = ExceptionUtils.getRootCauseStackTrace(ex);
        StringBuilder msg = new StringBuilder();
        for (int i = 0; i < Math.min(4, array.length); i++) {
            msg.append(array[i]).append("\n");
        }
        return msg.toString();
    }
}
