import com.opencsv.CSVWriter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.diff.*;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.*;
import java.lang.module.ModuleDescriptor;
import java.text.MessageFormat;
import java.util.*;

public class JGitRetriever {


    private String path;
    private String outputCsv="C:\\Users\\39320\\Desktop\\Corsi\\isw2\\data\\progetto\\databk.csv";
    private List<RevCommit> commits;
    private List<TicketBug> tickets;

    private Git git;
    private Repository repository;
    private List<Release> releases;
    //private List<csvRow> startingFiles;


    public JGitRetriever(String path, List<Release> releases, List<TicketBug> tickets) throws GitAPIException, IOException {
        this.path=path;
        this.tickets=tickets;
        this.commits=new ArrayList<>();
        //this.startingFiles=new ArrayList<>();
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
        int i = this.commits.size(), j = 0, repeat = 0;
        RevCommit oldCommit = null, newCommit;
        List<DiffEntry> diffEntries;
        DiffEntry entry;
        csvRow row;
        boolean tick=false;
        TicketBug currentTb = null;
        HashMap<String, csvRow> rows = new HashMap<>();

        List<String[]> output = new ArrayList<>();
        output.add(csvRow.getHeadString());
        OpencsvInterface ocsvi;

        Set entrySet;
        Iterator it;

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
                    Map.Entry me = (Map.Entry)it.next();
                    csvRow currRow=(csvRow) me.getValue();
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
                            if (enp.endsWith(".java") && !rows.containsKey(enp)) {
                                //newCommit.
                                row = getFileData(enp, j+1, el, rows, ct, auth);
                                row.setCreationDate(newCommit.getCommitTime());
                                rows.put(enp, row);
                            }
                        }
                        else if (ct == DiffEntry.ChangeType.MODIFY){
                            if(enp.endsWith(".java") && rows.containsKey(enp))
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
                            if (eop.endsWith(".java") && rows.containsKey(eop)) {
                                if(tick&&(!currentTb.getAffectedFiles().contains(eop))){
                                    currentTb.addAffectedFile(eop);
                                }
                                rows.remove(eop);
                            }
                        }
                        else if (ct == DiffEntry.ChangeType.RENAME) {
                            if (eop.endsWith(".java") && rows.containsKey(eop)) {
                                csvRow tempRow=rows.get(eop);
                                tempRow.setVersion(j+1);
                                tempRow.setFilePath(enp);
                                rows.put(enp, tempRow);
                                rows.remove(eop);
                            }
                        }
                        else if (ct == DiffEntry.ChangeType.COPY) {
                            if (!rows.containsKey(enp)) {
                                csvRow tempRow=rows.get(eop);
                                tempRow.setVersion(j+1);
                                tempRow.setFilePath(enp);
                                rows.put(enp, tempRow);
                            }

                        }


                    }

                }
            }
            i--;
        }

        entrySet = rows.entrySet();

        it = entrySet.iterator();
        while(it.hasNext()){
            Map.Entry me = (Map.Entry)it.next();
            csvRow currRow=(csvRow) me.getValue();
            currRow.setVersion(j);
            output.add(currRow.toStringArray());
        }
        setBuggyness(output);

        ocsvi = new OpencsvInterface(this.outputCsv, output);
        ocsvi.writeFile();

    }

    private void setBuggyness(List<String[]> output){
        int v, it=0;
        for(TicketBug bug: this.tickets){
            try {

                for (String file : bug.getAffectedFiles()) {
                    for (String[] row : output) {
                        it++;
                        try {
                            v = Integer.valueOf(row[0]);
                        }catch(NumberFormatException e){continue;}
                        if ((row[1].equals(file)) && (v >= bug.getInfectRelease().getVersion() && v < bug.getFixedRelease().getVersion())) {
                            row[14] = "Yes";
                        }
                    }
                }
            }catch(NullPointerException e){

            }
        }



    }

    private csvRow getFileData(String file, int vers, List<Edit> editList, HashMap<String,csvRow> currentRows, DiffEntry.ChangeType ct, PersonIdent auth){
        int LOC=0, touchedLOC=0, NRevisions=0, addedLOC=0, maxAddedLOC=0, churn=0, maxChurn=0, revChurn=0;
        int addedLines=0, deletedLines=0, modifiedLines=0;
        double AVGAddedLOC=0, AVGChurn=0;
        List<PersonIdent> pi=new ArrayList<>();
        csvRow current=new csvRow(vers, file);


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

            csvRow old= currentRows.get(file);
            LOC=old.getLOC();
            touchedLOC=old.getTouchedLOC();
            NRevisions=old.getNRevisions()+1;
            addedLOC=old.getAddedLOC();
            maxAddedLOC=old.getMaxAddedLOC();
            churn=old.getChurn();
            maxChurn=old.getMaxChurn();
            AVGAddedLOC=old.getAVGAddedLOC();
            AVGChurn=old.getAVGChurn();
            pi=old.getAuthors();

            touchedLOC = touchedLOC + addedLines + deletedLines + modifiedLines;
            addedLOC = addedLOC + addedLines;
            maxAddedLOC = Math.max(maxAddedLOC, addedLines);
            AVGAddedLOC = ((AVGAddedLOC * (NRevisions)) + addedLines) / NRevisions + 1;
            revChurn = addedLines - deletedLines;
            if (revChurn < 0) {
                revChurn *= -1;
            }

            churn = churn + revChurn;
            maxChurn = Math.max(maxChurn, revChurn);
            AVGChurn = ((AVGChurn * (NRevisions)) + revChurn) / NRevisions + 1;

        }
        LOC = LOC + addedLines - deletedLines;
        if(!pi.contains(auth)){ pi.add(auth);}

        current.setLOC(LOC);
        current.setTouchedLOC(touchedLOC);
        current.setAddedLOC(addedLOC);
        current.setMaxAddedLOC(maxAddedLOC);
        current.setAVGAddedLOC(AVGAddedLOC);
        current.setNRevisions(NRevisions);

        current.setChurn(churn);
        current.setMaxChurn(maxChurn);
        current.setAVGChurn(AVGChurn);
        current.setAuthors(pi);

        return current;
    }


}
