function RemoteToken() {
    RemoteToken.value = "";
}

RemoteToken.set = function (text) {
    if (localStorage && typeof text === "string") {
        localStorage.setItem("dcapture-remote-token", text);
    } else if (typeof text === "string") {
        RemoteToken.value = text;
    }
}
RemoteToken.get = function () {
    if (typeof RemoteToken.value === "string") {
        return RemoteToken.value;
    } else if (localStorage) {
        const text = localStorage.getItem("dcapture-remote-token");
        if (typeof text === "string") {
            RemoteToken.value = text;
            return text;
        }
    }
}

RemoteToken.clear = function () {
    delete RemoteToken.value;
    if (localStorage) {
        localStorage.removeItem("dcapture-remote-token");
    }
}

function createHttpRequest() {
    try {
        return new XMLHttpRequest();
    } catch (e) {
        try {
            return new ActiveXObject("Msxml2.XMLHTTP");
        } catch (e) {
            try {
                return new ActiveXObject("Microsoft.XMLHTTP");
            } catch (e) {
                alert("Browser not supported xml http request");
                return false;
            }
        }
    }
}

function remoteStream(args) {
    const xhr = createHttpRequest();
    if (!xhr) {
        alert("Http request not supported by the browser!");
        return;
    }
    if (typeof args.type !== "string") {
        args.type = "POST";
    }
    if (typeof args.contentType !== "string") {
        if (args.file instanceof File) {
            args.contentType = args.file.type || 'application/octet-stream';
            args.contentType = args.contentType + "; filename=" + encodeURIComponent(args.file.name);
        } else if (typeof args.data === "object") {
            args.contentType = "application/json";
        } else {
            args.contentType = "text/html";
        }
    }
    if (typeof args.data !== "object") {
        args.data = {};
    }
    if (typeof args.dataType !== "string") {
        args.dataType = "json";
    }
    xhr.open(args.type, args.url, true);
    xhr.onreadystatechange = function () {
        if (xhr.readyState === 4) {
            if (xhr.status === 200) {
                if (-1 < args.dataType.indexOf("json")) {
                    try {
                        if (typeof args.success === "function") {
                            args.success(JSON.parse(xhr.responseText));
                        }
                    } catch (e) {
                        console.log(e);
                        if (typeof args.error === "function") {
                            args.error({status: xhr.status, responseText: xhr.responseText});
                        }
                    }
                } else {
                    let respHeader = xhr.getResponseHeader("X-Request-ID");
                    try {
                        if (typeof respHeader === "string") {
                            respHeader = window.atob(respHeader);
                        }
                        if (typeof respHeader === "string") {
                            respHeader = JSON.parse(respHeader);
                        }
                    } catch (e) {
                        console.log(e);
                    }
                    if (typeof respHeader !== "object") {
                        respHeader = {};
                    }
                    respHeader.contentType = xhr.getResponseHeader("Content-Type");
                    args.success(xhr.responseText, respHeader);
                }
            } else if (xhr.status === 401 && typeof window.remoteCallUnauthorized === "function") {
                window.remoteCallUnauthorized({url: args.url, status: xhr.status, responseText: xhr.responseText});
            } else if (typeof args.error === "function") {
                args.error({status: xhr.status, responseText: xhr.responseText});
            }
            if (typeof args.always === "function") {
                args.always();
            }
        }
    };
    const token = RemoteToken.get();
    if (typeof token === "string") {
        xhr.setRequestHeader("X-Auth-Token", token);
    }
    if (args.file instanceof File) {
        let reader = new FileReader();
        reader.onloadend = function () {
            let reqId = window.btoa("{}");
            if (typeof args.data === "object") {
                reqId = window.btoa(JSON.stringify(args.data));
            } else if (typeof args.data === "string") {
                reqId = window.btoa(args.data);
            }
            xhr.setRequestHeader("Content-Type", args.contentType);
            xhr.setRequestHeader("X-Request-ID", reqId);
            xhr.send(reader.result);
        };
        reader.readAsArrayBuffer(args.file);
    } else if (typeof args.data === "object") {
        xhr.setRequestHeader("Content-Type", args.contentType);
        xhr.send(JSON.stringify(args.data));
    } else if (typeof args.data === "string") {
        xhr.setRequestHeader("Content-Type", args.contentType);
        xhr.send(args.data);
    } else {
        xhr.setRequestHeader("Content-Type", args.contentType);
        xhr.send(args.data);
    }
}

