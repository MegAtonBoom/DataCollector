package main;

import org.eclipse.jgit.lib.PersonIdent;

import java.util.List;

public class CsvRow {

    private int version;
    private int loc;
    private int touchedLoc;
    private int nRevisions;
    private int addedLoc;
    private int maxAddedLoc;
    private int churn;
    private int maxChurn;
    private int age;
    private int weighedAge;
    private int creationTime;
    private double avgAddedLoc;
    private double avgChurn;
    private List<PersonIdent> authors;
    private String filePath;
    private boolean buggy=false;

    public int getLoc() {
        return this.loc;
    }

    public void setLoc(int loc) {
        this.loc = loc;
    }

    public int getTouchedLoc() {
        return this.touchedLoc;
    }

    public void setTouchedLoc(int loc) {
        this.touchedLoc = loc;
    }

    public int getnRevisions() {
        return this.nRevisions;
    }

    public void setnRevisions(int nRev) {
        this.nRevisions = nRev;
    }

    public int getAddedLoc() {
        return this.addedLoc;
    }

    public void setAddedLoc(int loc) {
        this.addedLoc = loc;
    }

    public int getMaxAddedLoc() {
        return this.maxAddedLoc;
    }

    public void setMaxAddedLoc(int loc) {
        this.maxAddedLoc = loc;
    }

    public int getChurn() {
        return this.churn;
    }

    public void setChurn(int churn) {
        this.churn = churn;
    }

    public int getMaxChurn() {
        return this.maxChurn;
    }

    public void setMaxChurn(int churn) {
        this.maxChurn = churn;
    }

    public double getAvgAddedLoc() {
        return this.avgAddedLoc;
    }

    public void setAvgAddedLoc(double loc) {
        this.avgAddedLoc = loc;
    }

    public double getAvgChurn() {
        return this.avgChurn;
    }

    public void setAvgChurn(double churn) { this.avgChurn = churn; }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getCreationTime() {
        return this.creationTime;
    }

    public void setCreationDate(int creationDate) {
        this.creationTime = creationDate;
    }

    public int getAge() {
        return this.age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getWeighedAge() {
        return this.weighedAge;
    }

    public void setWeighedAge(int weighedAge) {
        this.weighedAge = weighedAge;
    }

    public List<PersonIdent> getAuthors() {
        return this.authors;
    }

    public void setAuthors(List<PersonIdent> auth) {
        this.authors = auth;
    }

    public void addAuthor(PersonIdent auth) { this.authors.add(auth); }

    public boolean getBuggy() {
        return buggy;
    }

    public void setBuggy(boolean buggy) {
        this.buggy = buggy;
    }

    public CsvRow(int version, String filePath){
        this.version=version;
        this.filePath=filePath;
    }

    public String[] toStringArray(){
        String noBuggy="No";
        if(this.buggy) noBuggy="Yes";

        return new String[]{""+this.version,
                this.filePath,
                ""+this.nRevisions,
                ""+this.loc,
                ""+this.touchedLoc,
                ""+this.addedLoc,
                ""+this.maxAddedLoc,
                ""+this.avgAddedLoc,
                ""+this.churn,
                ""+this.maxChurn,
                ""+this.avgChurn,
                ""+this.age,
                ""+this.weighedAge,
                ""+this.authors.size(),
                noBuggy};
    }

    public static String[] getHeadString(){
        return new String[]{"Version",
                "File_Name",
                "Num_Revisions",
                "LOC",
                "LOC_touched",
                "LOC_added",
                "MAX_LOC_Added",
                "AVG_LOC_Added",
                "Churn",
                "MAX_Churn",
                "AVG_Churn",
                "Age_In_Weeks",
                "Weighed_Age",
                "N_Authors",
                "Buggy"};
    }

    public void ageSetter(int current){
        double time=((((current-this.creationTime)/60)/60)/(double)24)/7;
        this.age=(int)Math.ceil(time);
        this.weighedAge=this.age*this.touchedLoc;
    }

}
