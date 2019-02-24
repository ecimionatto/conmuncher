# conmuncher
![](https://media.giphy.com/media/mh6H1spZTaQa4/giphy.gif)

## Requirements
- Java 1.8
- gradle 4.9+ 

## Build JAR
To run build the JAR file use:
``gradle jar``

## Run JAR
To run build the JAR file use:
``java -jar build/libs/conmuncher-1.0-SNAPSHOT.jar``

## Run compile and test
Because the project uses integration tests, it is possible that tests may fail due to local environment reasons. 
``gradle clean build``

## Assumptions
- The application has been design for simplicity: no additional objects have been created although I considered having a more Object Oriented objects like Report and Code.

## Stress Test 
Using local java client test/java.StressTest performance numbers:
```
APQLHTD66B3A9F:conmuncher ecimio$ java -jar build/libs/conmuncher-1.0-SNAPSHOT.jar
Received 0 unique numbers, 0 duplicates. Unique total: 0
Received 53350 unique numbers, 1 duplicates. Unique total: 53350
Received 91093 unique numbers, 3 duplicates. Unique total: 144447
Received 94288 unique numbers, 19 duplicates. Unique total: 238739
Received 93555 unique numbers, 23 duplicates. Unique total: 332298
Received 94386 unique numbers, 41 duplicates. Unique total: 426686
 ``` 
  Received message interval display is 10secs per requirement.