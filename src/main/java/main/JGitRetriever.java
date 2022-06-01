package main;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.*;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;

import java.io.*;
import java.util.*;

public class JGitRetriever {


    private String path;
    private String outputCsv="C:\\Users\\39320\\Desktop\\Corsi\\isw2\\data\\progetto\\data.csv";
    private List<RevCommit> commits;



    private List<TicketBug> tickets;

    private Git git;
    private Repository repository;
    private List<Release> releases;

    public List<String[]> getOutput() {
        return output;
    }

    public void setOutput(List<String[]> output) {
        this.output = output;
    }

    private List<String[]> output;

    public JGitRetriever(String path, List<Release> releases, List<TicketBug> tickets) throws GitAPIException, IOException {
        this.path=path;
        this.tickets=tickets;
        this.commits=new ArrayList<>();
        this.releases=releases;
        this.output=new ArrayList<>();
        getRepoAndCommits();
    }

    //creates the repository and gets each of the project commit
    private void getRepoAndCommits() throws IOException, GitAPIException {
        this.repository = new FileRepository(this.path);
        String treeName = "refs/heads/master"; // tag or branch
        this.git= new Git(repository);
        getCommits(this.releases.get(this.releases.size()-1), treeName);
    }

    public void getCommits(Release last, String treeName) throws IOException, GitAPIException {

        this.commits=new ArrayList<>();
        for (RevCommit commit : this.git.log().add(this.repository.resolve(treeName)).call()) {

            this.commits.add(commit);
        }
    }




    public void getAnyFileAndData(Release last, String address ) throws IOException {
        int i = this.commits.size();
        int j = 0;
        int version;
        if(last.getVersion()<29){
            version=last.getVersion();
        }
        else{
            version=this.releases.size();
        }
        RevCommit oldCommit = null;
        RevCommit newCommit;
        List<DiffEntry> diffEntries;
        TicketBug currentTb = null;
        Date commitDate;
        HashMap<String, CsvRow> rows = new HashMap<>();


        output.add(CsvRow.getHeadString());
        OpencsvInterface ocsvi;

        Set<Map.Entry<String, CsvRow>> entrySet;
        Iterator<Map.Entry<String, CsvRow>> it;
        OutputStream outputStream = new ByteArrayOutputStream();
        System.out.println(last.getVersion());

        while (i > 0 && j <= version) {
            if (i != this.commits.size()) oldCommit = this.commits.get(i);
            newCommit = this.commits.get(i - 1);
            PersonIdent auth=newCommit.getCommitterIdent();
            commitDate=new Date(newCommit.getCommitTime() * 1000L);
            currentTb=getRelatedTicket(newCommit);
            if(commitDate.after(last.getDate())) {
                i--;
                continue;
            }

            if (commitDate.after(this.releases.get(j).getDate())) {
                j++;
                entrySet = rows.entrySet();
                it = entrySet.iterator();
                writeOutput(it, j, oldCommit);
            }

            try (ObjectReader reader = git.getRepository().newObjectReader()) {
                AbstractTreeIterator oldTreeIterator=getOld(reader, oldCommit);
                AbstractTreeIterator newTreeIterator = new CanonicalTreeParser(null, reader, newCommit.getTree().getId());

                try (DiffFormatter diffFormatter = new DiffFormatter(outputStream)) {
                    diffFormatter.setRepository(git.getRepository());
                    diffEntries = diffFormatter.scan(oldTreeIterator, newTreeIterator);
                    writeRows(diffEntries, rows, auth, j+1, newCommit, currentTb);

                }
            }
            i--;
        }
        entrySet = rows.entrySet();

        it = entrySet.iterator();
        writeOutput(it, j, oldCommit);
        checkBuggyness();

        ocsvi = new OpencsvInterface(address, output);
        ocsvi.writeFile();

    }

