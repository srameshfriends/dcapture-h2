function DCaptureAppsManagement() {

}

DCaptureAppsManagement.showMessage = function (msg) {
    if(typeof msg === "object" && msg.responseText) {
        if(msg.responseText.includes('html')) {
            DCaptureAppsManagement.messageBox.innerHTML = msg.responseText;
        } else {
            DCaptureAppsManagement.messageBox.innerText = msg.responseText;
        }
    } else if(typeof msg === "string") {
        DCaptureAppsManagement.messageBox.innerText = msg;
    } else {
        DCaptureAppsManagement.messageBox.innerText = msg.toString();
    }
}

DCaptureAppsManagement.databaseStatusEvent = function (evt) {
    evt.preventDefault();
    DCaptureAppsManagement.showMessage('Loading Database Status ...');
    remoteCall({
        url: "database/status",
        type:'GET',
        contentType:'html/text',
        error: function (msg) {
            DCaptureAppsManagement.showMessage(msg);
        },
        success: function (msg) {
            DCaptureAppsManagement.showMessage(msg);
        }
    });
}

DCaptureAppsManagement.databaseStartEvent = function (evt) {
    evt.preventDefault();
    DCaptureAppsManagement.showMessage('Starting Database ...');
    remoteCall({
        url: "database/start",
        type:'GET',
        contentType:'html/text',
        error: function (msg) {
            DCaptureAppsManagement.showMessage(msg);
        },
        success: function (msg) {
            DCaptureAppsManagement.showMessage(msg);
        }
    });
}

DCaptureAppsManagement.databaseStopEvent = function (evt) {
    evt.preventDefault();
    DCaptureAppsManagement.showMessage('Shutdown Database ...');
    remoteCall({
        url: "database/stop",
        type:'GET',
        contentType:'html/text',
        error: function (msg) {
            DCaptureAppsManagement.showMessage(msg);
        },
        success: function (msg) {
            DCaptureAppsManagement.showMessage(msg);
        }
    });
}

DCaptureAppsManagement.createDatabaseEvent = function (evt) {
    evt.preventDefault();
    const name = DCaptureAppsManagement.databaseNameField.value.trim();
    if(0 === name.length) {
        DCaptureAppsManagement.showMessage('Database name should not be empty.');
        return;
    }
    DCaptureAppsManagement.showMessage('Create Database ...');
    remoteCall({
        url: "database/create?name=" + name,
        type:'GET',
        contentType:'html/text',
        error: function (msg) {
            DCaptureAppsManagement.showMessage(msg);
        },
        success: function (msg) {
            DCaptureAppsManagement.showMessage(msg);
        }
    });
}

DCaptureAppsManagement.init = function () {
    DCaptureAppsManagement.messageBox = document.getElementById("message-box");
    DCaptureAppsManagement.statusBtn = document.getElementById("database-status");
    DCaptureAppsManagement.startBtn = document.getElementById("database-start");
    DCaptureAppsManagement.stopBtn = document.getElementById("database-stop");
    DCaptureAppsManagement.createBtn = document.getElementById("database-create");
    DCaptureAppsManagement.databaseNameField = document.getElementById("database-name");
    DCaptureAppsManagement.statusBtn.addEventListener('click', DCaptureAppsManagement.databaseStatusEvent);
    DCaptureAppsManagement.startBtn.addEventListener('click', DCaptureAppsManagement.databaseStartEvent);
    DCaptureAppsManagement.stopBtn.addEventListener('click', DCaptureAppsManagement.databaseStopEvent);
    DCaptureAppsManagement.createBtn.addEventListener('click', DCaptureAppsManagement.createDatabaseEvent);
    DCaptureAppsManagement.databaseNameField.value = "";
}