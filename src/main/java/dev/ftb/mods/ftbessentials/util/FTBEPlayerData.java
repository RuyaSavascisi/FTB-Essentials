package dev.ftb.mods.ftbessentials.util;

import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftbessentials.FTBEConfig;
import dev.ftb.mods.ftbessentials.net.UpdateTabNamePacket;
import dev.ftb.mods.ftblibrary.snbt.OrderedCompoundTag;
import dev.ftb.mods.ftblibrary.snbt.SNBT;
import me.shedaniel.architectury.utils.NbtType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public class FTBEPlayerData {
	public static final Map<UUID, FTBEPlayerData> MAP = new HashMap<>();

	public static FTBEPlayerData get(GameProfile profile) {
		FTBEPlayerData data = MAP.get(profile.getId());

		if (data == null) {
			data = new FTBEPlayerData(profile.getId());

			if (profile.getName() != null && !profile.getName().isEmpty()) {
				data.name = profile.getName();
			}

			MAP.put(profile.getId(), data);
		}

		return data;
	}

	public static FTBEPlayerData get(Player player) {
		return get(player.getGameProfile());
	}

	public static void addTeleportHistory(ServerPlayer player, ResourceKey<Level> dimension, BlockPos pos) {
		get(player).addTeleportHistory(player, new TeleportPos(dimension, pos));
	}

	public static void addTeleportHistory(ServerPlayer player) {
		addTeleportHistory(player, player.level.dimension(), player.blockPosition());
	}

	public final UUID uuid;
	public String name;
	private boolean save;

	public boolean muted;
	public boolean fly;
	public boolean god;
	public String nick;
	public TeleportPos lastSeen;
	public final LinkedHashMap<String, TeleportPos> homes;
	public int recording;

	public final CooldownTeleporter backTeleporter;
	public final CooldownTeleporter spawnTeleporter;
	public final CooldownTeleporter warpTeleporter;
	public final CooldownTeleporter homeTeleporter;
	public final CooldownTeleporter tpaTeleporter;
	public final CooldownTeleporter rtpTeleporter;
	public final LinkedList<TeleportPos> teleportHistory;

	public FTBEPlayerData(UUID u) {
		uuid = u;
		name = "Unknown";
		save = false;

		muted = false;
		fly = false;
		god = false;
		nick = "";
		lastSeen = new TeleportPos(Level.OVERWORLD, BlockPos.ZERO);
		homes = new LinkedHashMap<>();
		recording = 0;

		backTeleporter = new CooldownTeleporter(this, FTBEConfig::getBackCooldown);
		spawnTeleporter = new CooldownTeleporter(this, FTBEConfig::getSpawnCooldown);
		warpTeleporter = new CooldownTeleporter(this, FTBEConfig::getWarpCooldown);
		homeTeleporter = new CooldownTeleporter(this, FTBEConfig::getHomeCooldown);
		tpaTeleporter = new CooldownTeleporter(this, FTBEConfig::getTpaCooldown);
		rtpTeleporter = new CooldownTeleporter(this, FTBEConfig::getRtpCooldown);
		teleportHistory = new LinkedList<>();
	}

	public void save() {
		save = true;
	}

	public CompoundTag write() {
		CompoundTag json = new OrderedCompoundTag();
		json.putBoolean("muted", muted);
		json.putBoolean("fly", fly);
		json.putBoolean("god", god);
		json.putString("nick", nick);
		json.put("last_seen", lastSeen.write());
		json.putInt("recording", recording);

		ListTag tph = new ListTag();

		for (TeleportPos pos : teleportHistory) {
			tph.add(pos.write());
		}

		json.put("teleport_history", tph);

		CompoundTag hm = new CompoundTag();

		for (Map.Entry<String, TeleportPos> h : homes.entrySet()) {
			hm.put(h.getKey(), h.getValue().write());
		}

		json.put("homes", hm);

		return json;
	}

	public void read(CompoundTag tag) {
		muted = tag.getBoolean("muted");
		fly = tag.getBoolean("fly");
		god = tag.getBoolean("god");
		nick = tag.getString("nick");
		recording = tag.getInt("recording");
		lastSeen = tag.contains("last_seen") ? new TeleportPos(tag.getCompound("last_seen")) : null;

		teleportHistory.clear();

		ListTag th = tag.getList("teleport_history", NbtType.COMPOUND);

		for (int i = 0; i < th.size(); i++) {
			teleportHistory.add(new TeleportPos(th.getCompound(i)));
		}

		homes.clear();

		CompoundTag h = tag.getCompound("homes");

		for (String key : h.getAllKeys()) {
			homes.put(key, new TeleportPos(h.getCompound(key)));
		}
	}

	public void addTeleportHistory(ServerPlayer player, TeleportPos pos) {
		teleportHistory.add(pos);

		while (teleportHistory.size() > FTBEConfig.getMaxBack(player)) {
			teleportHistory.removeFirst();
		}

		save();
	}

	public void load() {
		CompoundTag tag = SNBT.read(FTBEWorldData.instance.mkdirs("playerdata").resolve(uuid + ".snbt"));

		if (tag != null) {
			read(tag);
		}
	}

	public void saveNow() {
		if (save && SNBT.write(FTBEWorldData.instance.mkdirs("playerdata").resolve(uuid + ".snbt"), write())) {
			save = false;
		}
	}

	public void sendTabName(MinecraftServer server) {
		new UpdateTabNamePacket(uuid, name, nick, recording, false).sendToAll(server);
	}

	public void sendTabName(ServerPlayer to) {
		new UpdateTabNamePacket(uuid, name, nick, recording, false).sendTo(to);
	}
}