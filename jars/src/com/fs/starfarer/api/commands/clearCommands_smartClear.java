package com.fs.starfarer.api.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.util.Pair;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.fs.starfarer.api.commands.clearCommands_fleetReport.*;

public class clearCommands_smartClear implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        boolean devmode = Global.getSettings().isDevMode();

        //fleets
        Set<CampaignFleetAPI> fleetList = getFleets();
        Map<String, Float> factionFleetCount = new HashMap<>();

        for (CampaignFleetAPI fleet : fleetList){
            addToStringMap((fleet.getFaction().getId()), factionFleetCount);
        }

        factionFleetCount = sortByLargestValue(factionFleetCount);
        com.fs.starfarer.api.util.Pair<Float, Float> bounds = new Pair<>((factionFleetCount.size()-1)*0.25f, (factionFleetCount.size()-1)*0.5f);
        if(devmode) Console.showMessage("Size " + factionFleetCount.size());
        if(devmode) Console.showMessage("Bounds " + bounds.one + " - " + bounds.two);

        int i = 0;
        float count = 0;
        float limit = 0;
        for(Map.Entry<String, Float> entry : factionFleetCount.entrySet()){
            if(i >= bounds.one && i <= bounds.two){
                limit += entry.getValue();
                count++;
            }

            i++;
        }

        if(devmode) Console.showMessage("Limit " +limit + " vs count " + count);
        limit /= count;
        limit *= 2f;

        if(devmode) Console.showMessage("Final Limit " + limit);

        boolean actuate = false;
        for(Map.Entry<String, Float> entry : factionFleetCount.entrySet()){
            if(entry.getValue() > limit || entry.getValue() > 100f) {
                if (Factions.THREAT.equals(entry.getKey()) || Factions.DWELLER.equals(entry.getKey()) || entry.getKey().contains("rat_abyssals")){
                    Console.showMessage("Skipping Fleets of Faction " + entry.getKey());
                }
                clearFleetsOfFaction(entry.getKey());
                actuate = true;
            }
        }

        String msg = actuate ? "Completed" : "Nothing to clear, faction fleet levels nominal";
        Console.showMessage(msg);
        return CommandResult.SUCCESS;
    }

    private void clearFleetsOfFaction(String id){
        int count = 0;
        FactionAPI fact = Global.getSector().getFaction(id);
        if(fact == null) return;

        Set<CampaignFleetAPI> excludingSet = getFleetsForSource("econ");
        excludingSet.addAll(getFleetsForSource("nex_specialForces"));
        excludingSet.addAll(getFleetsForSource("IndEvo_Shipping"));
        excludingSet.addAll(getFleetsWithAssignment("Orbiting"));
        excludingSet.addAll(getFleetsWithAssignment("Defending"));

        //clear hyperspace
        count += clearCommands_clearFleets.softClear(Global.getSector().getHyperspace(), fact, excludingSet);

        //clear everything else
        for(LocationAPI loc : Global.getSector().getAllLocations()){
            count += clearCommands_clearFleets.softClear(loc, fact, excludingSet);
        }

        Console.showMessage("Cleared " + count + " Fleets of " + fact.getDisplayName());
    }

    private Set<CampaignFleetAPI> getFleetsForSource(String source){
        Set<CampaignFleetAPI> ss = new HashSet<>();
        for(RouteManager.RouteData r : RouteManager.getInstance().getRoutesForSource(source)){
            if(r.getActiveFleet() != null) ss.add(r.getActiveFleet());
        }

        return ss;
    }

    private Set<CampaignFleetAPI> getFleetsWithAssignment(String name){
        Set<CampaignFleetAPI> fleetList = getFleets();
        Set<CampaignFleetAPI> asFleetList = new HashSet<>();
        for(CampaignFleetAPI f : fleetList){
            if(f.getCurrentAssignment() != null && f.getCurrentAssignment().getAssignment().getDescription().equals(name)) asFleetList.add(f);
        }

        return asFleetList;
    }
}
