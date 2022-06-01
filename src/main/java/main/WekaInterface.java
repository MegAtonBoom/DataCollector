package main;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;
import weka.core.converters.ConverterUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WekaInterface {

    String folderPath;
    String allDataSourcePath=folderPath+"data.csv";
    String training="training";
    String testing="testing";
    String csv=".csv";
    String arff=".arff";
    JGitRetriever jgit;
    int lastRel=19;
    List<TicketBug> allTickets;
    List<TicketBug> currentTbs;
    List<Release> releases;

    public WekaInterface(JGitRetriever jgit, List<Release> releases, List<TicketBug> allTickets, String folderPath){
        this.jgit=jgit;
        this.releases=releases;
        this.allTickets=allTickets;
        this.folderPath=folderPath;
    }

    public void getAllFiles() throws Exception{

        ConverterUtils.DataSource source1;
        ConverterUtils.DataSource source2;
        String trainPath;
        String testPath;
        String trainArffPath;
        String testArffPath;

        for(int i=2  ; i<=this.lastRel; i++){
            trainPath=this.folderPath+this.training+this.releases.get(i-1).getVersion()+this.csv;
            testPath=this.folderPath+this.testing+this.releases.get(i-1).getVersion()+this.csv;

            File file = new File(trainPath);
            // if file already exists will do nothing
            if(file.createNewFile()){  //skip
            }


            file=new File(testPath);
            if(file.createNewFile()){  //skip
            }

            createFiles(this.releases.get(i-1));

            trainArffPath=this.folderPath+this.training+this.releases.get(i-1).getVersion()+this.arff;
            testArffPath=this.folderPath+this.testing+this.releases.get(i-1).getVersion()+this.arff;

            file=new File(trainArffPath);
            if(file.createNewFile()){  //skip
            }

            file=new File(testArffPath);
            if(file.createNewFile()){  //skip
            }

            csvToArff(trainPath, trainArffPath);
            csvToArff(testPath, testArffPath);

            source1 = new ConverterUtils.DataSource(trainArffPath);
            Instances istTraining = source1.getDataSet();
            source2 = new ConverterUtils.DataSource(testArffPath);
            Instances istTesting = source2.getDataSet();

            int numAttr = istTraining.numAttributes();
            istTraining.setClassIndex(numAttr - 1);
            istTesting.setClassIndex(numAttr - 1);
        }

    }



    public void createFiles(Release testInstance) throws IOException, CsvValidationException {


        String[] actualCpy;
        String[] head=CsvRow.getHeadString();
        String trainPath=this.folderPath+this.training+testInstance.getVersion()+this.csv;
        String testPath=this.folderPath+this.testing+testInstance.getVersion()+this.csv;
        File fileTrain = new File(trainPath);
        File fileTest = new File(testPath);

        // create FileWriter object with file as parameter
        FileWriter outputfileTrain = new FileWriter(fileTrain);
        FileWriter outputfileTest = new FileWriter(fileTest);

        // create CSVWriter object filewriter object as parameter
        CSVWriter writerTrain = new CSVWriter(outputfileTrain);
        CSVWriter writerTest = new CSVWriter(outputfileTest);

        setTickets(testInstance.getVersion());
        this.jgit.setTickets(this.currentTbs);
        this.jgit.checkBuggyness();

        writerTrain.writeNext(Arrays.copyOfRange(head, 2, head.length));
        writerTest.writeNext(Arrays.copyOfRange(head, 2, head.length));
        for( String[] row: jgit.getOutput()) {

            try {
                if (Integer.valueOf(row[0]) < testInstance.getVersion()) {
                    actualCpy = Arrays.copyOfRange(row, 2, row.length);
                    writerTrain.writeNext(actualCpy);
                }
            }
            catch(NumberFormatException e){
                //just skipping
            }

        }


        try (var fr = new FileReader(this.allDataSourcePath, StandardCharsets.UTF_8);
            var reader = new CSVReader(fr)) {
            String[] nextLine;
            nextLine= reader.readNext();
            actualCpy= Arrays.copyOfRange(nextLine,2,nextLine.length);


            while ((nextLine = reader.readNext()) != null && Integer.valueOf(nextLine[0])<=testInstance.getVersion()+1) {
                if (Integer.valueOf(nextLine[0]) == testInstance.getVersion() + 1) {

                    actualCpy = Arrays.copyOfRange(nextLine, 2, nextLine.length);

                    writerTest.writeNext(actualCpy);

                }
            }
            writerTest.close();
            writerTrain.close();
        } catch (IOException | CsvValidationException e) {
            //just skipping
        }


    }

    private void setTickets(int fv){
        List<TicketBug> currentTb = new ArrayList<>();
        for(TicketBug tb: this.allTickets){
            if(tb.getFixedRelease().getVersion()<=fv) currentTb.add(tb);
        }
        this.currentTbs=currentTb;
    }


    public static void csvToArff(String csvPath, String arffPath) throws IOException {

        // load CSV
        CSVLoader loader = new CSVLoader();
        loader.setSource(new File(csvPath));
        Instances data = loader.getDataSet();

        // save ARFF
        ArffSaver saver = new ArffSaver();
        saver.setInstances(data);
        saver.setFile(new File(arffPath));
        saver.writeBatch();
        // .arff file will be created in the output location
    }
}
