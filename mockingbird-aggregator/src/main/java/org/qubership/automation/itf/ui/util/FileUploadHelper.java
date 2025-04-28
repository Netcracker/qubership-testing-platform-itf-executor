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

import java.io.InputStream;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.qubership.automation.itf.core.util.eds.model.FileEventType;
import org.qubership.automation.itf.core.util.eds.model.FileInfo;
import org.qubership.automation.itf.executor.service.ExecutorToMessageBrokerSender;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileUploadHelper {

    public static void checkStoredObjectIdAndSendMessageToExternalDataStorageUpdateTopic(ObjectId storedObjectId,
                                                                                         String fileName,
                                                                                         String filePath,
                                                                                         String contentType,
                                                                                         UUID projectUuid,
                                                                                         InputStream inputStream,
                                                                                         FileEventType eventType,
                                                                                         ExecutorToMessageBrokerSender executorToMessageBrokerSender, String tenantId) {
        log.info("Stored object id for file '{}' is {}null", fileName, (storedObjectId == null) ? "" : "not ");
        if (storedObjectId != null) {
            sendMessageToExternalDataStorageUpdateTopic(
                    storedObjectId, fileName, filePath, contentType,
                    projectUuid, null, eventType, executorToMessageBrokerSender, tenantId);
        } else {
            sendMessageToExternalDataStorageUpdateTopic(
                    null, fileName, filePath, contentType,
                    projectUuid, inputStream, eventType, executorToMessageBrokerSender, tenantId);
        }
    }

    public static void sendMessageToExternalDataStorageUpdateTopic(ObjectId objectId, String fileName, String filePath,
                                                                   String contentType, UUID projectUuid,
                                                                   InputStream inputStream, FileEventType eventType,
                                                                   ExecutorToMessageBrokerSender executorToMessageBrokerSender, String tenantId) {
        executorToMessageBrokerSender.sendMessageToExternalDataStorageUpdateTopic(
                new FileInfo(objectId, fileName, filePath, contentType, projectUuid, inputStream, eventType), tenantId);
    }
}
