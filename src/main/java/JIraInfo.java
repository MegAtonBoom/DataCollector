import com.opencsv.CSVWriter;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.lang.module.ModuleDescriptor;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class JIraInfo {

    private static String projName="BOOKKEEPER";
    private static List<Release> releases=new ArrayList<>();
    private static final double P=1.5;

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public static JSONArray readJsonArrayFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            JSONArray json = new JSONArray(jsonText);
            return json;
        } finally {
            is.close();
        }
    }

    public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            JSONObject json = new JSONObject(jsonText);
            return json;
        } finally {
            is.close();
        }
    }



    public static void main(String[] args) throws IOException, JSONException, GitAPIException, ParseException {
        JGitRetriever jgr;

        Integer j = 0, i = 0, total = 1;
        List<String> versNumbers= new ArrayList<String>();
        List<Date> dates=new ArrayList<>();
        List<TicketBug> bugs=new ArrayList<>();
        //Get JSON API for closed bugs w/ AV in the project
        do {
            //Only gets a max of 1000 at a time, so must do this multiple times if bugs >1000
            j = i + 1000;

            String url="https://issues.apache.org/jira/rest/api/2/project/"+projName+"/version?&maxResults="+j.toString()+"&startAt="+i.toString();
            JSONObject json = readJsonFromUrl(url);


            total = json.getInt("total");
            JSONArray versions = json.getJSONArray("values");

            for (; i < total && i < j; i++) {

                try {
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                    String stringDate = versions.getJSONObject(i % 1000).get("releaseDate").toString();
                    Date date = formatter.parse(stringDate);
                    String version = versions.getJSONObject(i % 1000).get("name").toString();
                    versNumbers.add(version);
                    dates.add(date);
                }
                catch(JSONException e){
                }

            }
        } while (i < total);

        Collections.sort(dates);
        for( i=0; i<dates.size();i++){
            releases.add(new Release(dates.get(i),i+1));
        }
        bugs=getJira();
        jgr=new JGitRetriever("C:\\Users\\39320\\Desktop\\Corsi\\isw2\\bookkeeper3\\.git", releases, bugs);
        //jgr.diffCommit();

        jgr.getAnyFileAndData();



        return;
    }


    private static List<TicketBug> getJira() throws IOException, ParseException {

        Integer j = 0, i = 0, total = 1, versNum, ivRelease=0;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date resolutionDate, openDate, infectDate;
        Release infectRelease = null, ovRelease = null, fvRelease = null;
        List<Date> avs=new ArrayList<>();
        List<TicketBug> allBugs=new ArrayList<>();
        TicketBug currTicket;

        //Get JSON API for closed bugs w/ AV in the project
        do {

            //Only gets a max of 1000 at a time, so must do this multiple times if bugs >1000
            j = i + 1000;

            String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
                    + projName + "%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22OR"
                    + "%22status%22=%22resolved%22)AND%22resolution%22=%22fixed%22&fields=key,resolutiondate,versions,created&startAt="
                    + i.toString() + "&maxResults=" + j.toString();

            JSONObject json = readJsonFromUrl(url);
            JSONArray bugs = json.getJSONArray("issues");
            total=json.getInt("total");
            for (; i < total && i < j; i++) {
                avs=new ArrayList<>();
                String key=bugs.getJSONObject(i%1000).get("key").toString();
                JSONArray versions=bugs.getJSONObject(i%1000).getJSONObject("fields").getJSONArray("versions");
                if( (versNum=versions.length())!=0){
                    for(int y=0; y<versNum; y++){
                        if(versions.getJSONObject(y).has("releaseDate")){
                            avs.add(formatter.parse(versions.getJSONObject(y).get("releaseDate").toString()));
                        }
                    }

                    Collections.sort(avs);
                }
                resolutionDate=formatter.parse(bugs.getJSONObject(i%1000).getJSONObject("fields").getString("resolutiondate").substring(0,10));
                if((resolutionDate=getRelease(resolutionDate))==null) continue;
                openDate=formatter.parse(bugs.getJSONObject(i%1000).getJSONObject("fields").getString("created").substring(0,10));
                if((openDate=getRelease(openDate))==null) continue;

                if(!checkInfoConsistency(openDate,resolutionDate,avs)) continue;


                if(versNum!=0){
                    infectDate=avs.get(0);
                    infectRelease=null;
                    for(int y=0;y<releases.size();y++){
                        if(infectDate.equals(releases.get(y).getDate())){
                            infectRelease=releases.get(y);
                        }
                    }
                    if(infectRelease==null) continue;
                }
                else{
                    for(int y=0;y<releases.size();y++){
                        if(resolutionDate.equals(releases.get(y).getDate())) fvRelease=releases.get(y);

                        if(openDate.equals(releases.get(y).getDate())) ovRelease=releases.get(y);
                    }
                    infectRelease=getInfectedRelease(ovRelease,fvRelease);
                }
                if(fvRelease==null||ovRelease==null||infectRelease==null) continue;
                allBugs.add(new TicketBug(key, infectRelease, fvRelease));

            }
        } while (i < total);
        return allBugs;
    }

    private static boolean checkInfoConsistency(Date ov, Date fv, List<Date> avs){
        boolean consistent=false;
        if(fv.equals(ov)) return false;
        if(avs.size()!=0){
            if(avs.get(0).after(ov)) return false;
        }
        if(avs.size()!=0){
            for(int y=0; y<avs.size();y++){
                if(avs.get(y).equals(ov)) consistent=true;
            }
            if(!consistent) return false;
            if(avs.get(avs.size()-1).after(fv)||avs.get(avs.size()-1).equals(fv)) return false;
        }
        return true;
    }


    //Cold Start approach
    private static Release getInfectedRelease(Release ovRel, Release fvRel){
        int ov=ovRel.getVersion(), fv= fvRel.getVersion();
        int iv=(int)Math.floor(fv-((fv-ov)*P));
        if(iv<0){
            return releases.get(0);
        }
        else { return releases.get(iv-1); }

    }



    private static Date getRelease(Date target){
        Date prev, next;
        for(int i=0; i<releases.size()-1; i++){
            prev=releases.get(i).getDate();
            next=releases.get(i+1).getDate();

            if(target.after(prev) && target.before(next)) return next;

        }
        return null;
    }




}
