### which branch to use:
In this branch (`justMergeFlow`) is variant which produce 'merge' flows to stdout. **If you don't want to compare persist/merge flows**, use `comparePersistToMergeBehavior` branch. 

### where is the crux of all this:

The main code is in method: `alfonz19.orphanRemovalTest.TestClass#action`, to invoke run Main class. 

There is lot of logging, but the performed action is:

1. create entity with naturalID and single associated entity
2. persist/merge
3. remove item from collection of associated entities
4. add item into collection of associated entities

### sql logging

please uncomment the line in `logback.xml` or use parameter `--logging.level.net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener=DEBUG`

### actual / expected behavior. 

result for merge flow: both association entities are persisted. 

Expected behavior: same as persist flow, just non-removed association entity is persisted.
