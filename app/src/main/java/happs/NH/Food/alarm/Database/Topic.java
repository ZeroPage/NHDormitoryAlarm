package happs.NH.Food.alarm.Database;

/**
 * Created by SH on 2016-04-03.
 */
public class Topic {
    private String tname = "";
    private int mode = 1;

    public Topic(final String n, final int m){
        this.setTopic(n, m);
    }

    public void setTopic(final String n, final int m){
        this.tname = n;
        this.mode = m;
    }

    public String getName(){
        return this.tname;
    }

    public int getMode(){
        return this.mode;
    }

}
