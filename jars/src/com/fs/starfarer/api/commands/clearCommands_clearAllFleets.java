package com.fs.starfarer.api.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.LocationAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import static com.fs.starfarer.api.commands.clearCommands_clearFleets.softClear;

public class clearCommands_clearAllFleets implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }
        int count = 0;

        //clear hyperspace
        count += softClear(Global.getSector().getHyperspace(), null, null);

        //clear everything else
        for(LocationAPI loc : Global.getSector().getAllLocations()){
            count += softClear(loc,null, null);
        }

        Console.showMessage("Cleared " + count + " Fleets");
        return CommandResult.SUCCESS;
    }
}
