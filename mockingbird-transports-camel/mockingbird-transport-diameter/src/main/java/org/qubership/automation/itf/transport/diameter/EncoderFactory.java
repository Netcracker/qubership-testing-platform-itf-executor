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

package org.qubership.automation.itf.transport.diameter;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.diameter.data.Encoder;
import org.qubership.automation.diameter.data.encoder.XmlEncoder;
import org.qubership.automation.diameter.data.encoder.wireshark.WireSharkEncoder;
import org.qubership.automation.diameter.dictionary.DictionaryConfig;

public class EncoderFactory {

    private static final Map<String, Class<? extends Encoder>> STORAGE = new HashMap<>();
    private static final EncoderFactory ENCODER_FACTORY = new EncoderFactory();

    private EncoderFactory() {
        STORAGE.put("XML", XmlEncoder.class);
        STORAGE.put("Wireshark", WireSharkEncoder.class);
    }

    public static EncoderFactory getInstance() {
        return ENCODER_FACTORY;
    }

    /**
     * Create new instance of Encoder. Can be {@link XmlEncoder} or {@link WireSharkEncoder}.
     *
     * @param format           "XML" or "Wireshark" string
     * @param dictionaryConfig config for encoder to work with correct dictionary. For the correct working it should be
     *                         the same that you use to read dictionary (ConfigReader.read(dictionaryConfig))
     * @return new instance of {@link XmlEncoder} or {@link WireSharkEncoder}
     */
    public Encoder getEncoder(String format, @Nonnull DictionaryConfig dictionaryConfig) {
        if (StringUtils.isBlank(format)) {
            throw new IllegalArgumentException("Invalid encoder format : " + format);
        }
        Class<? extends Encoder> encoder = STORAGE.computeIfAbsent(format, (key) -> {
            throw new IllegalArgumentException("Invalid type of encode: " + key);
        });
        boolean isXml = encoder.getSimpleName().equals(XmlEncoder.class.getSimpleName());
        return isXml
                ? new XmlEncoder(dictionaryConfig)
                : new WireSharkEncoder(dictionaryConfig);
    }
}
