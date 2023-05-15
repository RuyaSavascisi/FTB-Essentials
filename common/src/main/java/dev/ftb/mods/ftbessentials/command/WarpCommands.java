package dev.ftb.mods.ftbessentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftbessentials.util.FTBEPlayerData;
import dev.ftb.mods.ftbessentials.util.FTBEWorldData;
import dev.ftb.mods.ftbessentials.util.TeleportPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Set;

/**
 * @author LatvianModder
 */
public class WarpCommands {
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		if (FTBEConfig.WARP.isEnabled()) {
			dispatcher.register(Commands.literal("warp")
					.requires(FTBEConfig.WARP)
					.then(Commands.argument("name", StringArgumentType.greedyString())
							.suggests((context, builder) -> SharedSuggestionProvider.suggest(getWarpSuggestions(context), builder))
							.executes(context -> warp(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name")))
					)
			);

			dispatcher.register(Commands.literal("setwarp")
					.requires(FTBEConfig.WARP.enabledAndOp())
					.then(Commands.argument("name", StringArgumentType.greedyString())
							.executes(context -> setWarp(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name")))
					)
			);

			dispatcher.register(Commands.literal("delwarp")
					.requires(FTBEConfig.WARP.enabledAndOp())
					.then(Commands.argument("name", StringArgumentType.greedyString())
							.suggests((context, builder) -> SharedSuggestionProvider.suggest(getWarpSuggestions(context), builder))
							.executes(context -> deleteWarp(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name")))
					)
			);

			dispatcher.register(Commands.literal("listwarps")
					.requires(FTBEConfig.WARP)
					.executes(context -> listWarps(context.getSource()))
			);
		}
	}

	public static Set<String> getWarpSuggestions(CommandContext<CommandSourceStack> context) {
		return FTBEWorldData.instance.warps.keySet();
	}

	public static int warp(ServerPlayer player, String name) {
		FTBEPlayerData data = FTBEPlayerData.get(player);
		TeleportPos pos = FTBEWorldData.instance.warps.get(name.toLowerCase());

		if (pos == null) {
			player.displayClientMessage(Component.literal("Warp not found!"), false);
			return 0;
		}

		return data.warpTeleporter.teleport(player, p -> pos).runCommand(player);
	}

	public static int setWarp(ServerPlayer player, String name) {
		FTBEWorldData.instance.warps.put(name.toLowerCase(), new TeleportPos(player));
		FTBEWorldData.instance.markDirty();
		player.displayClientMessage(Component.literal("Warp set!"), false);
		return 1;
	}

	public static int deleteWarp(ServerPlayer player, String name) {
		if (FTBEWorldData.instance.warps.remove(name.toLowerCase()) != null) {
			FTBEWorldData.instance.markDirty();
			player.displayClientMessage(Component.literal("Warp deleted!"), false);
			return 1;
		} else {
			player.displayClientMessage(Component.literal("Warp not found!"), false);
			return 0;
		}
	}

	public static int listWarps(CommandSourceStack source) {
		if (FTBEWorldData.instance.warps.isEmpty()) {
			source.sendSuccess(Component.literal("None"), false);
			return 1;
		}

		TeleportPos origin = new TeleportPos(source.getLevel().dimension(), BlockPos.containing(source.getPosition()));

		for (Map.Entry<String, TeleportPos> entry : FTBEWorldData.instance.warps.entrySet()) {
			source.sendSuccess(Component.literal(entry.getKey() + ": " + entry.getValue().distanceString(origin)), false);
		}

		return 1;
	}
}
