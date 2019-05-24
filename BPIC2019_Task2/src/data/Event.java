package data;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Date;


public class Event {
    public String caseID;
    public String activityName;
    private Date timestamp;
    public HashMap<String, String> payload;

    public Event(List<String> attributes, String[] values){
        this.caseID = values[0];
        this.activityName = values[1];
        this.timestamp = stringToDate(values[2]);
        payload = new HashMap<>();
        for(int i = 3; i < values.length; i++)
            if(!values[i].equals(""))
                payload.put(attributes.get(i), values[i]);
    }

    public Event(String caseID, String activityName, String timestamp) {
        this.caseID = caseID;
        this.activityName = activityName;
        this.timestamp = stringToDate(timestamp);
        payload = new HashMap<>();
    }

    public Event(Event event){
        this.caseID = event.caseID;
        this.activityName = event.activityName;
        this.timestamp = event.timestamp;
        this.payload = new HashMap<>(event.payload);
    }

    public Date getTimestamp(){
        return timestamp;
    }

    public String toString() {
        return "(" + this.caseID + ", " + this.activityName + ", " + this.timestamp + ", " + payload + ")";
    }

    private Date stringToDate(String timestamp){
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        try{
            Date date = df.parse(timestamp.substring(0, timestamp.indexOf('+')));
            return date;
        }
        catch(Exception ex){
            System.out.println(ex);
            return null;
        }
    }
}