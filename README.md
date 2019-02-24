# conmuncher
![](https://media.giphy.com/media/mh6H1spZTaQa4/giphy.gif)

## Requirements
- Java 1.8
- gradle 4.9+ 

## Build JAR
To run build the JAT file use:
``gradle jar``

## Run compile and test
Because the project uses integration tests, it is possible that tests may fail due to local environment reasons. To skip test please us:
``gradle clean build``

## Assumptions
- The application has been design for simplicity: no additional objects have been created although I considered having a more Object Oriented objects like Report and Code.

## Stress Test 
Using local java client test/java.StressTest performance numbers:
``APQLHTD66B3A9F:conmuncher ecimio$ java -jar build/libs/conmuncher-1.0-SNAPSHOT.jar
  Received 0 unique numbers, 0 duplicates. Unique total: 0
  Received 53350 unique numbers, 1 duplicates. Unique total: 53350
  Received 91093 unique numbers, 3 duplicates. Unique total: 144447
  Received 94288 unique numbers, 19 duplicates. Unique total: 238739
  Received 93555 unique numbers, 23 duplicates. Unique total: 332298
  Received 94386 unique numbers, 41 duplicates. Unique total: 426686
  Received 94687 unique numbers, 50 duplicates. Unique total: 521375
  Received 87099 unique numbers, 46 duplicates. Unique total: 608477
  Received 80240 unique numbers, 51 duplicates. Unique total: 688719
  Received 78723 unique numbers, 48 duplicates. Unique total: 767444
  Received 84969 unique numbers, 64 duplicates. Unique total: 852414
  Received 88532 unique numbers, 68 duplicates. Unique total: 940948
  Received 93428 unique numbers, 101 duplicates. Unique total: 1034377
  Received 93088 unique numbers, 117 duplicates. Unique total: 1127467
  Received 86961 unique numbers, 114 duplicates. Unique total: 1214431
  Received 83471 unique numbers, 123 duplicates. Unique total: 1297903
  Received 82612 unique numbers, 99 duplicates. Unique total: 1380515
  Received 82196 unique numbers, 114 duplicates. Unique total: 1462713
  Received 85184 unique numbers, 119 duplicates. Unique total: 1547898
  Received 92821 unique numbers, 138 duplicates. Unique total: 1640721
  Received 57854 unique numbers, 85 duplicates. Unique total: 1698576``
  
  Received message interval display is 10secs per requirement.