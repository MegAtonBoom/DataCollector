package main;

import java.util.ArrayList;
import java.util.List;

public class TicketBug {
    private String id;

    private Release infectRelease;

    private Release fixedRelease;


    private Release ovRelease;


    private List<String> affectedFiles;

    public Release getOvRelease() {
        return ovRelease;
    }

    public void setOvRelease(Release ovRelease) {
        this.ovRelease = ovRelease;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    public List<String> getAffectedFiles() { return affectedFiles; }

    public void setAffectedFiles(List<String> affectedFiles) { this.affectedFiles = affectedFiles; }

    public void addAffectedFile(String affectedFile) { this.affectedFiles.add(affectedFile); }

    public Release getInfectRelease() {
        return infectRelease;
    }

    public void setInfectRelease(Release infectRelease) {
        this.infectRelease = infectRelease;
    }

    public Release getFixedRelease() {
        return fixedRelease;
    }

    public void setFixedRelease(Release fixedRelease) {
        this.fixedRelease = fixedRelease;
    }

    public TicketBug(String id, Release infectRelease, Release fixedRelease, Release ovRelease){
        this.id=id;
        this.infectRelease=infectRelease;
        this.fixedRelease=fixedRelease;
        this.affectedFiles=new ArrayList<>();
        this.ovRelease=ovRelease;
    }
}
