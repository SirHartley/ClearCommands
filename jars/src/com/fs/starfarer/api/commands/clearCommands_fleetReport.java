package com.fs.starfarer.api.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.*;
import java.util.regex.Pattern;



public class clearCommands_fleetReport implements BaseCommand
{
    public void log(String s){
        Console.showMessage(s);
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        FactionAPI fact = null;
        if(args != null && !args.isEmpty()){
            for(FactionAPI faction : Global.getSector().getAllFactions()){
                if(faction.getId().equals(args)
                        || faction.getDisplayName().equals(args)
                        || faction.getDisplayName().toLowerCase().equals(args)){

                    fact = faction;
                    break;
                }
            }

            if(fact == null){
                Console.showMessage("Invalid faction ID");
                return CommandResult.BAD_SYNTAX;
            }
        }

        boolean specific = fact != null;

        //routes
        Map<String, Float> routeSourceCounts = new HashMap<>();
        Set<RouteManager.RouteData> routeSet = getAllRoutes();

        int factionRouteCount = 0;
        for (RouteManager.RouteData data : routeSet){
            if(specific && !data.getFactionId().equals(fact.getId())) continue;
            factionRouteCount++;
            addToRouteSourceCount(data, routeSourceCounts);
        }

        int routeCount = specific? factionRouteCount : routeSet.size();

        //Route fleet counts
        Set<CampaignFleetAPI> unknownFleetList = new HashSet<>();
        Set<CampaignFleetAPI> routeFleetList = new HashSet<>();
        for(RouteManager.RouteData route : routeSet){
            if(route.getActiveFleet() != null){
                if(!specific || route.getActiveFleet().getFaction() == fact) routeFleetList.add(route.getActiveFleet());
            }
        }

        //Route Keyword groups
        Map<String, Float> keyWordMap = getKeyWordGrouping(routeSourceCounts);

        //fleets
        Set<CampaignFleetAPI> fleetList = getFleets();
        Set<CampaignFleetAPI> factionFleetList = new HashSet<>();
        Map<String, Float> factionFleetCount = new HashMap<>();
        Map<String, Float> assignmentCounter = new HashMap<>();
        Map<String, Float> listenerCounter = new HashMap<>();

        int importantFleetCount = 0;
        int stationFleetCount = 0;
        int noAssignment = 0;

        for (CampaignFleetAPI fleet : fleetList){
            if(specific && fleet.getFaction() != fact) continue;
            if(specific) factionFleetList.add(fleet);

            //faction fleet count
            addToStringMap((fleet.getFaction().getDisplayName() + " ["  + fleet.getFaction().getId() + "]"), factionFleetCount);
            if(fleet.getCurrentAssignment() != null){
                addToStringMap(fleet.getCurrentAssignment().getAssignment().getDescription(), assignmentCounter);
            } else if(!fleet.isStationMode()){
                noAssignment++;
            }

            //importants and stations
            if(fleet.getMemoryWithoutUpdate().getBoolean(MemFlags.ENTITY_MISSION_IMPORTANT)) importantFleetCount++;
            if(fleet.isStationMode()) stationFleetCount++;

            //listeners
            if(fleet.getEventListeners() != null){
                for(FleetEventListener l : fleet.getEventListeners()){
                    addToStringMap(l.getClass().getSimpleName(), listenerCounter);
                }
            }

            //unknowns
            if(!routeFleetList.contains(fleet) && !fleet.isStationMode()) unknownFleetList.add(fleet);
        }

        //sort the maps
        factionFleetCount = sortByLargestValue(factionFleetCount);
        routeSourceCounts = sortByLargestValue(routeSourceCounts);
        assignmentCounter = sortByLargestValue(assignmentCounter);
        listenerCounter = sortByLargestValue(listenerCounter);
        keyWordMap = sortByLargestValue(keyWordMap);

        //toggle counts if faction is specified
        int totalFleetCount = specific ? factionFleetList.size() : fleetList.size();

        //report start
        String header = specific ? fact.getDisplayName() + " Fleet Report" : "Fleet Report";

        log("--------------------- " + header + " ---------------------");
        log("Total fleet count: " + totalFleetCount);
        log("Important fleets: " + importantFleetCount);
        log("Station fleets: " + stationFleetCount);
        log("Route Fleets: " + routeFleetList.size());
        log("Unknown source Fleets: " + unknownFleetList.size());

        StringBuilder andMore = new StringBuilder();

        if(!specific){
            log("--------------------- Fleets by Factions");

            for(Map.Entry<String, Float> entry : factionFleetCount.entrySet()){
                if(entry.getValue() >= 10){
                    log(entry.getKey() + " - " + entry.getValue());
                } else {
                    andMore.append(entry.getKey());
                    andMore.append("; ");
                }
            }
            log("Factions with <10 fleets: ");
            log(andMore.toString());
        }

        log("--------------------- Assignments");
        log("Fleets without assignment (excl. stations): " + noAssignment);

        log("Fleet Assignment count: ");

        andMore.delete(0, andMore.length());

        for(Map.Entry<String, Float> entry : assignmentCounter.entrySet()){
            if(entry.getValue() >= 10 || specific){
                log(entry.getKey() + " - " + entry.getValue());
            } else {
                andMore.append(entry.getKey());
                andMore.append("; ");
            }
        }

        if(!specific) log("Assignments with <10 counts: ");
        if(!specific) log(andMore.toString());

        log("--------------------- Listeners");
        log("Class (Groups Instances) - Listening to X fleets:");

        andMore.delete(0, andMore.length());

        for(Map.Entry<String, Float> entry : listenerCounter.entrySet()){
            if(entry.getValue() >= 10 || specific){
                log(entry.getKey() + " - " + entry.getValue());
            } else {
                andMore.append(entry.getKey());
                andMore.append("; ");
            }
        }

        if(!specific) log("Listeners with <10 fleets: ");
        if(!specific) log(andMore.toString());

        log("--------------------- Routes:");
        log("Total registered Routes (Vanilla RouteManager): " + routeCount);

        log("-------- Routes by Keywords (inaccurate):");

        for(Map.Entry<String, Float> entry : keyWordMap.entrySet()) {
            log(entry.getKey() + " - " + entry.getValue());
        }

        log("-------- Routes by Source:");
        andMore.delete(0, andMore.length());

        for(Map.Entry<String, Float> entry : routeSourceCounts.entrySet()){
            if(entry.getValue() >= 10 || specific){
                log(entry.getKey() + " - " + entry.getValue());
            } else {
                andMore.append(entry.getKey());
                andMore.append("; ");
            }
        }

        if(!specific) log("Sources with <10 counts: ");
        if(!specific) log(andMore.toString());
        log("--------------------- Report End ---------------------");

        return CommandResult.SUCCESS;
    }

