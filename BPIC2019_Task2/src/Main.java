import data.Event;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        String logPath = args[0];
        HashMap<String, List<Event>> cases = filterActivities(logReader.readLog(logPath));
        List<HashMap<String, List<Event>>> groupedCases = groupByDataFlow(cases);
        for(HashMap<String, List<Event>> group: groupedCases){
            String variant = identifyDataFlow(group);
            Map.Entry<String,List<Event>> entry = cases.entrySet().iterator().next();
            System.out.println("\n" + entry.getValue().get(0).payload.get("(case)_Item_Category") + "\n");
            getThroughputResults(group, variant);
            getNetworthResults(group);
        }
    }

    static List<HashMap<String, List<Event>>> groupByDataFlow(HashMap<String, List<Event>> cases){
        List<HashMap<String, List<Event>>> groupedCases = new ArrayList<>();
        HashMap<String, List<Event>> df1 = new HashMap<>();
        HashMap<String, List<Event>> df2 = new HashMap<>();
        HashMap<String, List<Event>> df3 = new HashMap<>();
        for(String caseID: cases.keySet()){
            if(cases.get(caseID).get(0).payload.get("(case)_Item_Category").equals("3-way match, invoice after GR"))
                df1.put(caseID, cases.get(caseID));
            else if(cases.get(caseID).get(0).payload.get("(case)_Item_Category").equals("3-way match, invoice before GR"))
                df2.put(caseID, cases.get(caseID));
            else if(cases.get(caseID).get(0).payload.get("(case)_Item_Category").equals("2-way match"))
                df3.put(caseID, cases.get(caseID));
        }
        if(df1.size() > 0)
            groupedCases.add(df1);
        if(df2.size() > 0)
            groupedCases.add(df2);
        if(df3.size() > 0)
            groupedCases.add(df3);
        return groupedCases;
    }

    static String identifyDataFlow(HashMap<String, List<Event>> cases){
        Map.Entry<String,List<Event>> entry = cases.entrySet().iterator().next();
        if(entry.getValue().get(0).payload.get("(case)_Item_Category").equals("3-way match, invoice after GR"))
            return "variant1";
        else if(entry.getValue().get(0).payload.get("(case)_Item_Category").equals("3-way match, invoice before GR"))
            return "variant2";
        else if(entry.getValue().get(0).payload.get("(case)_Item_Category").equals("2-way match"))
            return "variant3";
        else
            return "unknown";
    }

    static void getThroughputResults(HashMap<String, List<Event>> cases, String variant){
        switch(variant){
            case "variant1": {
                getResultsVariant1(cases);
                getThroughput(cases, variant);
                break;
            }
            case "variant2":{
                getResultsVariant2(cases);
                getThroughput(cases, variant);
                break;
            }
            case "variant3":{
                getResultsVariant3(cases);
                getThroughput(cases, variant);
                break;
            }
            default: {
                System.out.println("Please, select one of the following variants:\n1) variant1\n2) variant2\n3) variant3");
                break;
            }
        }
    }

    static void getNetworthResults(HashMap<String, List<Event>> cases){
        Double totalPrice = getTotalPrice(cases)/1000000;
        System.out.println("Total net worth: " + totalPrice + " millions");
        System.out.println("Networth per case: " + totalPrice/cases.size() + " millions");
        System.out.println("Networth per item type (in millions): " + getNetWorthPerItemType(cases));
    }

    static void getThroughput(HashMap<String, List<Event>> cases, String variant){
        Integer closedInvoicesAmount = getCloseInvoiceAmount(cases);
        Double totalCaseTime = getTotalTime(cases)/24;
        Integer finishedCases = getNumberOfFinishedCases(cases, variant);
        Integer unfinishedCases = getNumberOfUnfinishedCases(cases, variant);
        System.out.println("\nTotal amount of cleared invoices: " + closedInvoicesAmount);
        System.out.println("Total time: " + totalCaseTime + " days");
        System.out.println("Overall throughput: " + closedInvoicesAmount/totalCaseTime + " payments per day");
        System.out.println("Number of complete cases: " +  finishedCases + " (" + finishedCases/totalCaseTime + " cases per day)");
        System.out.println("Number of incomplete cases: " + unfinishedCases);
        System.out.println("Fraction of completed cases per item type (completed_item/total_item): " + getFractionOFFinishedCases(cases, variant));
        System.out.println("Item frequency in the process: " + getItemTypeFraction(cases));
    }

    static HashMap<String, List<Event>> filterActivities(HashMap<String, List<Event>> cases){
        HashMap<String, List<Event>> processedCases = new HashMap<>();
        ArrayList<String> importantActivities = new ArrayList<>(){
            {
                add("Record Goods Receipt");
                add("Record Invoice Receipt");
                add("Clear Invoice");
            }
        };
        for(String key: cases.keySet()){
            List<Event> events = new ArrayList<>();
            for(Event event: cases.get(key))
                if(importantActivities.contains(event.activityName))
                    events.add(event);
                if(events.size() > 0)
                    processedCases.put(key, events);
        }
        return processedCases;
    }

    static double dateDiffSeconds(Date d1, Date d2){
        return Math.abs((double)(d2.getTime() - d1.getTime())/1000);
    }

    static double dateDiffHours(Date d1, Date d2){
        return dateDiffSeconds(d1, d2)/3600;
    }

    static double dateDiffDays(Date d1, Date d2){
        return dateDiffHours(d1, d2)/24;
    }

    static HashMap<Integer, Integer> getSizeStatistics(HashMap<String, List<Event>> cases){
        HashMap<Integer, Integer> sizes = new HashMap<>();
        for(String key: cases.keySet()){
            if(sizes.containsKey(cases.get(key).size()))
                sizes.put(cases.get(key).size(), sizes.get(cases.get(key).size()) + 1);
            else
                sizes.put(cases.get(key).size(), 1);
        }
        return sizes;
    }

    static HashMap<Long, List<Event>> getCorrespondingTuplesVariant1(HashMap<String, List<Event>> cases){
        HashMap<Long, List<Event>> correspondingEvents = new HashMap<>();
        long j = 0;
        for(String key: cases.keySet()){
            List<Integer> goodsReceipt = new ArrayList<>();
            List<Integer> invoiceReceipt = new ArrayList<>();
            List<Integer> clearInvoice = new ArrayList<>();

            for(int i = 0; i < cases.get(key).size(); i++){
                if(cases.get(key).get(i).activityName.equals("Record Goods Receipt"))
                    goodsReceipt.add(i);
                else if(cases.get(key).get(i).activityName.equals("Record Invoice Receipt"))
                    invoiceReceipt.add(i);
                else
                    clearInvoice.add(i);
            }

            for(int i = 0; i < goodsReceipt.size(); i++){
                List<Event> events = new ArrayList<>();
                for(int k = 0; k < invoiceReceipt.size(); k++)
                    if(invoiceReceipt.get(k) > goodsReceipt.get(i)){
                        for(int l = 0; l < clearInvoice.size(); l++)
                            if(clearInvoice.get(l) > invoiceReceipt.get(k)){
                                events.add(cases.get(key).get(goodsReceipt.get(i)));
                                events.add(cases.get(key).get(invoiceReceipt.get(k)));
                                events.add(cases.get(key).get(clearInvoice.get(l)));
                                correspondingEvents.put(j, events);
                                j++;
                                invoiceReceipt.remove(k);
                                clearInvoice.remove(l);
                                break;
                            }
                            break;
                    }
            }
        }
        return correspondingEvents;
    }

    static HashMap<Long, List<Event>> getCorrespondingTuplesVariant2(HashMap<String, List<Event>> cases){
        HashMap<Long, List<Event>> correspondingEvents = new HashMap<>();
        long j = 0;
        for(String key: cases.keySet()){
            List<Integer> goodsReceipt = new ArrayList<>();
            List<Integer> invoiceReceipt = new ArrayList<>();
            List<Integer> clearInvoice = new ArrayList<>();

            for(int i = 0; i < cases.get(key).size(); i++){
                if(cases.get(key).get(i).activityName.equals("Record Goods Receipt"))
                    goodsReceipt.add(i);
                else if(cases.get(key).get(i).activityName.equals("Record Invoice Receipt"))
                    invoiceReceipt.add(i);
                else
                    clearInvoice.add(i);
            }

            if(goodsReceipt.size() > 0 && invoiceReceipt.size() > 0 && goodsReceipt.get(0) < invoiceReceipt.get(0)){
                for(int i = 0; i < goodsReceipt.size(); i++){
                    List<Event> events = new ArrayList<>();
                    for(int k = 0; k < invoiceReceipt.size(); k++)
                        if(invoiceReceipt.get(k) > goodsReceipt.get(i)){
                            for(int l = 0; l < clearInvoice.size(); l++)
                                if(clearInvoice.get(l) > invoiceReceipt.get(k)){
                                    events.add(cases.get(key).get(goodsReceipt.get(i)));
                                    events.add(cases.get(key).get(invoiceReceipt.get(k)));
                                    events.add(cases.get(key).get(clearInvoice.get(l)));
                                    correspondingEvents.put(j, events);
                                    j++;
                                    invoiceReceipt.remove(k);
                                    clearInvoice.remove(l);
                                    break;
                                }
                            break;
                        }
                }
            }
            else{
                for(int i = 0; i < invoiceReceipt.size(); i++){
                    List<Event> events = new ArrayList<>();
                    for(int k = 0; k < goodsReceipt.size(); k++)
                        if(goodsReceipt.get(k) > invoiceReceipt.get(i)){
                            for(int l = 0; l < clearInvoice.size(); l++)
                                if(clearInvoice.get(l) > goodsReceipt.get(k)){
                                    events.add(cases.get(key).get(invoiceReceipt.get(i)));
                                    events.add(cases.get(key).get(goodsReceipt.get(k)));
                                    events.add(cases.get(key).get(clearInvoice.get(l)));
                                    correspondingEvents.put(j, events);
                                    j++;
                                    goodsReceipt.remove(k);
                                    clearInvoice.remove(l);
                                    break;
                                }
                            break;
                        }
                }
            }
        }
        return correspondingEvents;
    }

    static HashMap<Long, List<Event>> getCorrespondingTuplesVariant3(HashMap<String, List<Event>> cases){
        HashMap<Long, List<Event>> correspondingEvents = new HashMap<>();
        long j = 0;
        for(String key: cases.keySet()){
            List<Integer> invoiceReceipt = new ArrayList<>();
            List<Integer> clearInvoice = new ArrayList<>();

            for(int i = 0; i < cases.get(key).size(); i++){
                if(cases.get(key).get(i).activityName.equals("Record Invoice Receipt"))
                    invoiceReceipt.add(i);
                else
                    clearInvoice.add(i);
            }
            for(int i = 0; i < invoiceReceipt.size(); i++){
                List<Event> events = new ArrayList<>();
                for(int k = 0; k < clearInvoice.size(); k++)
                    if(clearInvoice.get(k) > invoiceReceipt.get(i)){
                        events.add(cases.get(key).get(invoiceReceipt.get(i)));
                        events.add(cases.get(key).get(clearInvoice.get(k)));
                        correspondingEvents.put(j, events);
                        j++;
                        invoiceReceipt.remove(i);
                        clearInvoice.remove(k);
                        break;
                    }
            }
        }
        return correspondingEvents;
    }

    static List<Double> getGoodsAndInvoiceTime(HashMap<Long, List<Event>> correspondingEvents){
        List<Double> results = new ArrayList<>();
        List<Double> time = new ArrayList<>();
        for(Long key: correspondingEvents.keySet()){
            Event goodsReceipt = null;
            Event invoiceReceipt = null;
            for(Event event: correspondingEvents.get(key)){
                if(event.activityName.equals("Record Goods Receipt"))
                    goodsReceipt = event;
                else if(event.activityName.equals("Record Invoice Receipt"))
                    invoiceReceipt = event;
            }
            time.add(dateDiffHours(invoiceReceipt.getTimestamp(), goodsReceipt.getTimestamp()));
        }
        results.add(time.stream().mapToDouble(v -> v).min().getAsDouble()); // minimum
        results.add(time.stream().mapToDouble(a -> a).average().getAsDouble()); // average
        results.add(getMedian(time)); // median
        results.add(time.stream().mapToDouble(v -> v).max().getAsDouble()); // maximum
        return results;
    }

    static List<Double> getInvoiceAndPaymentTime(HashMap<Long, List<Event>> correspondingEvents){
        List<Double> results = new ArrayList<>();
        List<Double> time = new ArrayList<>();
        for(Long key: correspondingEvents.keySet()){
            Event invoiceReceipt = null;
            Event clearInvoice = null;
            for(Event event: correspondingEvents.get(key)){
                if(event.activityName.equals("Record Invoice Receipt"))
                    invoiceReceipt = event;
                else if(event.activityName.equals("Clear Invoice"))
                    clearInvoice = event;
            }
            time.add(dateDiffHours(clearInvoice.getTimestamp(), invoiceReceipt.getTimestamp()));
        }
        results.add(time.stream().mapToDouble(v -> v).min().getAsDouble()); // minimum
        results.add(time.stream().mapToDouble(a -> a).average().getAsDouble()); // average
        results.add(getMedian(time)); // median
        results.add(time.stream().mapToDouble(v -> v).max().getAsDouble()); // maximum
        return results;
    }

    static List<Double> getGoodsAndPaymentTime(HashMap<Long, List<Event>> correspondingEvents){
        List<Double> results = new ArrayList<>();
        List<Double> time = new ArrayList<>();
        for(Long key: correspondingEvents.keySet()){
            Event goodsReceipt = null;
            Event clearInvoice = null;
            for(Event event: correspondingEvents.get(key)){
                if(event.activityName.equals("Record Goods Receipt"))
                    goodsReceipt = event;
                else if(event.activityName.equals("Clear Invoice"))
                    clearInvoice = event;
            }
            time.add(dateDiffHours(clearInvoice.getTimestamp(), goodsReceipt.getTimestamp()));
        }
        results.add(time.stream().mapToDouble(v -> v).min().getAsDouble()); // minimum
        results.add(time.stream().mapToDouble(a -> a).average().getAsDouble()); // average
        results.add(getMedian(time)); // median
        results.add(time.stream().mapToDouble(v -> v).max().getAsDouble()); // maximum
        return results;
    }

    static void getResultsVariant1(HashMap<String, List<Event>> cases){
        HashMap<Long, List<Event>> correspondingEvents = getCorrespondingTuplesVariant1(cases);
        System.out.println("\nTime between goods receipt and invoice receipt:");
        List<Double> results = getGoodsAndInvoiceTime(correspondingEvents);
        System.out.println("min - " + results.get(0) + " hours\n" + "average - " + results.get(1) + " hours\n" + "median - " + results.get(2) + " hours\n" + "max - " + results.get(3) + " hours");

        System.out.println("\nTime between invoice receipt and payment:");
        results = getInvoiceAndPaymentTime(correspondingEvents);
        System.out.println("min - " + results.get(0) + " hours\n" + "average - " + results.get(1) + " hours\n" + "median - " + results.get(2) + " hours\n" + "max - " + results.get(3) + " hours");

        System.out.println("\nTime between goods receipt and payment:");
        results = getGoodsAndPaymentTime(correspondingEvents);
        System.out.println("min - " + results.get(0) + " hours\n" + "average - " + results.get(1) + " hours\n" + "median - " + results.get(2) + " hours\n" + "max - " + results.get(3) + " hours");
    }

    static void getResultsVariant2(HashMap<String, List<Event>> cases){
        HashMap<Long, List<Event>> correspondingEvents = getCorrespondingTuplesVariant2(cases);

        System.out.println("\nTime between goods receipt and invoice receipt:");
        List<Double> results = getGoodsAndInvoiceTime(correspondingEvents);
        System.out.println("min - " + results.get(0) + " hours\n" + "average - " + results.get(1) + " hours\n" + "median - " + results.get(2) + " hours\n" + "max - " + results.get(3) + " hours");

        System.out.println("\nTime between invoice receipt and payment:");
        results = getInvoiceAndPaymentTime(correspondingEvents);
        System.out.println("min - " + results.get(0) + " hours\n" + "average - " + results.get(1) + " hours\n" + "median - " + results.get(2) + " hours\n" + "max - " + results.get(3) + " hours");

        System.out.println("\nTime between goods receipt and payment:");
        results = getGoodsAndPaymentTime(correspondingEvents);
        System.out.println("min - " + results.get(0) + " hours\n" + "average - " + results.get(1) + " hours\n" + "median - " + results.get(2) + " hours\n" + "max - " + results.get(3) + " hours");
    }

    static void getResultsVariant3(HashMap<String, List<Event>> cases){
        HashMap<Long, List<Event>> correspondingEvents = getCorrespondingTuplesVariant3(cases);
        System.out.println("\nTime between invoice receipt and payment:");
        List<Double> results = getInvoiceAndPaymentTime(correspondingEvents);
        System.out.println("min - " + results.get(0) + " hours\n" + "average - " + results.get(1) + " hours\n" + "median - " + results.get(2) + " hours\n" + "max - " + results.get(3) + " hours");
    }

    static Integer getCloseInvoiceAmount(HashMap<String, List<Event>> cases){
        int sum = 0;
        for(String caseID: cases.keySet()){
            for(Event event: cases.get(caseID))
                if(event.activityName.equals("Clear Invoice"))
                    sum++;
        }
        return sum;
    }

    static Double getTotalTime(HashMap<String, List<Event>> cases){
        Map.Entry<String,List<Event>> entry = cases.entrySet().iterator().next();
        List<Event> events = entry.getValue();
        Date earliestDate = events.get(0).getTimestamp();
        Date latestDate = events.get(events.size() - 1).getTimestamp();
        for(String caseID: cases.keySet()){
            for(Event event: cases.get(caseID)){
                if (event.getTimestamp().compareTo(earliestDate) < 0)
                    earliestDate = event.getTimestamp();
                if (event.getTimestamp().compareTo(latestDate) > 0)
                    latestDate = event.getTimestamp();
            }
        }
        return dateDiffHours(latestDate, earliestDate);
    }

    static double getMedian(List<Double> results){
        Collections.sort(results);
        return results.get(results.size()/2);
    }

    static double getTotalPrice(HashMap<String, List<Event>> cases){
        Double cost = 0.0;
        for(String caseID: cases.keySet()){
            for(Event event: cases.get(caseID)){
                if(event.activityName.equals("Clear Invoice"))
                    cost += Double.parseDouble(event.payload.get("Cumulative_net_worth_(EUR)"));
            }
        }
        return cost;
    }

    static HashMap<String, Double> getNetWorthPerItemType(HashMap<String, List<Event>> cases){
        HashMap<String, Double> networthPerItemType = new HashMap<>();
        for(String caseID: cases.keySet()){
            for(Event event: cases.get(caseID)){
                if(event.activityName.equals("Clear Invoice")){
                    String itemType = event.payload.get("(case)_Item_Type");
                    if(!networthPerItemType.containsKey(itemType))
                        networthPerItemType.put(itemType, Double.parseDouble(event.payload.get("Cumulative_net_worth_(EUR)"))/1000000);
                    else
                        networthPerItemType.put(itemType, networthPerItemType.get(itemType) + Double.parseDouble(event.payload.get("Cumulative_net_worth_(EUR)"))/1000000);
                }
            }
        }
        if(networthPerItemType.size() > 0){
            for(String key: networthPerItemType.keySet()){
                BigDecimal bd = new BigDecimal(Double.toString(networthPerItemType.get(key)));
                bd = bd.setScale(3, RoundingMode.HALF_UP);
                networthPerItemType.put(key, bd.doubleValue());
            }
        }
        return networthPerItemType;
    }

    static Integer getNumberOfFinishedCases(HashMap<String, List<Event>> cases, String variant){
        Integer finishedCases = 0;
        for(String caseID: cases.keySet()){
            Integer goodsReceipt = 0;
            Integer invoiceReceipt = 0;
            Integer clearInvoice = 0;
            for(Event event: cases.get(caseID)){
                if(event.activityName.equals("Record Goods Receipt"))
                    goodsReceipt++;
                else if(event.activityName.equals("Record Invoice Receipt"))
                    invoiceReceipt++;
                else if(event.activityName.equals("Clear Invoice"))
                    clearInvoice++;
            }
            if(variant.equals("variant1") || variant.equals("variant2")){
                if(goodsReceipt == invoiceReceipt && goodsReceipt == clearInvoice)
                    finishedCases++;
            }
            else if(variant.equals("variant3")){
                if(invoiceReceipt == clearInvoice)
                    finishedCases++;
            }
        }
        return finishedCases;
    }

    static Integer getNumberOfUnfinishedCases(HashMap<String, List<Event>> cases, String variant){
        return cases.size() - getNumberOfFinishedCases(cases, variant);
    }

    static HashMap<String, Double> getFractionOFFinishedCases(HashMap<String, List<Event>> cases, String variant){
        List<String> itemTypes = new ArrayList<>();
        for(String caseID: cases.keySet()){
            String itemType = cases.get(caseID).get(0).payload.get("(case)_Item_Type");
            if(!itemTypes.contains(itemType))
                itemTypes.add(itemType);
        }
        HashMap<String, Integer> finishedCases = new HashMap<>();
        HashMap<String, Integer> unfinishedCases = new HashMap<>();
        for(String itemType: itemTypes){
            HashMap<String, List<Event>> casesForItemType = getCasesForItemType(cases, itemType);
            finishedCases.put(itemType, getNumberOfFinishedCases(casesForItemType, variant));
            unfinishedCases.put(itemType, getNumberOfUnfinishedCases(casesForItemType, variant));
        }
        HashMap<String, Double> fractionOfFinishedCases = new HashMap<>();
        for(String itemType: itemTypes)
            fractionOfFinishedCases.put(itemType, (double)finishedCases.get(itemType)/(finishedCases.get(itemType) + unfinishedCases.get(itemType)));
        if(fractionOfFinishedCases.size() > 0){
            for(String key: fractionOfFinishedCases.keySet()){
                BigDecimal bd = new BigDecimal(Double.toString(fractionOfFinishedCases.get(key)));
                bd = bd.setScale(3, RoundingMode.HALF_UP);
                fractionOfFinishedCases.put(key, bd.doubleValue());
            }
        }
        return fractionOfFinishedCases;
    }

    static HashMap<String, List<Event>> getCasesForItemType(HashMap<String, List<Event>> cases, String itemType){
        HashMap<String, List<Event>> casesForItemType = new HashMap<>();
        for(String caseID: cases.keySet()){
            if(cases.get(caseID).get(0).payload.get("(case)_Item_Type").equals(itemType))
                casesForItemType.put(caseID, cases.get(caseID));
        }
        return casesForItemType;
    }

    static HashMap<String, Double> getItemTypeFraction(HashMap<String, List<Event>> cases){
        HashMap<String, Double> itemTypeFraction = new HashMap<>();
        for(String caseID: cases.keySet()){
            String itemType = cases.get(caseID).get(0).payload.get("(case)_Item_Type");
            if(!itemTypeFraction.containsKey(itemType))
                itemTypeFraction.put(itemType, 1.0);
            else
                itemTypeFraction.put(itemType, itemTypeFraction.get(itemType) + 1.0);
        }
        if(itemTypeFraction.size() > 0){
            for(String itemType: itemTypeFraction.keySet()){
                BigDecimal bd = new BigDecimal(Double.toString(itemTypeFraction.get(itemType)/cases.size()));
                bd = bd.setScale(3, RoundingMode.HALF_UP);
                itemTypeFraction.put(itemType, bd.doubleValue());
            }
        }
        return itemTypeFraction;
    }
}