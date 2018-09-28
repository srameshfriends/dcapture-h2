package dcapture.io;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MultiPartRequest extends HttpServletRequestWrapper {
    private final List<FileItem> fileItems;

    MultiPartRequest(DiskFileItemFactory factory, HttpServletRequest request)
            throws FileUploadException {
        super(request);
        ServletFileUpload upload = new ServletFileUpload(factory);
        fileItems = upload.parseRequest(request);
    }

    public List<CSVRecord> first() throws IOException {
        FileItem fileItem = null;
        if (0 < fileItems.size()) {
            fileItem = fileItems.get(0);
        }
        return fileItem != null ? getCsvRecord(fileItem) : new ArrayList<>();
    }

    public List<CSVRecord> getCsvRecord(FileItem item) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(item.getInputStream(), StandardCharsets.UTF_8));
        CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT);
        return parser.getRecords();
    }

    public List<FileItem> getFileItems() {
        return fileItems;
    }
}
