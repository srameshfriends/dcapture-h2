package dcapture.io;

import javax.json.JsonStructure;

public class JsonResult extends TextMessage {
    private JsonStructure structure;

    public JsonResult(int status, String message) {
        super(status, message);
    }

    public JsonResult(JsonStructure structure) {
        this.structure = structure;
    }

    public static JsonResult send(String message) {
        return new JsonResult(SC_OK, message);
    }

    public JsonStructure getStructure() {
        return structure;
    }
}