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

package org.qubership.automation.itf.ui.messages.objects.callchain;

import java.util.List;

import org.qubership.automation.itf.core.model.common.Storable;
import org.qubership.automation.itf.core.model.dataset.DataSetList;
import org.qubership.automation.itf.core.model.jpa.callchain.CallChain;
import org.qubership.automation.itf.core.model.jpa.step.EmbeddedStep;
import org.qubership.automation.itf.core.model.jpa.step.SituationStep;
import org.qubership.automation.itf.core.model.jpa.step.Step;
import org.qubership.automation.itf.ui.messages.objects.UIDataSetList;
import org.qubership.automation.itf.ui.messages.objects.UIKey;
import org.qubership.automation.itf.ui.messages.objects.UIObject;
import org.qubership.automation.itf.ui.messages.objects.callchain.step.UIAbstractCallChainStep;
import org.qubership.automation.itf.ui.messages.objects.callchain.step.UIEmbeddedChainStep;
import org.qubership.automation.itf.ui.messages.objects.callchain.step.UISituationStep;
import org.qubership.automation.itf.ui.messages.objects.wrap.UIWrapper;
import org.qubership.automation.itf.ui.util.UIHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class UICallChain extends UIObject {

    private static final Logger LOGGER = LoggerFactory.getLogger(UICallChain.class);
    private UIWrapper<List<UIAbstractCallChainStep>> steps;
    private UIWrapper<List<UIKey>> keys;
    private UIWrapper<List<UIDataSetList>> dataSetLists;

    public UICallChain() {
    }

    public UICallChain(Storable callchain) {
        this((CallChain) callchain);
    }

    public UICallChain(CallChain chain) {
        super(chain);
    }

    public UICallChain(CallChain chain, boolean isFull) {
        super(chain, isFull);

        try {
            List<UIDataSetList> dataSetLists = Lists.newArrayList();
            for (Storable child : chain.getCompatibleDataSetLists(chain.getProjectId())) {
                dataSetLists.add(new UIDataSetList((DataSetList) child));
            }
            setDataSetLists(new UIWrapper<>(dataSetLists));
        } catch (Exception e) {
            LOGGER.warn("Exception occurred during getting compatible dataset lists: ", e);
        }

        List<UIKey> keys = Lists.newArrayList();
        for (String child : chain.getKeys()) {
            keys.add(new UIKey(child));
        }
        setKeys(new UIWrapper<>(keys));

        UIWrapper<List<UIAbstractCallChainStep>> wrapper = new UIWrapper<>();
        wrapper.setData(Lists.newArrayList());
        for (Step step : chain.getSteps()) {
            if (step instanceof SituationStep) {
                SituationStep situationStep = (SituationStep) step;
                wrapper.getData().add(new UISituationStep(situationStep, isFull));
            } else if (step instanceof EmbeddedStep) {
                EmbeddedStep embeddedStep = (EmbeddedStep) step;
                wrapper.getData().add(new UIEmbeddedChainStep(embeddedStep, isFull));
            }
        }
        setSteps(wrapper);
    }

    public UIWrapper<List<UIAbstractCallChainStep>> getSteps() {
        return steps;
    }

    public void setSteps(UIWrapper<List<UIAbstractCallChainStep>> steps) {
        this.steps = steps;
    }

    public UIWrapper<List<UIKey>> getKeys() {
        return keys;
    }

    public void setKeys(UIWrapper<List<UIKey>> keys) {
        this.keys = keys;
    }

    public UIWrapper<List<UIDataSetList>> getDataSetLists() {
        return dataSetLists;
    }

    public void setDataSetLists(UIWrapper<List<UIDataSetList>> dataSetLists) {
        this.dataSetLists = dataSetLists;
    }

    public void loadChildrenByClass(Class childClass, List<Storable> children) {
        if (Step.class.isAssignableFrom(childClass)) {
            List<UIAbstractCallChainStep> uiSteps = Lists.newArrayList();
            for (Storable child : children) {
                uiSteps.add((UIAbstractCallChainStep) UIHelper.getUIPresentationByStorable(child, null,
                        Lists.newArrayList()));
            }
            setSteps(new UIWrapper<>(uiSteps));
        }
    }
}
