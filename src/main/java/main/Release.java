package main;

import java.util.Date;

public class Release {

    private int version;
    private Date date;

    public Release(Date date, int version){
        this.date=date;
        this.version=version;
    }
    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }



    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }


}
