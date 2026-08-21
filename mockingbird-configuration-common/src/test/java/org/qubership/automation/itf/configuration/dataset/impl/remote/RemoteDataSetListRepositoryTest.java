/*
 * # Copyright 2024-2026 NetCracker Technology Corporation
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

package org.qubership.automation.itf.configuration.dataset.impl.remote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.qubership.atp.datasets.dto.DataSetDto;
import org.qubership.atp.datasets.dto.DataSetListCreatedModifiedViewDto;
import org.qubership.atp.datasets.dto.VisibilityAreaFlatModelDto;
import org.qubership.automation.itf.core.model.dataset.DataSetList;
import org.qubership.automation.itf.core.model.dataset.DataSetListsSource;
import org.qubership.automation.itf.core.model.dataset.IDataSet;
import org.qubership.automation.itf.core.model.jpa.folder.Folder;
import org.qubership.automation.itf.core.util.config.ApplicationConfig;
import org.qubership.automation.itf.core.util.feign.http.HttpClientFactory;
import org.qubership.automation.itf.core.util.feign.impl.DatasetsAttributeFeignClient;
import org.qubership.automation.itf.core.util.feign.impl.DatasetsDatasetListFeignClient;
import org.qubership.automation.itf.core.util.feign.impl.DatasetsVisibilityAreaFeignClient;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RemoteDataSetListRepository Tests")
public class RemoteDataSetListRepositoryTest {

    @Mock
    private Folder<DataSetListsSource> folder;

    @Mock
    private DatasetsVisibilityAreaFeignClient visibilityAreaFeignClient;

    @Mock
    private DatasetsDatasetListFeignClient datasetListFeignClient;

    private RemoteDataSetListRepository repository;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {

        // For RemoteDataSetListRepository#makeErrorMessage - start
        Environment mockEnvironment = mock(Environment.class);
        when(mockEnvironment.getProperty("feign.atp.datasets.url")).thenReturn("http://test-host:8080");
        when(mockEnvironment.getProperty("feign.atp.datasets.route")).thenReturn("/api/test");

        // Set static field via Reflection
        Field envField = ApplicationConfig.class.getDeclaredField("env");
        envField.setAccessible(true);
        envField.set(null, mockEnvironment);
        // For RemoteDataSetListRepository#makeErrorMessage - end

        repository = new RemoteDataSetListRepository(folder);
    }

    /*
     * The most important test.
     * It tests processing of Visibility Area and DataSetList.
     */
    @Test
    @DisplayName("getByNatureId should find Visibility area and DSL by UUID")
    void testGetVisibilityAreaAndDataSetListByNatureId() {
        // Given
        UUID visibilityAreaId = UUID.randomUUID();
        UUID datasetListId = UUID.randomUUID();
        List<UUID> ourListOfDataSetLists = List.of(UUID.randomUUID(), datasetListId, UUID.randomUUID());
        String natureId = visibilityAreaId + "_" + datasetListId;
        BigInteger projectId = new BigInteger("123");

        // Mock HttpClientFactory and FeignClient
        try (MockedStatic<HttpClientFactory> httpClientFactoryMock = mockStatic(HttpClientFactory.class)) {
            when(HttpClientFactory.getDatasetsVisibilityAreaFeignClient()).thenReturn(visibilityAreaFeignClient);
            when(HttpClientFactory.getDatasetsDatasetListFeignClient()).thenReturn(datasetListFeignClient);

            // Create test response with a list of VisibilityAreas.
            VisibilityAreaFlatModelDto dto1 = fillVisibilityAreaDto(UUID.randomUUID(),
                    "Another Visibility Area #1",
                    List.of(UUID.randomUUID(), UUID.randomUUID()));

            VisibilityAreaFlatModelDto ourVisibilityAreaDto = fillVisibilityAreaDto(visibilityAreaId,
                    "Our Visibility Area",
                    ourListOfDataSetLists);

            VisibilityAreaFlatModelDto dto2 = fillVisibilityAreaDto(UUID.randomUUID(),
                    "Another Visibility Area #2",
                    List.of(UUID.randomUUID(), UUID.randomUUID()));

            ResponseEntity<List<VisibilityAreaFlatModelDto>> responseEntity =
                    new ResponseEntity<>(List.of(dto1, ourVisibilityAreaDto, dto2), HttpStatus.OK);
            when(visibilityAreaFeignClient.getVisibilityAreas()).thenReturn(responseEntity);

            // Create test response with a list of DataSetLists.
            List<DataSetListCreatedModifiedViewDto> dslDtoList = new ArrayList<>();
            for (UUID id : ourListOfDataSetLists) {
                dslDtoList.add(fillDataSetListCreatedModifiedViewDto(id, "DSL " + id));
            }
            ResponseEntity<List<DataSetListCreatedModifiedViewDto>> responseEntityDslDto =
                    new ResponseEntity<>(dslDtoList, HttpStatus.OK);
            when(datasetListFeignClient.getDataSetListsByVaId(any(UUID.class), any()))
                    .thenReturn(responseEntityDslDto);

            // When
            DataSetList result = repository.getByNatureId(natureId, projectId);

            // Then
            // Check if naturalId of the result equals expected.
            assertNotNull(result);
            assertEquals(natureId, result.getNaturalId());
        }
    }

    private VisibilityAreaFlatModelDto fillVisibilityAreaDto(UUID id,
                                                             String name,
                                                             List<UUID> dataSetLists) {
        VisibilityAreaFlatModelDto dto = new VisibilityAreaFlatModelDto();
        dto.setId(id);
        dto.setName(name);
        dto.setDataSetLists(dataSetLists);
        return dto;
    }

    private DataSetListCreatedModifiedViewDto fillDataSetListCreatedModifiedViewDto(UUID id, String name) {
        DataSetListCreatedModifiedViewDto dslDto = new DataSetListCreatedModifiedViewDto();
        dslDto.setId(id);
        dslDto.setName(name);
        dslDto.setCreatedBy(UUID.randomUUID());
        dslDto.setCreatedWhen(OffsetDateTime.now());
        dslDto.setModifiedBy(UUID.randomUUID());
        dslDto.setModifiedWhen(OffsetDateTime.now());
        dslDto.setLabels(new ArrayList<>());
        dslDto.setTestPlan(null);
        return dslDto;
    }

    @Test
    @DisplayName("getByNatureId should handle invalid natureId format")
    void testGetByNatureIdWithInvalidFormat() {
        // Given
        String invalidNatureId = "no_underscore";
        BigInteger projectId = new BigInteger("123");

        // When - Feign isn't invoked, because natureId format is invalid. And, there is no exception.
        DataSetList result = repository.getByNatureId(invalidNatureId, projectId);

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("getByNatureId should reject invalid UUID in dslId part")
    void testGetByNatureIdWithInvalidDslUuid() {
        // Given
        String invalidNatureId = UUID.randomUUID() + "_" + "not-a-uuid";
        BigInteger projectId = new BigInteger("123");

        // When
        DataSetList result = repository.getByNatureId(invalidNatureId, projectId);

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("getByNatureId should handle empty string")
    void testGetByNatureIdWithEmptyString() {
        // Given
        BigInteger projectId = new BigInteger("123");

        // When
        DataSetList result = repository.getByNatureId("", projectId);

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("getByNatureId should handle null response from DSS")
    void testGetByNatureIdWithNullResponse() {
        // Given
        String natureId = UUID.randomUUID() + "_" + UUID.randomUUID();
        BigInteger projectId = new BigInteger("123");

        try (MockedStatic<HttpClientFactory> httpClientFactoryMock = mockStatic(HttpClientFactory.class)) {
            when(HttpClientFactory.getDatasetsVisibilityAreaFeignClient()).thenReturn(visibilityAreaFeignClient);

            // DSS returns null
            when(visibilityAreaFeignClient.getVisibilityAreas()).thenReturn(null);

            // When
            DataSetList result = repository.getByNatureId(natureId, projectId);

            // Then
            assertNull(result);
        }
    }

    @Test
    @DisplayName("getByNatureId should handle empty response from DSS")
    void testGetByNatureIdWithEmptyResponse() {
        // Given
        String natureId = UUID.randomUUID() + "_" + UUID.randomUUID();
        BigInteger projectId = new BigInteger("123");

        try (MockedStatic<HttpClientFactory> httpClientFactoryMock = mockStatic(HttpClientFactory.class)) {
            when(HttpClientFactory.getDatasetsVisibilityAreaFeignClient()).thenReturn(visibilityAreaFeignClient);

            ResponseEntity<List<VisibilityAreaFlatModelDto>> responseEntity =
                    new ResponseEntity<>(List.of(), HttpStatus.OK);
            when(visibilityAreaFeignClient.getVisibilityAreas()).thenReturn(responseEntity);

            // When
            DataSetList result = repository.getByNatureId(natureId, projectId);

            // Then
            assertNull(result);
        }
    }

    @Test
    @DisplayName("getByNatureId should handle exception from DSS gracefully")
    void testGetByNatureIdWithException() {
        // Given
        String natureId = UUID.randomUUID() + "_" + UUID.randomUUID();
        BigInteger projectId = new BigInteger("123");

        try (MockedStatic<HttpClientFactory> httpClientFactoryMock = mockStatic(HttpClientFactory.class)) {
            when(HttpClientFactory.getDatasetsVisibilityAreaFeignClient()).thenReturn(visibilityAreaFeignClient);

            // DSS throws exception
            when(visibilityAreaFeignClient.getVisibilityAreas())
                    .thenThrow(new RuntimeException("DSS unavailable"));

            // When
            DataSetList result = repository.getByNatureId(natureId, projectId);

            // Then - method should process exception and return null
            assertNull(result);
        }
    }

    @Test
    @DisplayName("getAllSources should return all sources from DSS")
    void testGetAllSources() {
        // Given
        UUID vaId1 = UUID.randomUUID();
        UUID vaId2 = UUID.randomUUID();
        UUID vaId3 = UUID.randomUUID();

        String vaName1 = "Visibility Area 1";
        String vaName2 = "Visibility Area 2";
        String vaName3 = "Visibility Area 3";

        try (MockedStatic<HttpClientFactory> httpClientFactoryMock = mockStatic(HttpClientFactory.class)) {
            when(HttpClientFactory.getDatasetsVisibilityAreaFeignClient()).thenReturn(visibilityAreaFeignClient);

            // Create test response with multiple VisibilityAreas
            VisibilityAreaFlatModelDto dto1 = fillVisibilityAreaDto(vaId1, vaName1, List.of(UUID.randomUUID(), UUID.randomUUID()));
            VisibilityAreaFlatModelDto dto2 = fillVisibilityAreaDto(vaId2, vaName2, List.of(UUID.randomUUID()));
            VisibilityAreaFlatModelDto dto3 = fillVisibilityAreaDto(vaId3, vaName3, new ArrayList<>());

            ResponseEntity<List<VisibilityAreaFlatModelDto>> responseEntity =
                    new ResponseEntity<>(List.of(dto1, dto2, dto3), HttpStatus.OK);
            when(visibilityAreaFeignClient.getVisibilityAreas()).thenReturn(responseEntity);

            // When
            Collection<DataSetListsSource> result = repository.getAllSources();

            // Then
            assertNotNull(result);
            assertEquals(3, result.size());

            // Check that all sources have correct IDs and names
            List<UUID> expectedIds = List.of(vaId1, vaId2, vaId3);
            List<String> expectedNames = List.of(vaName1, vaName2, vaName3);

            for (DataSetListsSource source : result) {
                assertTrue(expectedIds.contains(UUID.fromString(source.getNaturalId())));
                assertTrue(expectedNames.contains(source.getName()));
            }

            // Verify that getVisibilityAreas was called exactly once
            verify(visibilityAreaFeignClient, times(1)).getVisibilityAreas();
        }
    }

    @Test
    @DisplayName("getAllSources with projectUuid should return filtered source")
    void testGetAllSourcesWithProjectUuid() {
        // Given
        UUID projectUuid = UUID.randomUUID();
        UUID vaId1 = UUID.randomUUID();
        UUID vaId2 = projectUuid; // This id should be in the result
        UUID vaId3 = UUID.randomUUID();

        String vaName1 = "Visibility Area 1";
        String vaName2 = "Visibility Area 2";
        String vaName3 = "Visibility Area 3";

        try (MockedStatic<HttpClientFactory> httpClientFactoryMock = mockStatic(HttpClientFactory.class)) {
            when(HttpClientFactory.getDatasetsVisibilityAreaFeignClient()).thenReturn(visibilityAreaFeignClient);

            VisibilityAreaFlatModelDto dto1 = fillVisibilityAreaDto(vaId1, vaName1, List.of(UUID.randomUUID()));
            VisibilityAreaFlatModelDto dto2 = fillVisibilityAreaDto(vaId2, vaName2, List.of(UUID.randomUUID(), UUID.randomUUID()));
            VisibilityAreaFlatModelDto dto3 = fillVisibilityAreaDto(vaId3, vaName3, List.of(UUID.randomUUID()));

            ResponseEntity<List<VisibilityAreaFlatModelDto>> responseEntity =
                    new ResponseEntity<>(List.of(dto1, dto2, dto3), HttpStatus.OK);
            when(visibilityAreaFeignClient.getVisibilityAreas()).thenReturn(responseEntity);

            // When
            Collection<DataSetListsSource> result = repository.getAllSources(projectUuid);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());

            DataSetListsSource source = result.iterator().next();
            assertEquals(projectUuid.toString(), source.getNaturalId());
            assertEquals(vaName2, source.getName());

            verify(visibilityAreaFeignClient, times(1)).getVisibilityAreas();
        }
    }

    @Test
    @DisplayName("getAllSources with projectUuid should return empty collection when no match")
    void testGetAllSourcesWithProjectUuidNoMatch() {
        // Given
        UUID projectUuid = UUID.randomUUID();
        UUID vaId1 = UUID.randomUUID();
        UUID vaId2 = UUID.randomUUID();
        UUID vaId3 = UUID.randomUUID();

        try (MockedStatic<HttpClientFactory> httpClientFactoryMock = mockStatic(HttpClientFactory.class)) {
            when(HttpClientFactory.getDatasetsVisibilityAreaFeignClient()).thenReturn(visibilityAreaFeignClient);

            VisibilityAreaFlatModelDto dto1 = fillVisibilityAreaDto(vaId1, "VA1", List.of(UUID.randomUUID()));
            VisibilityAreaFlatModelDto dto2 = fillVisibilityAreaDto(vaId2, "VA2", List.of(UUID.randomUUID()));
            VisibilityAreaFlatModelDto dto3 = fillVisibilityAreaDto(vaId3, "VA3", List.of(UUID.randomUUID()));

            ResponseEntity<List<VisibilityAreaFlatModelDto>> responseEntity =
                    new ResponseEntity<>(List.of(dto1, dto2, dto3), HttpStatus.OK);
            when(visibilityAreaFeignClient.getVisibilityAreas()).thenReturn(responseEntity);

            // When
            Collection<DataSetListsSource> result = repository.getAllSources(projectUuid);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(visibilityAreaFeignClient, times(1)).getVisibilityAreas();
        }
    }

    @Test
    @DisplayName("getSourceByNatureId should return source when UUID exists")
    void testGetSourceByNatureIdWhenUuidExists() {
        // Given
        UUID sourceId = UUID.randomUUID();
        String sourceIdStr = sourceId.toString();
        String sourceName = "Test Source";
        UUID vaId2 = UUID.randomUUID();

        try (MockedStatic<HttpClientFactory> httpClientFactoryMock = mockStatic(HttpClientFactory.class)) {
            when(HttpClientFactory.getDatasetsVisibilityAreaFeignClient()).thenReturn(visibilityAreaFeignClient);

            VisibilityAreaFlatModelDto dto1 = fillVisibilityAreaDto(UUID.randomUUID(), "VA1", List.of(UUID.randomUUID()));
            VisibilityAreaFlatModelDto dto2 = fillVisibilityAreaDto(vaId2, "VA2", List.of(UUID.randomUUID()));
            VisibilityAreaFlatModelDto dto3 = fillVisibilityAreaDto(sourceId, sourceName, List.of(UUID.randomUUID()));

            ResponseEntity<List<VisibilityAreaFlatModelDto>> responseEntity =
                    new ResponseEntity<>(List.of(dto1, dto2, dto3), HttpStatus.OK);
            when(visibilityAreaFeignClient.getVisibilityAreas()).thenReturn(responseEntity);

            BigInteger projectId = new BigInteger("123");

            // When
            DataSetListsSource result = repository.getSourceByNatureId(sourceIdStr, projectId);

            // Then
            assertNotNull(result);
            assertEquals(sourceIdStr, result.getNaturalId());
            assertEquals(sourceName, result.getName());

            verify(visibilityAreaFeignClient, times(1)).getVisibilityAreas();
        }
    }

    @Test
    @DisplayName("getSourceByNatureId should return null when UUID does not exist")
    void testGetSourceByNatureIdWhenUuidDoesNotExist() {
        // Given
        String nonExistentSourceId = UUID.randomUUID().toString();

        try (MockedStatic<HttpClientFactory> httpClientFactoryMock = mockStatic(HttpClientFactory.class)) {
            when(HttpClientFactory.getDatasetsVisibilityAreaFeignClient()).thenReturn(visibilityAreaFeignClient);

            VisibilityAreaFlatModelDto dto1 = fillVisibilityAreaDto(UUID.randomUUID(), "VA1", List.of(UUID.randomUUID()));
            VisibilityAreaFlatModelDto dto2 = fillVisibilityAreaDto(UUID.randomUUID(), "VA2", List.of(UUID.randomUUID()));

            ResponseEntity<List<VisibilityAreaFlatModelDto>> responseEntity =
                    new ResponseEntity<>(List.of(dto1, dto2), HttpStatus.OK);
            when(visibilityAreaFeignClient.getVisibilityAreas()).thenReturn(responseEntity);

            BigInteger projectId = new BigInteger("123");

            // When
            DataSetListsSource result = repository.getSourceByNatureId(nonExistentSourceId, projectId);

            // Then
            assertNull(result);

            verify(visibilityAreaFeignClient, times(1)).getVisibilityAreas();
        }
    }

    @Test
    @DisplayName("getSourceByNatureId should return null for invalid UUID format")
    void testGetSourceByNatureIdWithInvalidUuid() {
        // Given
        String invalidUuid = "not-a-uuid";
        BigInteger projectId = new BigInteger("123");

        // When
        DataSetListsSource result = repository.getSourceByNatureId(invalidUuid, projectId);

        // Then
        assertNull(result);

        // Feign isn't invoked, because invalid UUID
        verifyNoInteractions(visibilityAreaFeignClient);
    }

    @Test
    @DisplayName("getDataSetsWithLabel should return data sets from DSS")
    void testGetDataSetsWithLabel() {
        // Given
        UUID visibilityAreaId = UUID.randomUUID();
        UUID datasetListId = UUID.randomUUID();
        String natureId = visibilityAreaId + "_" + datasetListId;
        String label = null;
        BigInteger projectId = new BigInteger("123");

        // Create DataSetList with needed naturalId
        DataSetList mockDataSetList = mock(DataSetList.class);
        when(mockDataSetList.getNaturalId()).thenReturn(natureId);

        // Create test DataSetDtos
        UUID dataSetId1 = UUID.randomUUID();
        UUID dataSetId2 = UUID.randomUUID();

        DataSetDto dto1 = new DataSetDto();
        dto1.setId(dataSetId1);
        dto1.setName("Test DataSet 1");
        dto1.setLocked(false);

        DataSetDto dto2 = new DataSetDto();
        dto2.setId(dataSetId2);
        dto2.setName("Test DataSet 2");
        dto2.setLocked(true);

        try (MockedStatic<HttpClientFactory> httpClientFactoryMock = mockStatic(HttpClientFactory.class)) {
            when(HttpClientFactory.getDatasetsDatasetListFeignClient()).thenReturn(datasetListFeignClient);

            ResponseEntity<List<DataSetDto>> responseEntity =
                    new ResponseEntity<>(List.of(dto1, dto2), HttpStatus.OK);
            when(datasetListFeignClient.getDataSets(eq(datasetListId), isNull(), isNull()))
                    .thenReturn(responseEntity);

            // When
            Set<IDataSet> result = repository.getDataSetsWithLabel(mockDataSetList, label, projectId);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());

            // Check that Set of RemoteDataSets contains correct id and name
            List<String> expectedIds = List.of(dataSetId1.toString(), dataSetId2.toString());
            List<String> expectedNames = List.of("Test DataSet 1", "Test DataSet 2");

            for (IDataSet dataSet : result) {
                assertTrue(expectedIds.contains(dataSet.getIdDs()));
                assertTrue(expectedNames.contains(dataSet.getName()));
            }

            verify(datasetListFeignClient, times(1)).getDataSets(eq(datasetListId), isNull(), isNull());
        }
    }

    @Test
    @DisplayName("getDataSetsWithLabel should handle empty response from DSS")
    void testGetDataSetsWithLabelEmptyResponse() {
        // Given
        UUID visibilityAreaId = UUID.randomUUID();
        UUID datasetListId = UUID.randomUUID();
        String natureId = visibilityAreaId + "_" + datasetListId;
        BigInteger projectId = new BigInteger("123");

        DataSetList mockDataSetList = mock(DataSetList.class);
        when(mockDataSetList.getNaturalId()).thenReturn(natureId);

        try (MockedStatic<HttpClientFactory> httpClientFactoryMock = mockStatic(HttpClientFactory.class)) {
            when(HttpClientFactory.getDatasetsDatasetListFeignClient()).thenReturn(datasetListFeignClient);

            ResponseEntity<List<DataSetDto>> responseEntity =
                    new ResponseEntity<>(List.of(), HttpStatus.OK);
            when(datasetListFeignClient.getDataSets(any(UUID.class), isNull(), isNull()))
                    .thenReturn(responseEntity);

            // When
            Set<IDataSet> result = repository.getDataSetsWithLabel(mockDataSetList, null, projectId);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(datasetListFeignClient, times(1)).getDataSets(any(UUID.class), isNull(), isNull());
        }
    }

    @Test
    @DisplayName("getDataSetsWithLabel should handle null response body from DSS")
    void testGetDataSetsWithLabelNullResponse() {
        // Given
        UUID visibilityAreaId = UUID.randomUUID();
        UUID datasetListId = UUID.randomUUID();
        String natureId = visibilityAreaId + "_" + datasetListId;
        BigInteger projectId = new BigInteger("123");

        DataSetList mockDataSetList = mock(DataSetList.class);
        when(mockDataSetList.getNaturalId()).thenReturn(natureId);

        try (MockedStatic<HttpClientFactory> httpClientFactoryMock = mockStatic(HttpClientFactory.class)) {
            when(HttpClientFactory.getDatasetsDatasetListFeignClient()).thenReturn(datasetListFeignClient);

            ResponseEntity<List<DataSetDto>> responseEntity =
                    new ResponseEntity<>(null, HttpStatus.OK);
            when(datasetListFeignClient.getDataSets(any(UUID.class), isNull(), isNull()))
                    .thenReturn(responseEntity);

            // When - Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> repository.getDataSetsWithLabel(mockDataSetList, null, projectId));

            assertTrue(exception.getMessage().contains("Can not get list of Datasets"));

            verify(datasetListFeignClient, times(1))
                    .getDataSets(any(UUID.class), isNull(), isNull());
        }
    }

    @Test
    @DisplayName("getDataSetsWithLabel should handle exception from DSS")
    void testGetDataSetsWithLabelException() {
        // Given
        UUID visibilityAreaId = UUID.randomUUID();
        UUID datasetListId = UUID.randomUUID();
        String natureId = visibilityAreaId + "_" + datasetListId;
        BigInteger projectId = new BigInteger("123");

        DataSetList mockDataSetList = mock(DataSetList.class);
        when(mockDataSetList.getNaturalId()).thenReturn(natureId);

        try (MockedStatic<HttpClientFactory> httpClientFactoryMock = mockStatic(HttpClientFactory.class)) {
            when(HttpClientFactory.getDatasetsDatasetListFeignClient()).thenReturn(datasetListFeignClient);

            when(datasetListFeignClient.getDataSets(any(UUID.class), isNull(), isNull()))
                    .thenThrow(new IllegalArgumentException("DSS connection failed"));

            // When - Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> repository.getDataSetsWithLabel(mockDataSetList, null, projectId));

            assertTrue(exception.getMessage().contains("Can not get list of Datasets"));

            verify(datasetListFeignClient, times(1)).getDataSets(any(UUID.class), isNull(), isNull());
        }
    }

    @Test
    @DisplayName("getDataSetsWithLabel should handle invalid natureId format")
    void testGetDataSetsWithLabelInvalidNatureId() {
        // Given
        DataSetList mockDataSetList = mock(DataSetList.class);
        when(mockDataSetList.getNaturalId()).thenReturn("invalid_nature_id");
        BigInteger projectId = new BigInteger("123");

        // When - Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> repository.getDataSetsWithLabel(mockDataSetList, null, projectId));

        assertTrue(exception.getMessage().contains("Can not get list of Datasets"));
        // Feign isn't invoked
        verifyNoInteractions(datasetListFeignClient);
    }

    @Test
    @DisplayName("getVariables should return set of variable names from DSS")
    void testGetVariables() {
        // Given
        UUID visibilityAreaId = UUID.randomUUID();
        UUID datasetListId = UUID.randomUUID();
        String natureId = visibilityAreaId + "_" + datasetListId;

        DataSetList mockDataSetList = mock(DataSetList.class);
        when(mockDataSetList.getNaturalId()).thenReturn(natureId);

        // Create test list of variables in DSS response format
        List<String> expectedVariables = List.of("var1", "var2", "var3", "var4");

        try (MockedStatic<HttpClientFactory> httpClientFactoryMock = mockStatic(HttpClientFactory.class)) {
            DatasetsAttributeFeignClient attributeFeignClient = mock(DatasetsAttributeFeignClient.class);
            when(HttpClientFactory.getDatasetsAttributeFeignClient()).thenReturn(attributeFeignClient);

            ResponseEntity<Object> responseEntity =
                    new ResponseEntity<>(new ArrayList<>(expectedVariables), HttpStatus.OK);
            when(attributeFeignClient.getAttributesInItfFormat(eq(datasetListId)))
                    .thenReturn(responseEntity);

            // When
            Set<String> result = repository.getVariables(mockDataSetList);

            // Then
            assertNotNull(result);
            assertEquals(expectedVariables.size(), result.size());
            assertTrue(result.containsAll(expectedVariables));

            verify(attributeFeignClient, times(1)).getAttributesInItfFormat(eq(datasetListId));
        }
    }

    @Test
    @DisplayName("getVariables should handle empty list from DSS")
    void testGetVariablesEmptyList() {
        // Given
        UUID visibilityAreaId = UUID.randomUUID();
        UUID datasetListId = UUID.randomUUID();
        String natureId = visibilityAreaId + "_" + datasetListId;

        DataSetList mockDataSetList = mock(DataSetList.class);
        when(mockDataSetList.getNaturalId()).thenReturn(natureId);

        try (MockedStatic<HttpClientFactory> httpClientFactoryMock = mockStatic(HttpClientFactory.class)) {
            DatasetsAttributeFeignClient attributeFeignClient = mock(DatasetsAttributeFeignClient.class);
            when(HttpClientFactory.getDatasetsAttributeFeignClient()).thenReturn(attributeFeignClient);

            ResponseEntity<Object> responseEntity =
                    new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
            when(attributeFeignClient.getAttributesInItfFormat(eq(datasetListId)))
                    .thenReturn(responseEntity);

            // When
            Set<String> result = repository.getVariables(mockDataSetList);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(attributeFeignClient, times(1)).getAttributesInItfFormat(eq(datasetListId));
        }
    }

    @Test
    @DisplayName("getVariables should return empty set when response body is null")
    void testGetVariablesNullResponse() {
        // Given
        UUID visibilityAreaId = UUID.randomUUID();
        UUID datasetListId = UUID.randomUUID();
        String natureId = visibilityAreaId + "_" + datasetListId;

        DataSetList mockDataSetList = mock(DataSetList.class);
        when(mockDataSetList.getNaturalId()).thenReturn(natureId);

        try (MockedStatic<HttpClientFactory> httpClientFactoryMock = mockStatic(HttpClientFactory.class)) {
            DatasetsAttributeFeignClient attributeFeignClient = mock(DatasetsAttributeFeignClient.class);
            when(HttpClientFactory.getDatasetsAttributeFeignClient()).thenReturn(attributeFeignClient);

            ResponseEntity<Object> responseEntity =
                    new ResponseEntity<>(null, HttpStatus.OK);
            when(attributeFeignClient.getAttributesInItfFormat(eq(datasetListId)))
                    .thenReturn(responseEntity);

            // When
            Set<String> result = repository.getVariables(mockDataSetList);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(attributeFeignClient, times(1)).getAttributesInItfFormat(eq(datasetListId));
        }
    }

    @Test
    @DisplayName("getVariables should return empty set when exception occurs")
    void testGetVariablesException() {
        // Given
        UUID visibilityAreaId = UUID.randomUUID();
        UUID datasetListId = UUID.randomUUID();
        String natureId = visibilityAreaId + "_" + datasetListId;

        DataSetList mockDataSetList = mock(DataSetList.class);
        when(mockDataSetList.getNaturalId()).thenReturn(natureId);

        try (MockedStatic<HttpClientFactory> httpClientFactoryMock = mockStatic(HttpClientFactory.class)) {
            DatasetsAttributeFeignClient attributeFeignClient = mock(DatasetsAttributeFeignClient.class);
            when(HttpClientFactory.getDatasetsAttributeFeignClient()).thenReturn(attributeFeignClient);

            when(attributeFeignClient.getAttributesInItfFormat(eq(datasetListId)))
                    .thenThrow(new IllegalArgumentException("DSS connection failed"));

            // When
            Set<String> result = repository.getVariables(mockDataSetList);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(attributeFeignClient, times(1)).getAttributesInItfFormat(eq(datasetListId));
        }
    }

    @Test
    @DisplayName("getVariables should return empty set when response body is not a List")
    void testGetVariablesInvalidResponseType() {
        // Given
        UUID visibilityAreaId = UUID.randomUUID();
        UUID datasetListId = UUID.randomUUID();
        String natureId = visibilityAreaId + "_" + datasetListId;

        DataSetList mockDataSetList = mock(DataSetList.class);
        when(mockDataSetList.getNaturalId()).thenReturn(natureId);

        try (MockedStatic<HttpClientFactory> httpClientFactoryMock = mockStatic(HttpClientFactory.class)) {
            DatasetsAttributeFeignClient attributeFeignClient = mock(DatasetsAttributeFeignClient.class);
            when(HttpClientFactory.getDatasetsAttributeFeignClient()).thenReturn(attributeFeignClient);

            // Return Object, which is not List
            ResponseEntity<Object> responseEntity =
                    new ResponseEntity<>("Not a list", HttpStatus.OK);
            when(attributeFeignClient.getAttributesInItfFormat(eq(datasetListId)))
                    .thenReturn(responseEntity);

            // When - Then: expect ClassCastException
            assertThrows(ClassCastException.class,
                    () -> repository.getVariables(mockDataSetList));

            verify(attributeFeignClient, times(1)).getAttributesInItfFormat(eq(datasetListId));
        }
    }

    @Test
    @DisplayName("getVariables should handle invalid natureId format")
    void testGetVariablesInvalidNatureId() {
        // Given
        DataSetList mockDataSetList = mock(DataSetList.class);
        when(mockDataSetList.getNaturalId()).thenReturn("invalid_nature_id");

        // When
        Set<String> result = repository.getVariables(mockDataSetList);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // Feign isn't invoked
        verifyNoInteractions(datasetListFeignClient);
    }
}