package com.fs.starfarer.api.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.commands.ids.clearCommands_Ids;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.loading.VariantSource;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.List;

public class clearCommands_clearModularShips implements BaseCommand {

    @Override
    public CommandResult runCommand(String args, CommandContext context) {

        if ( context.isInCampaign() )
        {
//            Vector2f playerFleetLocation = Global.getSector().getPlayerFleet().getLocation();
//            Console.showMessage("Player fleet at "+playerFleetLocation.x+","+playerFleetLocation.y);
            LocationAPI location = Global.getSector().getCurrentLocation();
            List<CampaignFleetAPI> fleets = location.getFleets();
//            Map<Double,CampaignFleetAPI> fleetsAndDist = new TreeMap<Double,CampaignFleetAPI>(Collections.reverseOrder());

            boolean scopeAll = false;
            boolean scopeAllStations = false;

            if (!args.isEmpty()) {
                try
                {
                    if (args.toLowerCase().equals("all")) {
                        scopeAll = true;
                    } else if (args.toLowerCase().equals("allwithstations")) {
                        scopeAllStations = true;
                    }
                    else {
                        throw new Exception();
                    }
                }
                catch (Exception ex)
                {
                    return CommandResult.BAD_SYNTAX;
                }
            }

            for (CampaignFleetAPI fleet : fleets) {
                if (fleet.isPlayerFleet()
                        || fleet.getMemoryWithoutUpdate().getBoolean(MemFlags.ENTITY_MISSION_IMPORTANT)
                        || fleet.getMemoryWithoutUpdate().getBoolean(clearCommands_Ids.NO_CLEAR_KEY)) {
                    continue;
                }

                List<FleetMemberAPI> members = fleet.getFleetData().getMembersListCopy();
                for (FleetMemberAPI member : members) {
                    if (member.getVariant().getModuleSlots() != null && !member.getVariant().getModuleSlots().isEmpty()) {
                        if (scopeAllStations) {
                            fleet.removeFleetMemberWithDestructionFlash(member);
                            Console.showMessage(member.getShipName() + " - " + member.getHullId() + " -  destroyed");
                        } else if (scopeAll) {
                            if ( member.isStation() ) continue;
                            fleet.removeFleetMemberWithDestructionFlash(member);
                            Console.showMessage(member.getShipName() + " - " + member.getHullId() + " - destroyed");
                        } else {
                            if (member.getVariant().getSource().equals(VariantSource.REFIT) && !member.isStation()) {
                                fleet.removeFleetMemberWithDestructionFlash(member);
                                Console.showMessage(member.getShipName() + " - " + member.getHullId() + " - destroyed");
                            }
                        }
                    }
                }
            }
            return CommandResult.SUCCESS;

        } else {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }
    }
}
