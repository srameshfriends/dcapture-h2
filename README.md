# dcapture-io

Web Application Utility

Java -version 10

### Dependency

1. log4j-api, log4j-core - 2.11.0
2. commons-io - 2.6
3. pustike-inject - 1.4.2
4. javax.servlet-api - 4.0.1
5. jetty-server, jetty-servlet - 9.4.11.v20180605
6. javax.ws.rs-api - 2.1
7. javax.json-api - 1.1.2

### Configuration

- User settings should be at class path > settings.json
- Localization properties files details at > locale.json
- Localization files should be placed at class path > /locale/*.*

##### July-2018

IM01

- >Java 10 migration are updated.
- >Apache Log4j2 is used for logging
- >Git ignore file is added

IM02

- >Settings user id and name are added
- >Json response issue are fixed 
- >Localization : getMessage() method is returned as formatted value. 
- >File Monitor main method added

##### Sep-2018

IM03

- >Log4j2 is removed default java logging is used
- >Paging removed, FormModel is added