    private Map<String, Float> getKeyWordGrouping(Map<String, Float> routeSourceCounts){
        //Get all fleetmanagers
        //get their names
        //Split the strings into parts, use _ as seperator

        Map<String, Float> keyWordCountMap = new HashMap<>();
        Map<String, Float> routeByKeyWordCountMap = new HashMap<>();

        char c = '_';
        for(Map.Entry<String, Float> entry : routeSourceCounts.entrySet()){
            for (String s : splitStrings(entry.getKey(), c)){
                addToStringMap(s, keyWordCountMap);
            }
        }

        addToStringMap("Misc", keyWordCountMap);

        for(Map.Entry<String, Float> entry : routeSourceCounts.entrySet()){
            String keyWord = "Misc";
            List<String> splitList = splitStrings(entry.getKey(), c);
            for(String s : splitList){
                //order it with the largest keyword
                if(keyWordCountMap.containsKey(s)
                        && ((keyWordCountMap.containsKey(keyWord) && keyWordCountMap.get(s) > keyWordCountMap.get(keyWord))
                            || (splitList.size() == 1 && entry.getValue() > 10))) keyWord = s;
            }

            //on the list it goes
            if(entry.getValue() <= 2f){
                addOrIncrement("Misc", entry.getValue(), routeByKeyWordCountMap);
            } else {
                addOrIncrement(keyWord, entry.getValue(), routeByKeyWordCountMap);
            }
        }

        return routeByKeyWordCountMap;
    }

    private void addOrIncrement(String key, Float value, Map<String, Float> map){
        if(map.containsKey(key)){
            map.put(key, map.get(key) + value);
        } else {
            map.put(key, value);
        }
    }

    private static List<String> splitStrings(String str, char dl) {
        String[] tokens = str.split(Pattern.quote("_"));
        return Arrays.asList(tokens);
    }

    //sort hashmap by values - hashmaps don't have order -> return a LinkedHashMap
    public static Map<String, Float> sortByLargestValue(Map<String, Float> hm)
    {
        // Create a list from elements of HashMap
        List<Map.Entry<String, Float> > list =
                new LinkedList<>(hm.entrySet());

        // Sort the list
        Collections.sort(list, new Comparator<Map.Entry<String, Float> >() {
            public int compare(Map.Entry<String, Float> o1,
                               Map.Entry<String, Float> o2)
            {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });

        // put data from sorted list into a hashmap
        Map<String, Float> temp = new LinkedHashMap<>();
        for (Map.Entry<String, Float> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

    public static Set<RouteManager.RouteData> getAllRoutes(){
        RouteManager routeManager = RouteManager.getInstance();

        Set<RouteManager.RouteData> allRoutes = new HashSet<>(routeManager.getRoutesInLocation(Global.getSector().getHyperspace()));
        for(LocationAPI loc : Global.getSector().getAllLocations()){
            allRoutes.addAll(routeManager.getRoutesInLocation(loc));
        }

        return allRoutes;
    }

    private void addToRouteSourceCount(RouteManager.RouteData data, Map<String, Float> map){
        String key = data.getSource();
        addToStringMap(key, map);
    }

    public static void addToStringMap(String s, Map<String, Float> map){
        if(map.containsKey(s)){
            map.put(s, map.get(s) + 1f);
        } else {
            map.put(s, 1f);
        }
    }

    public static Set<CampaignFleetAPI> getFleets(){
        Set<CampaignFleetAPI> fl = new HashSet<>(Global.getSector().getHyperspace().getFleets());
        for(LocationAPI loc : Global.getSector().getAllLocations()){
            fl.addAll(loc.getFleets());
        }

        return fl;
    }
}
