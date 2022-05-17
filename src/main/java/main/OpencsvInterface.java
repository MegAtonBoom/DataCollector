package main;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class OpencsvInterface {

    private String writePath;

    private List<String[]> rows;

    public List<String[]> getRows() {
        return rows;
    }

    public void setRows(List<String[]> rows) {
        this.rows = rows;
    }

    public String getWritePath() {
        return writePath;
    }

    public void setWritePath(String writePath) {
        this.writePath = writePath;
    }

    public OpencsvInterface(String writePath, List<String[]> rows){
        this.writePath=writePath;
        this.rows=rows;
    }


    public void writeFile() throws IOException {
        File file = new File(this.writePath);

        FileWriter outputfile = new FileWriter(file);

        CSVWriter writer = new CSVWriter(outputfile);

        for( String[] row: this.rows) {
            writer.writeNext(row);
        }

        writer.close();

    }
}