remoteStream.toBase64 = function (buffer) {
    let binary = '';
    let bytes = new Uint8Array(buffer);
    for (let idx = 0; idx < bytes.byteLength; idx++) {
        binary += String.fromCharCode(bytes[idx]);
    }
    return window.btoa(binary);
};

function remoteCall(args) {
    const xhr = createHttpRequest();
    if (!xhr) {
        if (typeof args.error === "function") {
            args.error({
                responseText: "Http remote request not supported by the browser.",
                status: 426,
                url: args.url
            });
        }
        return;
    }
    if (typeof args.type !== "string") {
        args.type = "POST";
    }
    let format = "json";
    if (typeof args.contentType !== "string") {
        args.contentType = "application/json;charset=UTF-8";
    } else if (-1 < args.contentType.indexOf("multipart")) {
        args.contentType = "multipart/form-data; charset=utf-8; boundary=gc0p4Jq0M2Yt08jU534c0p";
        format = "multipart";
    } else {
        if (-1 < args.contentType.indexOf("text") || -1 < args.contentType.indexOf("html")) {
            format = "html/text";
            args.dataType = "html/text";
        } else if (-1 < args.contentType.indexOf("image")) {
            format = "image";
        } else if (-1 < args.contentType.indexOf("audio")) {
            format = "audio";
        } else if (-1 < args.contentType.indexOf("video")) {
            format = "video";
        }
    }
    if (typeof args.dataType === "undefined") {
        args.dataType = "multipart" === format ? "json" : format;
    }
    xhr.open(args.type, args.url, true);
    if (typeof args.progress === "function") {
        xhr.upload.addEventListener("progress", args.progress, false);
    }
    xhr.onreadystatechange = function () {
        if (xhr.readyState === 4) {
            if (xhr.status === 200) {
                if (-1 < args.dataType.indexOf("json")) {
                    try {
                        const resObj = JSON.parse(xhr.responseText);
                        if (typeof args.success === "function") {
                            args.success(resObj, args.url, xhr.status);
                        }
                    } catch (jpe) {
                        if (typeof args.error === "function") {
                            args.error({responseText: jpe.message, status: xhr.status, url: args.url});
                        }
                    }
                } else {
                    if (typeof args.success === "function") {
                        args.success(xhr.responseText, args.url, xhr.status);
                    }
                }
            } else {
                if (typeof args.error === "function") {
                    args.error({responseText: xhr.responseText, status: xhr.status, url: args.url});
                }
            }
        }
    };
    const token = RemoteToken.get();
    if (typeof token === "string") {
        xhr.setRequestHeader("X-Auth-Token", token);
    }
    if ("json" === format) {
        xhr.setRequestHeader("Content-Type", args.contentType);
        if (typeof args.data === "undefined") {
            xhr.send("{}");
        } else if (typeof args.data !== "string") {
            xhr.send(JSON.stringify(args.data));
        } else {
            xhr.send(args.data);
        }
    } else if ("multipart" === format) {
        if (args.data instanceof MultipartForm) {
            xhr.setRequestHeader("Content-Type", args.contentType);
            args.data.send(xhr);
        } else if (args.data instanceof FormData) {
            xhr.setRequestHeader("Content-Type", args.contentType);
            xhr.send(args.data);
            args.data.send(xhr);
        } else {
            console.log("RemoteCall file upload accept only form MultipartForm");
            xhr.abort();
        }
        const boundary = "--gc0p4Jq0M2Yt08jU534c0p";
    } else {
        xhr.setRequestHeader("Content-Type", args.contentType);
        xhr.send();
    }
}

function MultiRemoteCall() {
    const self = this;
    self.remoteCaller = [];
    self.add = function (args) {
        if (Array.isArray(args)) {
            for (let jfx = 0; jfx < args.length; jfx++) {
                const reqObj = args[jfx];
                if (typeof reqObj === "object") {
                    self.remoteCaller.push(reqObj);
                }
            }
        } else if (typeof args === "object") {
            self.remoteCaller.push(args);
        } else {
            console.log('Multiple remote call argument should be array of object or single object');
        }
        return self;
    };
    self.call = function (notification) {
        self.dataArray = [];
        for (let idx = 0; idx < self.remoteCaller.length; idx++) {
            const args = self.remoteCaller[idx];
            args.success = function (res, url, sta) {
                self.dataArray.push({data: res, url: url, status: sta});
                if (self.dataArray.length === self.remoteCaller.length) {
                    notification(self.dataArray);
                }
            };
            args.error = function (err) {
                self.dataArray.push(err);
                if (self.dataArray.length === self.remoteCaller.length) {
                    notification(self.dataArray);
                }
            };
            remoteCall(args);
        }
    }
}