# conmuncher (connection muncher)
![](https://media.giphy.com/media/mh6H1spZTaQa4/giphy.gif)

## Design
- 3 thread pools have been provided to increase performance: 
    - single threaded report execution pool .
    - 5 concurrent connections limit is enforce by connection thread pool size.
    - Repository thread pool to improve performance synchonized methods related to keep atomic counting of records. 
- for performance reasons, Monitor is not immutable to avoid creating new instances.
- for organization's purpose, 3 objects are provided:
    - Server starts and ends sockets connections.
    - Repository is responsible for saving records to file.
    - Monitor is responsible for repoting accurate numbers.

## Assumptions
- lines can include more than one sequence of nine digits as long as newline sequences are present between codes.
- system processing is limited to 5 client connections, but system is not enforcing unique client connections. 
- Monitor has synchronized methods to make sure counter operations are performed atomically, this has performance implications but ensures the correctness of reporting.
- testing could be improved by using a mock library, but for simplicity reasons the applications was tested using a combination of integration, unit, and load tests without mocking .


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
Using local java client test/StressTest.java performance numbers:
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
Total Received under 1 minute message displayed with 10secs interval is per requirement.