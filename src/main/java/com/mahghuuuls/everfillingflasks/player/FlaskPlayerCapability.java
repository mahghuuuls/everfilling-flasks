package com.mahghuuuls.everfillingflasks.player;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

import javax.annotation.Nullable;
import java.util.concurrent.Callable;

/**
 * Registration and lookup for the player Flask capability. One instance per player, attached by
 * {@link PlayerEvents}; {@link #get(EntityPlayer)} is the only lookup path the rest of the mod
 * uses.
 */
public final class FlaskPlayerCapability {

    @CapabilityInject(FlaskPlayerData.class)
    public static Capability<FlaskPlayerData> CAPABILITY = null;

    private FlaskPlayerCapability() {
    }

    public static void register() {
        CapabilityManager.INSTANCE.register(FlaskPlayerData.class,
                new Capability.IStorage<FlaskPlayerData>() {
                    // Serialization goes through the provider; this storage exists because the
                    // capability system requires one, and must not be a second write path.
                    @Nullable
                    @Override
                    public NBTBase writeNBT(Capability<FlaskPlayerData> capability,
                                            FlaskPlayerData instance, EnumFacing side) {
                        return null;
                    }

                    @Override
                    public void readNBT(Capability<FlaskPlayerData> capability,
                                        FlaskPlayerData instance, EnumFacing side, NBTBase nbt) {
                    }
                },
                new Callable<FlaskPlayerData>() {
                    @Override
                    public FlaskPlayerData call() {
                        return new FlaskPlayerData();
                    }
                });
    }

    /** This player's Flask data, or null for an entity the provider was never attached to. */
    @Nullable
    public static FlaskPlayerData get(EntityPlayer player) {
        return player.getCapability(CAPABILITY, null);
    }
}
