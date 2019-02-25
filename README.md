# conmuncher
![](https://media.giphy.com/media/mh6H1spZTaQa4/giphy.gif)

## Requirements
- Java 1.8
- gradle 4.9+ 

## Build JAR
To run build the JAR file use:
```gradle jar```

## Run JAR
To run build the JAR file use:
```java -jar build/libs/conmuncher-1.0-SNAPSHOT.jar```

## Run compile and test
Because the project uses integration tests, it is possible that tests may fail due to local environment reasons. 
```gradle clean build```

## Stress Test 
Using local java client test/StressTest.java performance numbers:
```
APQLHTD66B3A9F:conmuncher ecimio$ java -jar build/libs/conmuncher-1.0-SNAPSHOT.jar
Received 0 unique numbers, 0 duplicates. Unique total: 0
Received 114551 unique numbers, 8 duplicates. Unique total: 114551
Received 132053 unique numbers, 23 duplicates. Unique total: 246604
Received 132255 unique numbers, 42 duplicates. Unique total: 378859
Received 132488 unique numbers, 64 duplicates. Unique total: 511347
Received 133104 unique numbers, 89 duplicates. Unique total: 644451
Received 133271 unique numbers, 83 duplicates. Unique total: 777722
Received 7581 unique numbers, 7 duplicates. Unique total: 785303
``` 
Total Received under 1 minute message displayed with 10secs interval is per requirement.