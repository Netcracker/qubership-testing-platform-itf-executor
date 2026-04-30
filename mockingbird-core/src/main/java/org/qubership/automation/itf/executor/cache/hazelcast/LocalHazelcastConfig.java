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

package org.qubership.automation.itf.executor.cache.hazelcast;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

@Configuration
@Import(CommonHazelcastConfig.class)
@ConditionalOnProperty(value = "hazelcast.cache.enabled", havingValue = "false")
public class LocalHazelcastConfig {

    /**
     * Create {@link HazelcastInstance} bean.
     *
     * @return bean
     */
    @Bean(name = "hazelcastClient")
    public HazelcastInstance hazelcastInstance() {
        Config config = new Config();
        config.setClusterName("local-itf-hazelcast-cluster");
        config.setInstanceName("local-itf-hc-cache-instance");
        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(config);
        CommonHazelcastConfig.tryToCreateMapConfigsIfNotExist(hazelcastInstance, false);
        return hazelcastInstance;
    }
}
