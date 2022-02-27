package alfonz19.orphanRemovalTest;

import alfonz19.orphanRemovalTest.jpa.ItemRepository;
import alfonz19.orphanRemovalTest.jpa.TopLevelEntityRepository;
import alfonz19.orphanRemovalTest.jpa.entities.ItemCode;
import alfonz19.orphanRemovalTest.jpa.entities.TopLevelEntity;
import lombok.AllArgsConstructor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
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
public class TestClass {

    private static final Logger log = LoggerFactory.getLogger(TestClass.class);

    private static final String TLE_ID = "ManualTest1";
    private static final String FIRST_ITEM_CODE = "code1";
    private static final String SECOND_ITEM_CODE = "code2";


    private final TopLevelEntityRepository topLevelEntityRepository;
    private final ItemRepository itemRepository;
    private final TransactionTemplate transactionTemplate;

    private final File persistFile = new File(new File(System.getProperty("java.io.tmpdir")), "saveTest_persist.txt");
    private final File mergeFile = new File(new File(System.getProperty("java.io.tmpdir")), "saveTest_merge.txt");
    //for quicker human-eyes comparison of hashcode value.
    private final Map<Integer, String> hashCodes = new HashMap<>();

    @EventListener(ApplicationReadyEvent.class)
    public void doTest() {
        testPersist();
        testMerge();
    }

    public void testPersist() {
        withBos(persistFile, e->doTest("PERSIST", topLevelEntityRepository::persist, s -> writeStringToFileRethrowingCheckedException(e, s)));
    }

    public void testMerge() {
        withBos(mergeFile, e->doTest("MERGE", topLevelEntityRepository::merge, s -> writeStringToFileRethrowingCheckedException(e, s)));
    }

    private void doTest(String actionName,
                        final Function<TopLevelEntity, TopLevelEntity> saveAction,
                        Consumer<String> logConsumer) {
        hashCodes.clear();
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                //-------------------------create root entity, with 1 association.
                action(actionName, saveAction, logConsumer);
            }
        });


        //cleanup.
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                cleanup(logConsumer);
            }
        });
    }

    private void action(String actionName,
                        Function<TopLevelEntity, TopLevelEntity> saveAction,
                        Consumer<String> logConsumer) {
        logSeparator("testing using "+actionName, '=', logConsumer);

        TopLevelEntity entity = new TopLevelEntity();
        entity.setTleCode(TLE_ID);

        ItemCode assoc = createAssociatedEntity(FIRST_ITEM_CODE, entity);

        entity.getItems().add(assoc);

        logSeparator("entities created", logConsumer);
        printTopLevelEntity("top-level entity before calling _save_ ", entity, logConsumer);
        List<ItemCode> assocBackup = new ArrayList<>(entity.getItems());
        printAssociationEntities("association entities before calling _save_: ", entity, logConsumer);
        logSeparator(String.format("call to %s", actionName), logConsumer);
        entity = saveAction.apply(entity);

        printTopLevelEntity("top-level entity after _save_: ", entity, logConsumer);
        printAssociationEntities("association entities after _save_: ", entity, logConsumer);
        printAssociationEntities("association entities from backup after _save_: ", assocBackup, logConsumer);

        logSeparator(String.format("Updating association entities after %s", actionName), logConsumer);

        entity.getItems().removeIf(e -> e.getPk().getItemCode().equals(FIRST_ITEM_CODE));

        ItemCode secondAssoc = createAssociatedEntity(SECOND_ITEM_CODE, entity);
        entity.getItems().add(secondAssoc);

        logSeparator("done, tx about to end.", '=', logConsumer);

        printTopLevelEntity("top level entity after update: ", entity, logConsumer);
        printAssociationEntities("association entities after update: ", entity, logConsumer);
        printAssociationEntities("association entities from backup after update: ", assocBackup, logConsumer);
        logSeparator("tx end", logConsumer);
    }

    private void logSeparator(String message, char ch, Consumer<String> logConsumer) {
        int size = 60;
        int dashSize = (size - message.length() - 2);
        char[] dashesArr = new char[dashSize/2];
        Arrays.fill(dashesArr, ch);
        String dashes = new String(dashesArr);

        log(String.format("%s %s %s%s", dashes, message, dashes, (dashSize % 2 == 0) ? "" : "-"), logConsumer);
    }

    private void logSeparator(String message, Consumer<String> logConsumer) {
        logSeparator(message, '-', logConsumer);
    }

    private void printTopLevelEntity(String message,
                                     TopLevelEntity entity,
                                     Consumer<String> logConsumer) {
        log(String.format(message + "(managed = %s, hash = %s): %s",
                topLevelEntityRepository.isManaged(entity),
                calculateIdentityHashCodeHumanRepresentation(entity),
                entity), logConsumer);
    }

    private String calculateIdentityHashCodeHumanRepresentation(Object entity) {
        int hash = System.identityHashCode(entity);
        return hashCodes.computeIfAbsent(hash, integer -> "<"+((char)('A' + hashCodes.size()))+">");
    }

    private void printAssociationEntities(String message,
                                          TopLevelEntity entity,
                                          Consumer<String> logConsumer) {
        List<ItemCode> itemEntities = entity.getItems();
        printAssociationEntities(message, itemEntities, logConsumer);
    }

    private void printAssociationEntities(String message,
                                          List<ItemCode> itemEntities,
                                          Consumer<String> logConsumer) {
        log(itemEntities.stream()
                .map(e->String.format("\t - (managed = %s, hash = %s): %s", itemRepository.isManaged(e), calculateIdentityHashCodeHumanRepresentation(e), e))
                .collect(Collectors.joining("\n", message+String.format("(whole collection hash %s)\n", calculateIdentityHashCodeHumanRepresentation(itemEntities)), "")),
                logConsumer);

    }

    private ItemCode createAssociatedEntity(String itemCode, TopLevelEntity entity) {
        ItemCode assoc = new ItemCode();
        assoc.setPk(new ItemCode.PK(itemCode, entity.getTleCode()));
        assoc.setTle(entity);
        return assoc;
    }

    private void cleanup(Consumer<String> logConsumer) {
        logSeparator("reading from db:", logConsumer);
        //noinspection OptionalGetWithoutIsPresent
        printTopLevelEntity("top-level entity after tx commit", topLevelEntityRepository.findById(TLE_ID).get(), logConsumer);

        logSeparator("clean up", logConsumer);
        topLevelEntityRepository.deleteById(TLE_ID);
        itemRepository.deleteAll();
        logSeparator("done", logConsumer);
    }

    private void log(String line, Consumer<String> logConsumer) {
        System.out.println(line);
        logConsumer.accept(line);
    }

    private void writeStringToFileRethrowingCheckedException(BufferedWriter e, String s) {
        try {
            e.write(s);
            e.newLine();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void withBos(File fileA, Consumer<BufferedWriter> bc) {
        try (BufferedWriter bos = new BufferedWriter((new FileWriter(fileA)))) {
            bc.accept(bos);
        } catch(Exception e) {
            log.error("error", e);
        }
    }
}
