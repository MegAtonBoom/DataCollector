import org.eclipse.jgit.lib.PersonIdent;

import java.util.Date;
import java.util.List;

public class csvRow {

    private int version, LOC, touchedLOC, NRevisions, addedLOC, maxAddedLOC, churn, maxChurn, age, weighedAge, creationTime;
    private double AVGAddedLOC, AVGChurn;
    private List<PersonIdent> authors;
    private String filePath;
    private boolean buggy=false;

    public int getLOC() {
        return this.LOC;
    }

    public void setLOC(int LOC) {
        this.LOC = LOC;
    }

    public int getTouchedLOC() {
        return this.touchedLOC;
    }

    public void setTouchedLOC(int LOC) {
        this.touchedLOC = LOC;
    }

    public int getNRevisions() {
        return this.NRevisions;
    }

    public void setNRevisions(int nRev) {
        this.NRevisions = nRev;
    }

    public int getAddedLOC() {
        return this.addedLOC;
    }

    public void setAddedLOC(int LOC) {
        this.addedLOC = LOC;
    }

    public int getMaxAddedLOC() {
        return this.maxAddedLOC;
    }

    public void setMaxAddedLOC(int LOC) {
        this.maxAddedLOC = LOC;
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

    public double getAVGAddedLOC() {
        return this.AVGAddedLOC;
    }

    public void setAVGAddedLOC(double LOC) {
        this.AVGAddedLOC = LOC;
    }

    public double getAVGChurn() {
        return this.AVGChurn;
    }

    public void setAVGChurn(double churn) { this.AVGChurn = churn; }

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

    public csvRow(int version, String filePath){
        this.version=version;
        this.filePath=filePath;
    }

    public String[] toStringArray(){
        String buggy="No";
        if(this.buggy) buggy="Yes";

        String row[]=new String[]{""+this.version,
                this.filePath,
                ""+this.NRevisions,
                ""+this.LOC,
                ""+this.touchedLOC,
                ""+this.addedLOC,
                ""+this.maxAddedLOC,
                ""+this.AVGAddedLOC,
                ""+this.churn,
                ""+this.maxChurn,
                ""+this.AVGChurn,
                ""+this.age,
                ""+this.weighedAge,
                ""+this.authors.size(),
                buggy};
        return row;
    }

    public static String[] getHeadString(){
        String head[]=new String[]{"Version",
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
        return head;
    }

    public void ageSetter(int current){
        double time=(((((current-this.creationTime))/60)/60)/24)/7;
        this.age=(int)Math.ceil(time);
        this.weighedAge=this.age*this.touchedLOC;
    }

}
