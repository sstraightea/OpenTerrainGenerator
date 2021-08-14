package com.pg85.otg.spigot.commands;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import net.minecraft.server.v1_16_R3.BlockPosition;
import org.bukkit.entity.Player;

public class WorldEditUtil
{
	public static RegionCommand.Region getRegionFromPlayer(Player source)
	{
		if (!WorldEdit.getInstance().getSessionManager().contains(BukkitAdapter.adapt(source)))
		{
			return null;
		}
		try
		{
			com.sk89q.worldedit.regions.Region weRegion = WorldEdit.getInstance().getSessionManager()
				.get(BukkitAdapter.adapt(source))
				.getSelection(BukkitAdapter.adapt(source.getWorld()));
			RegionCommand.Region region = new RegionCommand.Region();
			region.setPos(getPosFromVector3(weRegion.getMinimumPoint()));
			region.setPos(getPosFromVector3(weRegion.getMaximumPoint()));
			return region;
		}
		catch (IncompleteRegionException e)
		{
			return null;
		}
	}

	private static BlockPosition getPosFromVector3(BlockVector3 vector)
	{
		return new BlockPosition(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
	}
}