    private void writeRows (List<DiffEntry> diffEntries, HashMap<String, CsvRow> rows, PersonIdent auth, int v, RevCommit commit, TicketBug tb ) throws IOException {

        OutputStream outputStream = new ByteArrayOutputStream();
        DiffEntry entry;
        String targetExt=".java";
        CsvRow row;

        try (DiffFormatter diffFormatter = new DiffFormatter(outputStream)) {
            diffFormatter.setRepository(git.getRepository());
            for (int k = 0; k < diffEntries.size(); k++) {

                entry = diffEntries.get(k);
                EditList el = diffFormatter.toFileHeader(entry).toEditList();
                String enp = entry.getNewPath();
                String eop = entry.getOldPath();
                DiffEntry.ChangeType ct = entry.getChangeType();

                if (ct == DiffEntry.ChangeType.ADD && enp.endsWith(targetExt) && !rows.containsKey(enp)) {
                        //new file
                        row = getFileData(enp, v, el, rows, ct, auth);
                        row.setCreationDate(commit.getCommitTime());
                        rows.put(enp, row);

                }
                else if (ct == DiffEntry.ChangeType.MODIFY && enp.endsWith(targetExt) && rows.containsKey(enp)){

                        addBuggedFile(tb, enp);
                        row = getFileData(enp, v, el, rows, ct, auth);
                        row.setCreationDate(rows.get(enp).getCreationTime());
                        rows.put(enp, row);



                }
                else if (ct == DiffEntry.ChangeType.DELETE && eop.endsWith(targetExt) && rows.containsKey(eop)) {

                    addBuggedFile(tb, eop);
                    rows.remove(eop);

                }
                else if (ct == DiffEntry.ChangeType.RENAME && eop.endsWith(targetExt) && rows.containsKey(eop)) {

                        CsvRow tempRow=rows.get(eop);
                        tempRow.setVersion(v);
                        tempRow.setFilePath(enp);
                        rows.put(enp, tempRow);
                        rows.remove(eop);
                }
                else if (ct == DiffEntry.ChangeType.COPY && !rows.containsKey(enp)) {

                    CsvRow tempRow=rows.get(eop);
                    tempRow.setVersion(v);
                    tempRow.setFilePath(enp);
                    rows.put(enp, tempRow);
                }

            }

        }

    }

    private void addBuggedFile(TicketBug tb, String file){
        if((tb!=null)&&(!tb.getAffectedFiles().contains(file))){
            tb.addAffectedFile(file);
        }
    }

    private TicketBug getRelatedTicket (RevCommit commit){
        for(TicketBug ticket: this.tickets ){
            if(commit.getFullMessage().contains(ticket.getId())){
                return ticket;
            }
        }
        return null;
    }

    private void writeOutput(Iterator<Map.Entry<String, CsvRow>> it, int j, RevCommit commit){
        while(it.hasNext()){
            Map.Entry<String, CsvRow> me =it.next();
            CsvRow currRow=me.getValue();
            currRow.setVersion(j);
            currRow.ageSetter(commit.getCommitTime());
            output.add(currRow.toStringArray());
        }

    }

    private AbstractTreeIterator getOld(ObjectReader or, RevCommit commit) throws IOException {
        if (commit == null) {
            return new EmptyTreeIterator();
        } else {
            return new CanonicalTreeParser(null, or, commit.getTree().getId());
        }

    }

    public void checkBuggyness(){
        for(TicketBug bug: this.tickets){
            try {
                setBugyness(output, bug);
            }catch(NullPointerException e){
                //just skipping
            }
        }
    }

    private void setBugyness(List<String[]> output, TicketBug bug){
        int v;
        for (String file : bug.getAffectedFiles()) {
            for (String[] row : output) {
                if((v=versionCast(row[0]))==-1) continue;
                if ((row[1].equals(file)) && (v >= bug.getInfectRelease().getVersion() && v < bug.getFixedRelease().getVersion())) {
                    row[14] = "Yes";
                }
            }
        }
    }

    private int versionCast(String number){
        int res;
        try{
            res=Integer.valueOf(number);
        }
        catch(NumberFormatException e){
            res=-1;
        }
        return res;
    }

