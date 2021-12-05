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

Copy files to ssh
Copy single file from local to remote.

 ```
 scp sample.zip remoteuser@remoteserver:/remote/folder/
 mv source/folder target/folder   
 ```


Ubuntu Change permission the directory

```
cd /opt/h2
sudo chown -R tomcat bin/ logs/ temp/ lib/ data/
sudo chgrp -R tomcat /opt/h2
```

Change to read permission only

```
  sudo chmod -R g-w /opt/h2/bin
  sudo chmod -R g-w /opt/h2/lib
```

Linux Service

```
[Unit]
Description=DCapture H2 Console
After=network.target

[Service]
Type=forking

User=tomcat
Group=tomcat

Environment="JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64"
Environment="JAVA_OPTS=-Djava.security.egd=file:///dev/urandom -Djava.awt.headless=true"

ExecStart=/opt/h2/bin/startup.sh
ExecStop=/opt/h2/bin/shutdown.sh

[Install]
WantedBy=multi-user.target
```

Add to ubuntu service

```
 sudo nano /etc/systemd/system/h2.service
```

Save and close the file. Make systemd aware of the new script with the command:

```
 sudo systemctl daemon-reload
 sudo systemctl enable h2.service
 sudo systemctl start h2.service
```
21.12.0

- H2 database library upgraded.
- version update to 1.2

21.9.2

- Create system database feature added.
- Servlet response content length issue fixed.

21.9.1

- boostrap client side library added.
- status, start and stop tested.
- creating new database with given name feature id added. 
