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
    private String outputCsv="C:\\Users\\39320\\Desktop\\Corsi\\isw2\\data\\progetto\\databk.csv";
    private List<RevCommit> commits;
    private List<TicketBug> tickets;

    private Git git;
    private Repository repository;
    private List<Release> releases;


    public JGitRetriever(String path, List<Release> releases, List<TicketBug> tickets) throws GitAPIException, IOException {
        this.path=path;
        this.tickets=tickets;
        this.commits=new ArrayList<>();
        this.releases=releases;
        getRepoAndCommits();
    }

    //creates the repository and gets each of the project commit
    private void getRepoAndCommits() throws IOException, GitAPIException {
        this.repository = new FileRepository(this.path);
        String treeName = "refs/heads/master"; // tag or branch
        this.git= new Git(repository);
        for (RevCommit commit : git.log().add(repository.resolve(treeName)).call()) {
            this.commits.add(commit);
        }
    }




    public void getAnyFileAndData() throws IOException {
        int i = this.commits.size();
        int j = 0;
        RevCommit oldCommit = null;
        RevCommit newCommit;
        List<DiffEntry> diffEntries;
        DiffEntry entry;
        CsvRow row;
        boolean tick=false;
        TicketBug currentTb = null;
        HashMap<String, CsvRow> rows = new HashMap<>();
        String targetExt=".java";

        List<String[]> output = new ArrayList<>();
        output.add(CsvRow.getHeadString());
        OpencsvInterface ocsvi;

        Set<Map.Entry<String, CsvRow>> entrySet;
        Iterator<Map.Entry<String, CsvRow>> it;

        OutputStream outputStream = new ByteArrayOutputStream();
        while (i > 0 && j < this.releases.size()) {
            tick=false;
            if (i != this.commits.size()) oldCommit = this.commits.get(i);
            newCommit = this.commits.get(i - 1);
            PersonIdent auth=newCommit.getCommitterIdent();

            for(TicketBug ticket: this.tickets ){
                if(newCommit.getFullMessage().contains(ticket.getId())){
                    tick=true;
                    currentTb=ticket;
                    break;
                }
            }

            if ((new Date((newCommit.getCommitTime() * 1000L))).after(this.releases.get(j).getDate())) {
                j++;
                entrySet = rows.entrySet();
                it = entrySet.iterator();
                while(it.hasNext()){
                    Map.Entry<String, CsvRow> me =it.next();
                    CsvRow currRow=me.getValue();
                    currRow.setVersion(j);
                    currRow.ageSetter(oldCommit.getCommitTime());
                    output.add(currRow.toStringArray());
                }
            }

            try (ObjectReader reader = git.getRepository().newObjectReader()) {
                AbstractTreeIterator oldTreeIterator;
                if (oldCommit == null) {
                    oldTreeIterator = new EmptyTreeIterator();
                } else {
                    oldTreeIterator = new CanonicalTreeParser(null, reader, oldCommit.getTree().getId());
                }
                AbstractTreeIterator newTreeIterator = new CanonicalTreeParser(null, reader, newCommit.getTree().getId());

                try (DiffFormatter diffFormatter = new DiffFormatter(outputStream)) {
                    diffFormatter.setRepository(git.getRepository());
                    diffEntries = diffFormatter.scan(oldTreeIterator, newTreeIterator);

                    for (int k = 0; k < diffEntries.size(); k++) {

                        entry = diffEntries.get(k);
                        EditList el = diffFormatter.toFileHeader(entry).toEditList();
                        String enp = entry.getNewPath();
                        String eop = entry.getOldPath();
                        DiffEntry.ChangeType ct = entry.getChangeType();

                        if (ct == DiffEntry.ChangeType.ADD) {
                            if (enp.endsWith(targetExt) && !rows.containsKey(enp)) {
                                //newCommit.
                                row = getFileData(enp, j+1, el, rows, ct, auth);
                                row.setCreationDate(newCommit.getCommitTime());
                                rows.put(enp, row);
                            }
                        }
                        else if (ct == DiffEntry.ChangeType.MODIFY){
                            if(enp.endsWith(targetExt) && rows.containsKey(enp))
                            {
                                if(tick&&(!currentTb.getAffectedFiles().contains(enp))) {
                                    currentTb.addAffectedFile(enp);
                                }
                                row = getFileData(enp, j+1, el, rows, ct, auth);
                                row.setCreationDate(rows.get(enp).getCreationTime());
                                rows.put(enp, row);

                            }

                        }
                        else if (ct == DiffEntry.ChangeType.DELETE) {
                            if (eop.endsWith(targetExt) && rows.containsKey(eop)) {
                                if(tick&&(!currentTb.getAffectedFiles().contains(eop))){
                                    currentTb.addAffectedFile(eop);
                                }
                                rows.remove(eop);
                            }
                        }
                        else if (ct == DiffEntry.ChangeType.RENAME) {
                            if (eop.endsWith(targetExt) && rows.containsKey(eop)) {
                                CsvRow tempRow=rows.get(eop);
                                tempRow.setVersion(j+1);
                                tempRow.setFilePath(enp);
                                rows.put(enp, tempRow);
                                rows.remove(eop);
                            }
                        }
                        else if (ct == DiffEntry.ChangeType.COPY && !rows.containsKey(enp)) {

                            CsvRow tempRow=rows.get(eop);
                            tempRow.setVersion(j+1);
                            tempRow.setFilePath(enp);
                            rows.put(enp, tempRow);


                        }


                    }

                }
            }
            i--;
        }

        entrySet = rows.entrySet();

        it = entrySet.iterator();
        while(it.hasNext()){
            Map.Entry<String, CsvRow> me =it.next();
            CsvRow currRow= me.getValue();
            currRow.setVersion(j);
            output.add(currRow.toStringArray());
        }
        setBuggyness(output);

        ocsvi = new OpencsvInterface(this.outputCsv, output);
        ocsvi.writeFile();

    }

    private void setBuggyness(List<String[]> output){
        int v;
        for(TicketBug bug: this.tickets){
            try {

                for (String file : bug.getAffectedFiles()) {
                    for (String[] row : output) {
                        if((v=versionCast(row[0]))==-1) continue;
                        if ((row[1].equals(file)) && (v >= bug.getInfectRelease().getVersion() && v < bug.getFixedRelease().getVersion())) {
                            row[14] = "Yes";
                        }
                    }
                }
            }catch(NullPointerException e){
                //just skipping
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

    private CsvRow getFileData(String file, int vers, List<Edit> editList, HashMap<String, CsvRow> currentRows, DiffEntry.ChangeType ct, PersonIdent auth){
        int loc=0;
        int touchedLoc=0;
        int nRevisions=0;
        int addedLOC=0;
        int maxAddedLoc=0;
        int churn=0;
        int maxChurn=0;
        int revChurn=0;
        int addedLines=0, deletedLines=0, modifiedLines=0;
        double AvgAddedLoc=0;
        double AvgChurn=0;
        List<PersonIdent> pi=new ArrayList<>();
        CsvRow current=new CsvRow(vers, file);


        for(Edit edit:editList){
            if(edit.getType() == Edit.Type.INSERT) { addedLines += edit.getEndB() - edit.getBeginB(); }
            if(edit.getType() == Edit.Type.DELETE) { deletedLines += edit.getEndA() - edit.getBeginA(); }
            if(edit.getType() == Edit.Type.REPLACE){
                int diffA = edit.getEndA()-edit.getBeginA();
                int diffB = edit.getEndB()-edit.getBeginB();
                if(diffA==diffB) modifiedLines += diffA;
                if(diffA<diffB){
                    modifiedLines += diffA;
                    addedLines += diffB-diffA;
                }
                if(diffA>diffB){
                    modifiedLines += diffB;
                    deletedLines += diffA-diffB;
                }
            }
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
            AvgAddedLoc=old.getAvgAddedLoc();
            AvgChurn=old.getAvgChurn();
            pi=old.getAuthors();

            touchedLoc = touchedLoc + addedLines + deletedLines + modifiedLines;
            addedLOC = addedLOC + addedLines;
            maxAddedLoc = Math.max(maxAddedLoc, addedLines);
            AvgAddedLoc = ((AvgAddedLoc * (nRevisions)) + addedLines) / nRevisions + 1;
            revChurn = addedLines - deletedLines;
            if (revChurn < 0) {
                revChurn *= -1;
            }

            churn = churn + revChurn;
            maxChurn = Math.max(maxChurn, revChurn);
            AvgChurn = ((AvgChurn * (nRevisions)) + revChurn) / nRevisions + 1;

        }
        loc = loc + addedLines - deletedLines;
        if(!pi.contains(auth)){ pi.add(auth);}

        current.setLoc(loc);
        current.setTouchedLoc(touchedLoc);
        current.setAddedLoc(addedLOC);
        current.setMaxAddedLoc(maxAddedLoc);
        current.setAvgAddedLoc(AvgAddedLoc);
        current.setnRevisions(nRevisions);

        current.setChurn(churn);
        current.setMaxChurn(maxChurn);
        current.setAvgChurn(AvgChurn);
        current.setAuthors(pi);

        return current;
    }


}
