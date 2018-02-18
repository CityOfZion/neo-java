# neo-java

to build and do code coverage run the following command:

```
mvn clean install site;
```

to build, do code coverage, and start the application, run the following command:

```
mvn clean install site; java -jar target/neo-java-0.0.1-SNAPSHOT-jar-with-dependencies.jar;
```

if the blockchain appears to be corrupt, run the following command:
```
java -jar target/neo-java-0.0.1-SNAPSHOT-jar-with-dependencies.jar /validate
```

todo:
1) check message checksum
2) use VM to check scripts
3) check signer hash
4) check that bulk insert stops on the first bad block in a list.

Image should be 100,000,000 NEO in 10,000 pixels. 10,000 NEO in a pixel.
