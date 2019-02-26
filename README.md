# conmuncher (connection muncher)
![](https://media.giphy.com/media/mh6H1spZTaQa4/giphy.gif)

## Design
- 3 thread pools have been provided to increase performance: 
    - Report execution scheduled pool.
    - 5 concurrent connections limit is enforced by connection thread pool size.
    - Repository thread pool improves performance due to the use of synchonized methods needed to atomically count records. 
- For performance reasons Monitor is not immutable in order to avoid creating tons of new instances.
- For organization purpose, 3 objects are provided:
    - Server singleton starts and ends socket's connections.
    - Repository object is responsible for saving records to file.
    - Monitor object is responsible for reporting accurate numbers every 10secs.

## Assumptions
- A connection can include more than one sequence of nine digits as long as newline sequences are present between codes.
- System processing is limited to 5 client connections, but it does not enforce unique client connections. 
- Monitor has synchronized methods to make sure counter operations are performed atomically, this has performance implications but ensures the correctness of reporting.
- Testing could be improved by using a mock library, but for simplicity reasons the applications was tested using a combination of integration, unit, and load tests without mocking.

## Requirements
- Java 1.8
- gradle 4.9+ 

## Build JAR
To build the JAR file use:

```gradle clean jar```

## Run JAR
To run the .jar file use:

```java -jar build/libs/conmuncher-1.0-SNAPSHOT.jar```

## Run compile and test
Because the project uses integration tests, it is possible that tests may fail due to local environment reasons. To run tests you can use:

```gradle clean build```

## Stress Test 
Using src/intTest/StressTest.java while running application at the same host displayed the following results:
```
APQLHTD66B3A9F:conmuncher ecimio$ java -jar build/libs/conmuncher-1.0-SNAPSHOT.jar
Received 0 unique numbers, 0 duplicates. Unique total: 0
Received 78372 unique numbers, 4 duplicates. Unique total: 78372
Received 180188 unique numbers, 25 duplicates. Unique total: 258560
Received 195069 unique numbers, 74 duplicates. Unique total: 453629
Received 194273 unique numbers, 100 duplicates. Unique total: 647902
Received 193795 unique numbers, 153 duplicates. Unique total: 841697
Received 184937 unique numbers, 191 duplicates. Unique total: 1026634
Received 182576 unique numbers, 214 duplicates. Unique total: 1209210
``` 
Totals received under 1 minute; message displayed with 10secs interval per requirement.
