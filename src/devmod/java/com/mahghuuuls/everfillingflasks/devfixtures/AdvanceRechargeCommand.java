package com.mahghuuuls.everfillingflasks.devfixtures;

import com.mahghuuuls.everfillingflasks.api.FlaskApi;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

/**
 * {@code /eff_advance <ticks>}: the fixture's stand-in for an add-on event that refills a Flask
 * faster (1.1.0, REQ-048). Calls the public API for the sender and reports what it returned,
 * so a card can read the applied count in chat and the diagnostics line in the log.
 */
final class AdvanceRechargeCommand extends CommandBase {

    @Override
    public String getName() {
        return "eff_advance";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/eff_advance <ticks>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args)
            throws CommandException {
        if (args.length != 1) {
            throw new WrongUsageException(getUsage(sender));
        }
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        int ticks = parseInt(args[0]);
        int applied = FlaskApi.advanceRecharge(player, ticks);
        String line = "eff_advance " + ticks + ": applied " + applied;
        DevFixturesMod.LOGGER.info("{}: {}", player.getName(), line);
        sender.sendMessage(new TextComponentString(line));
    }
}
