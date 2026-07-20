package org.qubership.automation.itf.ui.controls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.qubership.automation.itf.ui.util.UIHelper.getObjectList;
import static org.qubership.automation.itf.ui.util.UIHelper.getUIList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.qubership.automation.itf.configuration.dataset.impl.remote.RemoteDataSetList;
import org.qubership.automation.itf.configuration.dataset.impl.remote.RemoteDataSetListRepository;
import org.qubership.automation.itf.configuration.dataset.impl.remote.RemoteDataSetListsSource;
import org.qubership.automation.itf.core.model.common.Named;
import org.qubership.automation.itf.core.model.dataset.DataSetList;
import org.qubership.automation.itf.core.model.dataset.DataSetListsSource;
import org.qubership.automation.itf.core.model.jpa.folder.Folder;
import org.qubership.automation.itf.ui.messages.UIList;
import org.qubership.automation.itf.ui.messages.UIObjectList;
import org.qubership.automation.itf.ui.messages.objects.UIDataSetList;
import org.qubership.automation.itf.ui.messages.objects.UIObject;

class DatasetControllerTest {

    private static final Function<DataSetList, UIDataSetList> UI_DS_LIST_FUNC = UIDataSetList::new;

    @Mock
    private Folder<DataSetListsSource> folder;

    private RemoteDataSetListRepository repository;

    @BeforeEach
    void setUp() {
        repository = new RemoteDataSetListRepository(folder);
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void getLists() {
        UUID visibilityAreaId = UUID.randomUUID();
        DataSetListsSource va = new RemoteDataSetListsSource(repository, folder, visibilityAreaId, "Test VA");
        int total = 5;
        List<RemoteDataSetList> remoteDataSetLists = fillListOfDataSetLists(va, total);

        /*
            Below method throws NumberFormatException, after changing of ID type from Object to BigInteger:
            java.lang.NumberFormatException: Illegal embedded sign character
        	at java.base/java.math.BigInteger.<init>(BigInteger.java:499)
	        at java.base/java.math.BigInteger.<init>(BigInteger.java:679)
	        at org.qubership.automation.itf.configuration.dataset.impl.remote.RemoteDataSetList
	            .getID(RemoteDataSetList.java:183)
         */
        UIObjectList list = getObjectList(remoteDataSetLists.stream().sorted(Comparator.comparing(Named::getName))
                .collect(Collectors.toList()));
        assertNotNull(list.getObjects());
        assertEquals(remoteDataSetLists.size(), list.getObjects().size());

        List<UIObject> objects = list.getObjects().stream().toList();
        for (int k=0; k < total; k++) {
            assertEquals(remoteDataSetLists.get(k).returnDisplayId(), objects.get(k).getId());
            assertEquals(remoteDataSetLists.get(k).getName(), objects.get(k).getName());
        }
    }

    @Test
    void getList() {
        UUID visibilityAreaId = UUID.randomUUID();
        DataSetListsSource va = new RemoteDataSetListsSource(repository, folder, visibilityAreaId, "Test VA");
        int total = 5;
        List<RemoteDataSetList> remoteDataSetLists = fillListOfDataSetLists(va, total);

        UIList<UIDataSetList> uiList = getUIList(
                remoteDataSetLists.stream()
                        .sorted(Comparator.comparing(Named::getName))
                        .collect(Collectors.toList()),
                UI_DS_LIST_FUNC::apply);
        assertNotNull(uiList.getObjects());
        assertEquals(remoteDataSetLists.size(), uiList.getObjects().size());
        List<UIDataSetList> objects = uiList.getObjects().stream().toList();
        for (int k=0; k < total; k++) {
            assertEquals(remoteDataSetLists.get(k).returnDisplayId(), objects.get(k).getId());
        }
    }

    private List<RemoteDataSetList> fillListOfDataSetLists(DataSetListsSource va, int total) {
        List<RemoteDataSetList> remoteDataSetLists = new ArrayList<>();
        for (int k=0; k < total; k++) {
            remoteDataSetLists.add(new RemoteDataSetList(
                    repository, va, va.getNaturalId() + "_" + UUID.randomUUID(), "Test DSL#" + k));
        }
        return remoteDataSetLists;
    }
}