import data.Event;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.in.XesXmlGZIPParser;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public final class logReader {
    public static HashMap<String, List<Event>> readLog(String path){
        HashMap<String, List<Event>> cases = new HashMap<>();
        String format = path.substring(path.lastIndexOf(".") + 1);
        if(format.equals("csv"))
            cases = logReader.readCSV(path);
        else if(format.equals("xes"))
            cases = logReader.readXES(path);
        return cases;
    }


    public static HashMap<String, List<Event>> readCSV(String path){
        HashMap<String, List<Event>> cases = new HashMap<>();
        List<Event> events = new ArrayList();
        List<String> attributes = new ArrayList();
        int counter = 0;
        String line;

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {

            while ((line = br.readLine()) != null) {

                String[] row = line.split("[,;]");
                if(counter == 0) {
                    counter++;
                    Collections.addAll(attributes, row);
                }
                else {
                    events.add(new Event(attributes, row));
                    counter++;
                }
            }
            for(Event event: events)
                if (!cases.containsKey(event.caseID)) {
                    List<Event> list = new ArrayList<>();
                    list.add(event);
                    cases.put(event.caseID, list);
                } else cases.get(event.caseID).add(event);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return cases;
    }

    public static HashMap<String, List<Event>> readXES(String path){
        HashMap<String, List<Event>> cases = new HashMap<>();
        final String conceptname = "concept:name";
        final String timestamp = "time:timestamp";

        try {
            File xesFile = new File(path);
            XesXmlParser parser = new XesXmlParser(new XFactoryNaiveImpl());
            if (!parser.canParse(xesFile)) {
                parser = new XesXmlGZIPParser();
                if (!parser.canParse(xesFile)) {
                    System.out.println("Unparsable log file: " + xesFile.getAbsolutePath());
                }
            }
            List<XLog> xLogs = parser.parse(xesFile);
            XLog xLog = xLogs.remove(0);

            for(int i = 0; i < xLog.size(); i++){
                XTrace trace = xLog.get(i);
                String traceID = ((XAttributeLiteral) trace.getAttributes().get(conceptname)).getValue();
                List<Event> events = new ArrayList();
                for(int j = 0; j < trace.size(); j++){
                    List<String> attributes = new ArrayList<>();
                    String[] values = new String[trace.get(j).getAttributes().size() + 1];
                    values[0] = traceID;
                    attributes.add("CaseID");
                    values[1] = trace.get(j).getAttributes().get(conceptname).toString();
                    attributes.add("Activity");
                    values[2] = trace.get(j).getAttributes().get(timestamp).toString();
                    attributes.add("Timestamp");

                    int k = 3;

                    for(String s: trace.get(j).getAttributes().keySet()){
                        switch(s){
                            case timestamp:
                                break;
                            case conceptname:
                                break;
                            default: {
                                attributes.add(s);
                                values[k] = trace.get(j).getAttributes().get(s).toString();
                                k++;
                            }
                        }
                    }
                    events.add(new Event(attributes, values));
                }
                cases.put(traceID, events);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return cases;
    }
}
