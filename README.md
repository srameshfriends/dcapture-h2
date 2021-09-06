# DCapture H2 Database start and stop service.

### Configuration

- port 8083 for h2 database server start and stop
- http://localhost:8083 start automatically the h2 database web and tcp server
- Manual start : http://localhost:8083/database/start
- Database service status: http://localhost:8083/database/status
- Manual stop : http://localhost:8083/database/stop
- Shutdown the jetty server as well as database services :  http://localhost:8084

### Dependency

1. java 11
2. H2 Database   
3. jetty-server, jetty-servlet (Development Only)
4. Log4j

### Build

Command prompt enter the following command and copy the dependency files into lib directory if your package.

```
 mvn dependency:copy-dependencies
```

Maven jar plugin configuration have to change mainClass ether EntryPoint or ExitPoint.

pom.xml (startup.jar)
```
 <manifest>
    <mainClass>dcapture.h2.embedded.EntryPoint</mainClass>
 </manifest>
```
pom.xml (shutdown.jar)
```
 <manifest>
    <mainClass>dcapture.h2.embedded.ExitPoint</mainClass>
 </manifest>
```