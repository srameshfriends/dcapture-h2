function DCaptureAppsDB() {

}

DCaptureAppsDB.showMessage = function (msg) {
    if(typeof msg === "object" && msg.responseText) {
        if(msg.responseText.includes('html')) {
            DCaptureAppsDB.messageBox.innerHTML = msg.responseText;
        } else {
            DCaptureAppsDB.messageBox.innerText = msg.responseText;
        }
    } else if(typeof msg === "string") {
        DCaptureAppsDB.messageBox.innerText = msg;
    } else {
        DCaptureAppsDB.messageBox.innerText = msg.toString();
    }
}

DCaptureAppsDB.databaseStatusEvent = function (evt) {
    evt.preventDefault();
    DCaptureAppsDB.showMessage('Loading Database Status ...');
    remoteCall({
        url: "database/status",
        type:'GET',
        contentType:'html/text',
        error: function (msg) {
            DCaptureAppsDB.showMessage(msg);
        },
        success: function (msg) {
            DCaptureAppsDB.showMessage(msg);
        }
    });
}

DCaptureAppsDB.databaseStartEvent = function (evt) {
    evt.preventDefault();
    DCaptureAppsDB.showMessage('Starting Database ...');
    remoteCall({
        url: "database/start",
        type:'GET',
        contentType:'html/text',
        error: function (msg) {
            DCaptureAppsDB.showMessage(msg);
        },
        success: function (msg) {
            DCaptureAppsDB.showMessage(msg);
        }
    });
}

DCaptureAppsDB.databaseStopEvent = function (evt) {
    evt.preventDefault();
    DCaptureAppsDB.showMessage('Shutdown Database ...');
    remoteCall({
        url: "database/stop",
        type:'GET',
        contentType:'html/text',
        error: function (msg) {
            DCaptureAppsDB.showMessage(msg);
        },
        success: function (msg) {
            DCaptureAppsDB.showMessage(msg);
        }
    });
}

DCaptureAppsDB.createDatabaseEvent = function (evt) {
    evt.preventDefault();
    const name = DCaptureAppsDB.databaseNameField.value.trim();
    if(0 === name.length) {
        DCaptureAppsDB.showMessage('Database name should not be empty.');
        return;
    }
    DCaptureAppsDB.showMessage('Create Database ...');
    remoteCall({
        url: "database/create?name=" + name,
        type:'GET',
        contentType:'html/text',
        error: function (msg) {
            DCaptureAppsDB.showMessage(msg);
        },
        success: function (msg) {
            DCaptureAppsDB.showMessage(msg);
        }
    });
}

DCaptureAppsDB.createDatabaseBackupEvent = function (evt) {
    evt.preventDefault();
    const name = DCaptureAppsDB.databaseNameField.value.trim();
    if(0 === name.length) {
        DCaptureAppsDB.showMessage('Database name should not be empty.');
        return;
    }
    remoteCall({
        url: "backup/create/" + name + "?type=offline",
        type:'GET',
        contentType:'text',
        error: function (msg) {
            DCaptureAppsDB.showMessage(msg);
        },
        success: function (msg) {
            DCaptureAppsDB.showMessage(msg);
        }
    });
};

