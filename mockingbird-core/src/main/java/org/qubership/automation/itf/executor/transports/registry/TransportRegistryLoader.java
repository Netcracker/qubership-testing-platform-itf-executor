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

package org.qubership.automation.itf.executor.transports.registry;

import java.rmi.RemoteException;

import javax.annotation.Nonnull;

import org.qubership.automation.itf.core.util.config.ApplicationConfig;
import org.qubership.automation.itf.core.util.exception.ExportException;
import org.qubership.automation.itf.core.util.exception.NoDeployedTransportException;
import org.qubership.automation.itf.core.util.provider.MeansCommunication;
import org.qubership.automation.itf.core.util.transport.access.AccessTransport;
import org.qubership.automation.itf.core.util.transport.base.InboundTransport;
import org.qubership.automation.itf.core.util.transport.base.OutboundTransport;
import org.qubership.automation.itf.core.util.transport.loader.LoaderInboundTransportImpl;
import org.qubership.automation.itf.core.util.transport.loader.LoaderOutboundTransportImpl;
import org.qubership.automation.itf.core.util.transport.registry.base.AbstractTransportRegistry;
import org.qubership.automation.itf.executor.transports.classloader.TransportClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class TransportRegistryLoader extends AbstractTransportRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransportRegistryLoader.class);
    private static final String TRANSPORT_FOLDER = "transport.folder";
    private static final String TRANSPORT_LIB = "transport.lib";

    private TransportClassLoader loader;
    private final LoadingCache<String, AccessTransport> transportCache = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, AccessTransport>() {
                @Override
                public AccessTransport load(@Nonnull String transportType) throws Exception {
                    MeansCommunication instanceClass = loader.getInstanceClass(transportType);
                    if (instanceClass instanceof InboundTransport) {
                        return new LoaderInboundTransportImpl((InboundTransport) instanceClass);
                    } else if (instanceClass instanceof OutboundTransport) {
                        return new LoaderOutboundTransportImpl((OutboundTransport) instanceClass);
                    } else {
                        throw new NoDeployedTransportException(transportType + " is not loaded");
                    }
                }
            });

    @Override
    public void init() throws ExportException {
        if (loader == null) {
            loader = TransportClassLoader.getInstance();
            LOGGER.info("Transport registry is ready");
        }
        loader.load(ApplicationConfig.env.getProperty(TRANSPORT_FOLDER),
                ApplicationConfig.env.getProperty(TRANSPORT_LIB));
    }

    @Override
    protected void protectedRegister(AccessTransport accessTransport) {
    }

    @Override
    protected void protectedUnregister(String s) throws RemoteException {
    }

    @Override
    protected AccessTransport protectedFind(String typeName) throws RemoteException {
        try {
            return (isLoaded())
                    ? transportCache.getIfPresent(typeName)
                    : transportCache.get(typeName);
        } catch (Exception e) {
            throw new RemoteException("", e);
        }
    }

    @Override
    public void destroy() {
        loader.cleanClassLoaders();
    }
}
