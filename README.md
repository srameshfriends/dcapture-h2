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