    private int getAddedLines(Edit edit){
        if(edit.getType() == Edit.Type.INSERT) return edit.getEndB()-edit.getBeginB();
        else if(edit.getType() == Edit.Type.REPLACE){
            int diffA = edit.getEndA()-edit.getBeginA();
            int diffB = edit.getEndB()-edit.getBeginB();
            if(diffA<diffB)
                return diffB-diffA;

        }
        return 0;
    }

    private int getDeletedLines(Edit edit) {
        if (edit.getType() == Edit.Type.DELETE) {
            return edit.getEndA() - edit.getBeginA();
        } else if (edit.getType() == Edit.Type.REPLACE) {
            int diffA = edit.getEndA() - edit.getBeginA();
            int diffB = edit.getEndB() - edit.getBeginB();
            if (diffA > diffB) {
                return diffA - diffB;
            }

        }
        return 0;
    }

    private int getModifiedLines(Edit edit){
        if(edit.getType() == Edit.Type.REPLACE){
            int diffA = edit.getEndA()-edit.getBeginA();
            int diffB = edit.getEndB()-edit.getBeginB();
            if((diffA==diffB)||(diffA<diffB)) return diffA;
            else{
                return diffB;
            }
        }
        return 0;
    }

    private CsvRow getFileData(String file, int vers, List<Edit> editList, HashMap<String, CsvRow> currentRows, DiffEntry.ChangeType ct, PersonIdent auth){
        int loc=0;
        int touchedLoc=0;
        int nRevisions=0;
        int addedLOC=0;
        int maxAddedLoc=0;
        int churn=0;
        int maxChurn=0;
        int revChurn=0;
        int addedLines=0;
        int deletedLines=0;
        int modifiedLines=0;
        double avgAddedLoc=0;
        double avgChurn=0;
        List<PersonIdent> pi=new ArrayList<>();
        CsvRow current=new CsvRow(vers, file);


        for(Edit edit:editList){
            addedLines=getAddedLines(edit);
            deletedLines=getDeletedLines(edit);
            modifiedLines=getModifiedLines(edit);
        }

        if(ct==DiffEntry.ChangeType.MODIFY) {

            CsvRow old= currentRows.get(file);
            loc=old.getLoc();
            touchedLoc=old.getTouchedLoc();
            nRevisions=old.getnRevisions()+1;
            addedLOC=old.getAddedLoc();
            maxAddedLoc=old.getMaxAddedLoc();
            churn=old.getChurn();
            maxChurn=old.getMaxChurn();
            avgAddedLoc=old.getAvgAddedLoc();
            avgChurn=old.getAvgChurn();
            pi=old.getAuthors();

            touchedLoc = touchedLoc + addedLines + deletedLines + modifiedLines;
            addedLOC = addedLOC + addedLines;
            maxAddedLoc = Math.max(maxAddedLoc, addedLines);
            avgAddedLoc = ((avgAddedLoc * (nRevisions)) + addedLines) / nRevisions + 1;
            revChurn = addedLines - deletedLines;
            if (revChurn < 0) {
                revChurn *= -1;
            }

            churn = churn + revChurn;
            maxChurn = Math.max(maxChurn, revChurn);
            avgChurn = ((avgChurn * (nRevisions)) + revChurn) / nRevisions + 1;

        }
        loc = loc + addedLines - deletedLines;
        if(!pi.contains(auth)){ pi.add(auth);}

        current.setLoc(loc);
        current.setTouchedLoc(touchedLoc);
        current.setAddedLoc(addedLOC);
        current.setMaxAddedLoc(maxAddedLoc);
        current.setAvgAddedLoc(avgAddedLoc);
        current.setnRevisions(nRevisions);

        current.setChurn(churn);
        current.setMaxChurn(maxChurn);
        current.setAvgChurn(avgChurn);
        current.setAuthors(pi);

        return current;
    }

    public List<TicketBug> getTickets() {
        return tickets;
    }

    public void setTickets(List<TicketBug> tickets) {
        this.tickets = tickets;
    }


}
