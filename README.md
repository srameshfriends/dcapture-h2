# dcapture-servlet

Application Dispatcher Servlet

### Dependency

1. java-8
2. dcapture-api dependency
3. pustike-inject - 1.4.2
4. commons-email - 1.5
5. jetty-server, jetty-servlet - 9.4.11.v20180605 (Development Only)

### Configuration

- Settings is json format located at class path > config/settings.json
- Localization api provided.
- Localization loaded from webapp resource base > locale/*-en.properties, locale/*-ta.properties.
- Application admin console is responsible for loading and update localization.
- DispatcherListener, DispatcherServlet helps to run applications
- Session, Backup, Email and sms notification service api accessible from applications
- HttpService : method level annotation like POST, GET, DELETE url pattern map acceptable ( /* ), 
method(String parameter) received suffix of url. example [ http://api/upload/profile/* ] this is pattern map, 
The user requested url : [ http://api/upload/profile/kannamma ] String parameter received value : ( kannamma )       

## Changes and Commits

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

IM04

- >JsonRequest getSessionAttribute method created.

IM05

- >Apache commons file upload and csv library are included
- >Multi part servlet request support updated

##### Oct-2018

IM06

- >Commons-csv(1.6) and javax.ws.rs(2.1.1) library upgraded
- >DispatcherServlet request parameter HttpServletRequest and HttpServletResponse updated.

##### Nov-2018

IM07

- >Http method implementation completed
- >Application Setting improved 
- >Servlet Request updated FormModel features, FormModel removed
- >IOStream helps to avoid development issue

IM08

- >Text message response created
- >Dispatcher servlet more error message updated

##### Feb-2019
 
IM09
 
- >dcapture-maven-parent and dcapture-db projects dependency added
- >Jdk revert back to Java 8 due to tomcat 9 deployment

RID10 (Reference Id)

- >dcapture-io renamed to dcapture-servlet
- >dcapture-servlet handle all http servlet request and response, Implemented by HttpModule

##### Mar-2019

RID11

- >Improved error handler
- >dcapture-api changes are updated

RID12

- >dcapture-api changes updated

RID13

- >Dispatcher servlet and content reader fix 

##### Apr-2019

RID12

- >dcapture-api changes updated

##### Oct-2019

RID13

- >Authentication error message improved

RID14

- >HttpModule : getEntityList() method added to map with sql tables and entity classes

##### Nov-2019

RID15

- >DispatcherServlet : void result and null response error improved 

##### Dec-2019

RID16

- >Dispatcher initialize listener and authentication filter are added.
-  Token based authentication to be updated 