DCaptureAppsDB.setBackupList = function (appsName, date, items) {
    if(0 === items.length) {
        DCaptureAppsDB.sharedDBLink.innerText = '';
        DCaptureAppsDB.cashbookDBLink.innerText = '';
        DCaptureAppsDB.materialsDBLink.innerText = '';
        DCaptureAppsDB.projectDBLink.innerText = '';
        DCaptureAppsDB.inventoryDBLink.innerText = '';
        DCaptureAppsDB.purchaseDBLink.innerText = '';
        DCaptureAppsDB.salesDBLink.innerText = '';
        DCaptureAppsDB.sharedDBLink.setAttribute('href', '#');
        DCaptureAppsDB.cashbookDBLink.setAttribute('href', '#');
        DCaptureAppsDB.materialsDBLink.setAttribute('href', '#');
        DCaptureAppsDB.projectDBLink.setAttribute('href', '#');
        DCaptureAppsDB.inventoryDBLink.setAttribute('href', '#');
        DCaptureAppsDB.purchaseDBLink.setAttribute('href', '#');
        DCaptureAppsDB.salesDBLink.setAttribute('href', '#');
    } else {
        DCaptureAppsDB.sharedDBLink.innerText = 'shared.zip';
        DCaptureAppsDB.cashbookDBLink.innerText = 'cashbook.zip';
        DCaptureAppsDB.materialsDBLink.innerText = 'materials.zip';
        DCaptureAppsDB.projectDBLink.innerText = 'project.zip';
        DCaptureAppsDB.inventoryDBLink.innerText = 'inventory.zip';
        DCaptureAppsDB.purchaseDBLink.innerText = 'purchase.zip';
        DCaptureAppsDB.salesDBLink.innerText = 'sales.zip';
        let prefix = '/backup/download/' + appsName + '?date=' + date + '&db=';
        let href = window.location.href;
        if(0 > href.indexOf('localhost')) {
            prefix = '/dcapture-h2/backup/download/' + appsName + '?date=' + date + '&db=';
        }
        DCaptureAppsDB.sharedDBLink.setAttribute('href', prefix + 'shared');
        DCaptureAppsDB.cashbookDBLink.setAttribute('href', prefix + 'cashbook');
        DCaptureAppsDB.materialsDBLink.setAttribute('href', prefix + 'materials');
        DCaptureAppsDB.projectDBLink.setAttribute('href', prefix + 'project');
        DCaptureAppsDB.inventoryDBLink.setAttribute('href', prefix + 'inventory');
        DCaptureAppsDB.purchaseDBLink.setAttribute('href', prefix + 'purchase');
        DCaptureAppsDB.salesDBLink.setAttribute('href', prefix + 'sales');
    }
};

DCaptureAppsDB.changeBackupDateEvent = function (evt) {
    evt.preventDefault();
    const name = DCaptureAppsDB.databaseNameField.value.trim();
    if(0 === name.length) {
        DCaptureAppsDB.showMessage('Database name should not be empty.');
        return;
    }
    const date = DCaptureAppsDB.backupDateFld.value;
    remoteCall({
        url: "backup/load-backup/" + name + "?date=" + date,
        type:'GET',
        contentType:'text',
        error: function (msg) {
            DCaptureAppsDB.showMessage(msg);
            DCaptureAppsDB.setBackupList(name, date, "");
        },
        success: function (body) {
            DCaptureAppsDB.setBackupList(name, date, body);
        }
    });
};

DCaptureAppsDB.init = function () {
    DCaptureAppsDB.messageBox = document.getElementById("message-box");
    DCaptureAppsDB.statusBtn = document.getElementById("database-status");
    DCaptureAppsDB.startBtn = document.getElementById("database-start");
    DCaptureAppsDB.stopBtn = document.getElementById("database-stop");
    DCaptureAppsDB.createBtn = document.getElementById("database-create");
    DCaptureAppsDB.databaseNameField = document.getElementById("database-name");
    DCaptureAppsDB.databaseBackupBtn = document.getElementById("database-backup");
    DCaptureAppsDB.backupDateFld = document.getElementById("backup-date");
    //
    DCaptureAppsDB.sharedDBLink = document.getElementById("shared-db");
    DCaptureAppsDB.cashbookDBLink = document.getElementById("cashbook-db");
    DCaptureAppsDB.materialsDBLink = document.getElementById("materials-db");
    DCaptureAppsDB.projectDBLink = document.getElementById("project-db");
    DCaptureAppsDB.inventoryDBLink = document.getElementById("inventory-db");
    DCaptureAppsDB.purchaseDBLink = document.getElementById("purchase-db");
    DCaptureAppsDB.salesDBLink = document.getElementById("sales-db");
    //
    DCaptureAppsDB.statusBtn.addEventListener('click', DCaptureAppsDB.databaseStatusEvent);
    DCaptureAppsDB.startBtn.addEventListener('click', DCaptureAppsDB.databaseStartEvent);
    DCaptureAppsDB.stopBtn.addEventListener('click', DCaptureAppsDB.databaseStopEvent);
    DCaptureAppsDB.createBtn.addEventListener('click', DCaptureAppsDB.createDatabaseEvent);
    DCaptureAppsDB.databaseBackupBtn.addEventListener('click', DCaptureAppsDB.createDatabaseBackupEvent);
    DCaptureAppsDB.backupDateFld.addEventListener('change', DCaptureAppsDB.changeBackupDateEvent);
    DCaptureAppsDB.databaseNameField.value = "";
}