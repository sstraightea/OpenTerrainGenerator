package com.pg85.otg.forge.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.pg85.otg.customobject.util.Corner;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public class RegionCommand extends BaseCommand
{
	protected static HashMap<Entity, Region> playerSelectionMap = new HashMap<>();

	public RegionCommand()
	{
		this.name = "region";
		this.helpMessage = "Allows for setting and modifying regions";
		this.usage = "Please see /otg help region.";
	}

	@Override
	public void build(LiteralArgumentBuilder<CommandSource> builder)
	{
		builder.then(
			Commands.literal("region").then(
				Commands.literal("mark").executes(
					context -> mark(context.getSource())
				).then(Commands.argument("point",new RegionMarkerArgument()).executes(
					context -> mark(context.getSource(), context.getArgument("point", String.class))
				))
			).then(
				Commands.literal("clear").executes(
					context ->clear(context.getSource())
				)
			).then(
				Commands.literal("expand").then(
					Commands.argument("direction", new DirectionArgument(true)).then(
						Commands.argument("value", IntegerArgumentType.integer()).executes(
							context -> expand(context.getSource(),
								context.getArgument("direction", String.class),
								context.getArgument("value", Integer.class))
						)
					)
				)
			).then(
				Commands.literal("shrink").then(
					Commands.argument("direction", new DirectionArgument(true)).then(
						Commands.argument("value", IntegerArgumentType.integer()).executes(
							context -> shrink(context.getSource(),
								context.getArgument("direction", String.class),
								context.getArgument("value", Integer.class))
						)
					)
				)
			)
		);
	}

	public int mark(CommandSource source)
	{
		if (checkForNonPlayer(source)) return 0;
		if (playerSelectionMap.get(source.getEntity()).setPos(source.getEntity().blockPosition()))
		{
			source.sendSuccess(new StringTextComponent("Point 1 marked"), false);
		} else {
			source.sendSuccess(new StringTextComponent("Point 2 marked"), false);
		}
		return 0;
	}

	public int mark(CommandSource source, String input)
	{
		if (checkForNonPlayer(source)) return 0;
		Region r = playerSelectionMap.get(source.getEntity());
		switch(input)
		{
			case "min":
			case "pos1":
			case "1":
			{
				r.setPos1(source.getEntity().blockPosition());
				source.sendSuccess(new StringTextComponent("Point 1 marked"), false);
				return 0;
			}
			case "max":
			case "pos2":
			case "2":
			{
				r.setPos2(source.getEntity().blockPosition());
				source.sendSuccess(new StringTextComponent("Point 2 marked"), false);
				return 0;
			}
			case "center":
			{
				r.setCenter(Region.cornerFromBlockPos(source.getEntity().blockPosition()));
				source.sendSuccess(new StringTextComponent("Center marked"), false);
				return 0;
			}
			default:
			{
				source.sendSuccess(new StringTextComponent(input + " is not recognized"), false);
				return 0;
			}
		}
	}

	public int clear(CommandSource source)
	{
		if (checkForNonPlayer(source)) return 0;
		playerSelectionMap.get(source.getEntity()).clear();
		source.sendSuccess(new StringTextComponent("Position cleared"), false);
		return 0;
	}

	public int expand(CommandSource source, String direction, Integer value)
	{
		if (checkForNonPlayer(source)) return 0;
		Region region = playerSelectionMap.get(source.getEntity());
		if (region.getMax() == null)
		{
			source.sendSuccess(new StringTextComponent("Please mark two positions before modifying or exporting the region"), false);
			return 0;
		}

		switch (direction)
		{
			case "south": // positive Z
				if (region.pos2.getZ() >= region.pos1.getZ())
					region.setPos2(region.pos2.south(value));
				else
					region.setPos1(region.pos1.south(value));
				break;
			case "north": // negative Z
				if (region.pos2.getZ() < region.pos1.getZ())
					region.setPos2(region.pos2.north(value));
				else
					region.setPos1(region.pos1.north(value));
				break;
			case "east": // positive X
				if (region.pos2.getX() >= region.pos1.getX())
					region.setPos2(region.pos2.east(value));
				else
					region.setPos1(region.pos1.east(value));
				break;
			case "west": // negative X
				if (region.pos2.getX() < region.pos1.getX())
					region.setPos2(region.pos2.west(value));
				else
					region.setPos1(region.pos1.west(value));
				break;
			case "up": // positive y
				if (region.pos2.getY() >= region.pos1.getY())
					region.setPos2(region.pos2.above(value));
				else
					region.setPos1(region.pos1.above(value));
				break;
			case "down": // negative y
				if (region.pos2.getY() < region.pos1.getY())
					region.setPos2(region.pos2.below(value));
				else
					region.setPos1(region.pos1.below(value));
				break;
			default:
				source.sendSuccess(new StringTextComponent("Unrecognized direction " + direction), false);
				return 0;
		}

		source.sendSuccess(new StringTextComponent("Region modified"), false);
		return 0;
	}

	public int shrink(CommandSource source, String direction, Integer value)
	{
		if (checkForNonPlayer(source)) return 0;

		expand(source, direction, -value);

		return 0;
	}

	private static boolean checkForNonPlayer(CommandSource source)
	{
		if (!(source.getEntity() instanceof ServerPlayerEntity))
		{
			source.sendSuccess(new StringTextComponent("Only players can execute this command"), false);
			return true;
		}
		if (!playerSelectionMap.containsKey(source.getEntity()))
		{
			playerSelectionMap.put(source.getEntity(), new Region());
		}
		return false;
	}
	public static class Region
	{
		private BlockPos pos1 = null;
		private BlockPos pos2 = null;
		private Corner center = null;
		private boolean flip = true;

		// Returns whether it set pos1 or not
		public boolean setPos(BlockPos blockPos)
		{
			// alternate between setting min and max
			// Flip initializes as true, meaning we set min first
			if (flip)
				pos1 = blockPos;
			else
				pos2 = blockPos;
			flip = !flip;
			return !flip;
		}

		public static Corner cornerFromBlockPos(BlockPos blockPos)
		{
			return new Corner(blockPos.getX(), blockPos.getY(), blockPos.getZ());
		}

		public void clear()
		{
			pos1 = null;
			pos2 = null;
			center = null;
		}

		public Corner getMin()
		{
			if (pos1 == null || pos2 == null) {
				return null;
			}
			return new Corner(
				Math.min(pos1.getX(), pos2.getX()),
				Math.min(pos1.getY(), pos2.getY()),
				Math.min(pos1.getZ(), pos2.getZ())
			);
		}

		public Corner getMax()
		{
			if (pos1 == null || pos2 == null) {
				return null;
			}
			return new Corner(
				Math.max(pos1.getX(), pos2.getX()),
				Math.max(pos1.getY(), pos2.getY()),
				Math.max(pos1.getZ(), pos2.getZ())
			);
		}

		public void setPos1(BlockPos pos)
		{
			flip = false;
			this.pos1 = pos;
		}

		public void setPos2(BlockPos pos)
		{
			flip = true;
			this.pos2 = pos;
		}

		public Corner getCenter()
		{
			return center;
		}

		public void setCenter(Corner center)
		{
			this.center = center;
		}
	}
	private static class DirectionArgument implements ArgumentType<String>
	{
		private final String[] options;

		public DirectionArgument(boolean vertical)
		{
			if (vertical) options = new String[]{"north", "south", "east", "west", "up", "down"};
			else options = new String[]{"north", "south", "east", "west"};
		}

		@Override
		public String parse(StringReader reader) throws CommandSyntaxException
		{
			return reader.readString();
		}

		@Override
		public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder)
		{
			return ISuggestionProvider.suggest(options, builder);
		}
	}

	private static class RegionMarkerArgument implements ArgumentType<String>
	{
		private final String[] options = new String[] {"1", "2", "center"};

		@Override
		public String parse(StringReader reader) throws CommandSyntaxException
		{
			return reader.readString();
		}

		@Override
		public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder)
		{
			return ISuggestionProvider.suggest(options, builder);
		}
	}
}