package main;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.RandomForest;
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

public class wekaInterface {

    String folderPath="C:\\Users\\39320\\Desktop\\Corsi\\isw2\\data\\progetto\\";
    String allDataSourcePath=folderPath+"data.csv";
    JGitRetriever jgit;
    int lastRel=19;
    List<TicketBug> allTickets;
    List<TicketBug> currentTbs;
    List<Release> releases;

    public wekaInterface(JGitRetriever jgit, List<Release> releases, List<TicketBug> allTickets){
        this.jgit=jgit;
        this.releases=releases;
        this.allTickets=allTickets;
    }

    public void getAllFiles() throws Exception{

        ConverterUtils.DataSource source1, source2;
        String trainPath;
        String testPath;
        String trainArffPath;
        String testArffPath;

        for(int i=2  ; i<=this.lastRel; i++){
            trainPath=this.folderPath+"training"+this.releases.get(i-1).getVersion()+".csv";
            testPath=this.folderPath+"testing"+this.releases.get(i-1).getVersion()+".csv";

            File file = new File(trainPath);
            file.createNewFile(); // if file already exists will do nothing

            file=new File(testPath);
            file.createNewFile();

            createFiles(this.releases.get(i-1));

            trainArffPath=this.folderPath+"training"+this.releases.get(i-1).getVersion()+".arff";
            testArffPath=this.folderPath+"testing"+this.releases.get(i-1).getVersion()+".arff";

            file=new File(trainArffPath);
            file.createNewFile();

            file=new File(testArffPath);
            file.createNewFile();

            csvToArff(trainPath, trainArffPath);
            csvToArff(testPath, testArffPath);

            source1 = new ConverterUtils.DataSource(trainArffPath);
            Instances training = source1.getDataSet();
            source2 = new ConverterUtils.DataSource(testArffPath);
            Instances testing = source2.getDataSet();

            int numAttr = training.numAttributes();
            training.setClassIndex(numAttr - 1);
            testing.setClassIndex(numAttr - 1);
        }

    }



    public void createFiles(Release testInstance) throws IOException, CsvValidationException {


        String[] actualCpy;
        String[] head=CsvRow.getHeadString();
        String trainPath=this.folderPath+"training"+testInstance.getVersion()+".csv";
        String testPath=this.folderPath+"testing"+testInstance.getVersion()+".csv";
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
            e.printStackTrace();
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
