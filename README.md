# Low Availability Bike Station Service

Service to calculate in real time the availability of the bike stations and produce a message in case the ratio is under a certain threshold.

Kafka topics:
- station_information: provide information about the bike station (capacity, coordenates, etc)
- station_status: provide information about the status of the bike station such as the number of bikes available
- low_availability: receive a message in case the bike station availability ratio is under a certaion ratio

### Building
```
mvn clean install
```

### Running

Low Availability Bike Station Service needs the following containers to be running 
- kafka
- zookeeper

So, before continue running the subscription internal microservice, it is needed to execute this command in order to start them:
```
docker-compose up -d 
```

#### Intellij idea 
Choose run on Application.class

#### Maven
```
mvn spring-boot:run
```

#### Java
```
java -jar target/low-availability-service.jar
```
