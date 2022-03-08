package alfonz19.orphanRemovalTest;

import alfonz19.orphanRemovalTest.jpa.ItemRepository;
import alfonz19.orphanRemovalTest.jpa.TopLevelEntityRepository;
import alfonz19.orphanRemovalTest.jpa.entities.ItemCode;
import alfonz19.orphanRemovalTest.jpa.entities.TopLevelEntity;
import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@AllArgsConstructor
public class TestMergeAndUpdatingWhenManaged {

    private static final Logger log = LoggerFactory.getLogger(TestMergeAndUpdatingWhenManaged.class);

    private static final String TLE_ID = "ManualTest1";
    private static final String FIRST_ITEM_CODE = "code1";
    private static final String SECOND_ITEM_CODE = "code2";


    private final TopLevelEntityRepository topLevelEntityRepository;
    private final ItemRepository itemRepository;
    private final TransactionTemplate transactionTemplate;

    //for quicker human-eyes comparison of hashcode value.
    private final Map<Integer, String> hashCodes = new HashMap<>();

//    @EventListener(ApplicationReadyEvent.class)
    public void doTest() {
        hashCodes.clear();
        TopLevelEntity detachedTopLevelEntity = transactionTemplate.execute(status -> persistRecord());

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                updateExisting(detachedTopLevelEntity);
            }
        });


        //cleanup.
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                cleanup();
            }
        });
    }

    private TopLevelEntity persistRecord() {
        String actionName = "PERSIST";
        logSeparator("testing using "+actionName, '=');

        TopLevelEntity entity = new TopLevelEntity();
        entity.setTleCode(TLE_ID);

        ItemCode assoc = createAssociatedEntity(FIRST_ITEM_CODE, entity);

        entity.getItems().add(assoc);

        logSeparator("entities created");
        printTopLevelEntity("top-level entity before calling _save_ ", entity);
        printAssociationEntities("association entities before calling _save_: ", entity);
        logSeparator(String.format("call to %s", actionName));
        entity = topLevelEntityRepository.persist(entity);
        logSeparator("tx end");
        return entity;
    }

    private void updateExisting(TopLevelEntity detachedTopLevelEntity) {
        logSeparator(String.format("merging and updating entity"));

        printTopLevelEntity("top-level entity before merge: ", detachedTopLevelEntity);
        printAssociationEntities("association entities before merge: ", detachedTopLevelEntity);

        logSeparator(String.format("merging"));
        TopLevelEntity entity = topLevelEntityRepository.merge(detachedTopLevelEntity);

        printTopLevelEntity("top-level entity after merge: ", detachedTopLevelEntity);
        printAssociationEntities("association entities after merge: ", detachedTopLevelEntity);

        logSeparator(String.format("Updating association entities"));


        entity.getItems().removeIf(e -> e.getPk().getItemCode().equals(FIRST_ITEM_CODE));

        ItemCode secondAssoc = createAssociatedEntity(SECOND_ITEM_CODE, entity);
        entity.getItems().add(secondAssoc);

        logSeparator("done, tx about to end.", '=');

        printTopLevelEntity("top level entity after update: ", entity);
        printAssociationEntities("association entities after update: ", entity);
        logSeparator("tx end");
    }

    private void logSeparator(String message, char ch) {
        int size = 60;
        int dashSize = (size - message.length() - 2);
        char[] dashesArr = new char[dashSize/2];
        Arrays.fill(dashesArr, ch);
        String dashes = new String(dashesArr);

        log(String.format("%s %s %s%s", dashes, message, dashes, (dashSize % 2 == 0) ? "" : "-"));
    }

    private void logSeparator(String message) {
        logSeparator(message, '-');
    }

    private void printTopLevelEntity(String message,
                                     TopLevelEntity entity) {
        log(String.format(message + "(managed = %s, hash = %s): %s",
                topLevelEntityRepository.isManaged(entity),
                calculateIdentityHashCodeHumanRepresentation(entity),
                entity));
    }

    private String calculateIdentityHashCodeHumanRepresentation(Object entity) {
        int hash = System.identityHashCode(entity);
        return hashCodes.computeIfAbsent(hash, integer -> "<"+((char)('A' + hashCodes.size()))+">");
    }

    private void printAssociationEntities(String message,
                                          TopLevelEntity entity) {
        List<ItemCode> itemEntities = entity.getItems();
        printAssociationEntities(message, itemEntities);
    }

    private void printAssociationEntities(String message,
                                          List<ItemCode> itemEntities) {
        log(itemEntities.stream()
                .map(e->String.format("\t - (managed = %s, hash = %s): %s", itemRepository.isManaged(e), calculateIdentityHashCodeHumanRepresentation(e), e))
                .collect(Collectors.joining("\n", message+String.format("(whole collection hash %s)\n", calculateIdentityHashCodeHumanRepresentation(itemEntities)), ""))
        );

    }

    private ItemCode createAssociatedEntity(String itemCode, TopLevelEntity entity) {
        ItemCode assoc = new ItemCode();
        assoc.setPk(new ItemCode.PK(itemCode, entity.getTleCode()));
        assoc.setTle(entity);
        return assoc;
    }

    private void cleanup() {
        logSeparator("reading from db:");
        //noinspection OptionalGetWithoutIsPresent
        printTopLevelEntity("top-level entity after tx commit", topLevelEntityRepository.findById(TLE_ID).get());

        logSeparator("clean up");
        topLevelEntityRepository.deleteById(TLE_ID);
        itemRepository.deleteAll();
        logSeparator("done");
    }

    private void log(String line) {
        System.out.println(line);
    }
}
