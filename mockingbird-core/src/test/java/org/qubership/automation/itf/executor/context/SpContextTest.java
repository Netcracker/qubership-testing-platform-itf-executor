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

package org.qubership.automation.itf.executor.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.qubership.automation.itf.core.model.jpa.context.SpContext;
import org.qubership.automation.itf.core.model.jpa.message.parser.MessageParameter;

public class SpContextTest {
    private static final MessageParameter MESSAGE_PARAMETER = mock(MessageParameter.class);
    private SpContext context;

    @Before
    public void setUp() {
        context = new SpContext();
        List<MessageParameter> messageParams = Collections.singletonList(MESSAGE_PARAMETER);
        context.putMessageParameters(messageParams);
    }

    @Test
    public void getMessageParameters() {
        assertFalse(context.getMessageParameters().isEmpty());
    }

    @Test
    public void putMessageParameters() {
        MessageParameter mock = mock(MessageParameter.class);
        List<MessageParameter> messageParams = Collections.singletonList(mock);
        System.gc();
        context.putMessageParameters(messageParams);
        assertEquals(2, context.getMessageParameters().size());
        assertEquals(MESSAGE_PARAMETER, context.getMessageParameters().get(0));
        assertEquals(mock, context.getMessageParameters().get(1));
    }
}
