package com.fs.starfarer.api.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.commands.ids.clearCommands_Ids;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.campaign.Faction;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class clearCommands_clearFleets implements BaseCommand
{

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        if(args == null || args.isEmpty()){
            Console.showMessage("Specify a faction to clear the fleets of");
            return CommandResult.BAD_SYNTAX;
        }

        FactionAPI fact = null;

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

        int count = 0;

        //clear hyperspace
        count += softClear(Global.getSector().getHyperspace(), fact, null);

        //clear everything else
        for(LocationAPI loc : Global.getSector().getAllLocations()){
            count += softClear(loc, fact, null);
        }

        Console.showMessage("Cleared " + count + " Fleets of " + fact.getDisplayName());
        return CommandResult.SUCCESS;
    }

    public static int softClear(LocationAPI loc, FactionAPI fact, Set<CampaignFleetAPI> exclude){
        int count = 0;

        //clear everything else
        List<CampaignFleetAPI> campaignFleetAPIS = new ArrayList<>(loc.getFleets());

        //clear fleets
        for (CampaignFleetAPI fleet : campaignFleetAPIS){
            //check for important
            if(fleet.isPlayerFleet()
                    || fleet.isStationMode()
                    || (fact != null && fleet.getFaction() != fact)
                    || (exclude != null && exclude.contains(fleet))
                    || fleet.getMemoryWithoutUpdate().getBoolean(MemFlags.ENTITY_MISSION_IMPORTANT)
                    || fleet.getMemoryWithoutUpdate().getBoolean(MemFlags.FLEET_IGNORES_OTHER_FLEETS)
                    || fleet.getMemoryWithoutUpdate().getBoolean(MemFlags.FLEET_DO_NOT_IGNORE_PLAYER)
                    || fleet.getMemoryWithoutUpdate().getBoolean(clearCommands_Ids.NO_CLEAR_KEY)
                    || fleet.getMemoryWithoutUpdate().getBoolean("$ziggurat")
            ) continue;

            //check for important ships
            boolean hasUnique = false;
            for (FleetMemberAPI m : fleet.getMembersWithFightersCopy()){
                ShipVariantAPI variant = m.getVariant();

                if (variant == null) continue;
                if (variant.hasTag(Tags.SHIP_UNIQUE_SIGNATURE)
                        || variant.hasTag(Tags.SHIP_CAN_NOT_SCUTTLE)
                        || variant.hasTag(Tags.TAG_NO_AUTOFIT_UNLESS_PLAYER)) {

                    hasUnique = true;
                    break;
                }
            }

            if (hasUnique) continue;

            Global.getSector().reportFleetDespawned(fleet, CampaignEventListener.FleetDespawnReason.OTHER, null);
            loc.removeEntity(fleet);
            count++;
        }

        return count;
    }
}
