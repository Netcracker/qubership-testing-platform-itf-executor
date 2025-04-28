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

package org.qubership.automation.itf.executor.cache.service.impl;

import static org.qubership.automation.itf.core.util.constants.CacheNames.ATP_ITF_RESPONSE_MESSAGES;

import java.util.concurrent.TimeUnit;

import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.nio.serialization.HazelcastSerializationException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ResponseCacheService {

    private HazelcastInstance hazelcastClient;

    @Autowired
    public void setHazelcastClient(@Qualifier("hazelcastClient") HazelcastInstance hazelcastClient) {
        this.hazelcastClient = hazelcastClient;
    }

    public Message getByKey(String key) {
        return getResponseCache().get(key);
    }

    /**
     * Set to ATP_ITF_REST_SOAP_RESPONSE_OBJECT cache.
     *
     * @param key       key to cache
     * @param stringTtl time to live as string.
     * @param message   {@link Message} response object.
     */
    public void set(String key, String stringTtl, Message message) {
        try {
            long ttl = Long.parseLong(stringTtl);
            getResponseCache().set(key, message, ttl, TimeUnit.SECONDS);
            log.debug("Message {} was set to {} cache.", key, ATP_ITF_RESPONSE_MESSAGES);
        } catch (HazelcastSerializationException ex) {
            log.error("Can't serialize Message to set into ATP_ITF_REST_SOAP_RESPONSE_OBJECT.", ex);
        } catch (Exception e) {
            log.error("Something went wrong while set Message to {} cache", ATP_ITF_RESPONSE_MESSAGES);
        }
    }

    public void evict(String key) {
        getResponseCache().evict(key);
    }

    private IMap<Object, Message> getResponseCache() {
        return hazelcastClient.getMap(ATP_ITF_RESPONSE_MESSAGES);
    }
}
