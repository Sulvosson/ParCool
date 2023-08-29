package com.alrex.parcool.common.network;

import com.alrex.parcool.ParCool;
import com.alrex.parcool.common.capability.Parkourability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.function.Supplier;

public class SyncClientInformationMessage {
	private final ByteBuffer data = ByteBuffer.allocate(512);
	private UUID playerID = null;
	private boolean requestLimitations = false;

	public void encode(FriendlyByteBuf packet) {
		packet.writeLong(playerID.getMostSignificantBits());
		packet.writeLong(playerID.getLeastSignificantBits());
		packet.writeBoolean(requestLimitations);
		packet.writeBytes(data);
		data.rewind();
	}

	public static SyncClientInformationMessage decode(FriendlyByteBuf packet) {
		SyncClientInformationMessage message = new SyncClientInformationMessage();
		message.playerID = new UUID(packet.readLong(), packet.readLong());
		message.requestLimitations = packet.readBoolean();
		while (packet.isReadable()) {
			message.data.put(packet.readByte());
		}
		message.data.flip();
		return message;
	}

	@OnlyIn(Dist.CLIENT)
	public void handleClient(Supplier<NetworkEvent.Context> contextSupplier) {
		contextSupplier.get().enqueueWork(() -> {
			Player player;
			if (contextSupplier.get().getDirection().getReceptionSide() == LogicalSide.CLIENT) {
				Level world = Minecraft.getInstance().level;
				if (world == null) return;
				player = world.getPlayerByUUID(playerID);
				if (player == null) return;
			} else {
				ServerPlayer serverPlayer = contextSupplier.get().getSender();
				player = serverPlayer;
				if (player == null) return;
				ParCool.CHANNEL_INSTANCE.send(PacketDistributor.ALL.noArg(), this);
				if (requestLimitations) {
					SyncLimitationMessage.sendServerLimitation(serverPlayer);
					SyncLimitationMessage.sendIndividualLimitation(serverPlayer);
				}
			}
			Parkourability parkourability = Parkourability.get(player);
			if (parkourability == null) return;
			if (!player.isLocalPlayer()) {
				parkourability.getClientInfo().readFrom(data);
				data.rewind();
			}
			parkourability.getClientInfo().setSynced(true);
		});
		contextSupplier.get().setPacketHandled(true);
	}

	public void handleServer(Supplier<NetworkEvent.Context> contextSupplier) {
		contextSupplier.get().enqueueWork(() -> {
			ServerPlayer player = contextSupplier.get().getSender();
			if (player == null) return;
			ParCool.CHANNEL_INSTANCE.send(PacketDistributor.ALL.noArg(), this);

			Parkourability parkourability = Parkourability.get(player);
			if (parkourability == null) return;
			if (requestLimitations) {
				SyncLimitationMessage.sendServerLimitation(player);
				SyncLimitationMessage.sendIndividualLimitation(player);
			}
			parkourability.getClientInfo().readFrom(data);
			data.rewind();
			parkourability.getClientInfo().setSynced(true);
		});
		contextSupplier.get().setPacketHandled(true);
	}

	@OnlyIn(Dist.CLIENT)
	public static void sync(LocalPlayer player, boolean requestSendLimitation) {
		Parkourability parkourability = Parkourability.get(player);
		if (parkourability == null) return;
		parkourability.getClientInfo().readFromLocalConfig();
		SyncClientInformationMessage message = new SyncClientInformationMessage();
		parkourability.getClientInfo().setSynced(false);
		parkourability.getClientInfo().writeTo(message.data);
		message.data.flip();
		message.playerID = player.getUUID();
		message.requestLimitations = requestSendLimitation;

		ParCool.CHANNEL_INSTANCE.send(PacketDistributor.SERVER.noArg(), message);
	}
}