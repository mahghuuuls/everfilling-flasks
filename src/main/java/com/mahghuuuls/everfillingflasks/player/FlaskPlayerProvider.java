package com.mahghuuuls.everfillingflasks.player;

import com.mahghuuuls.everfillingflasks.EverfillingFlasksMod;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import javax.annotation.Nullable;

/**
 * Attaches one {@link FlaskPlayerData} to one player and carries it through save and load.
 *
 * <p>Implements {@code ICapabilitySerializable<NBTBase>} rather than the narrower
 * {@code <NBTTagCompound>}: with the narrow type the compiler's bridge method casts before this
 * code runs, and a wrong stored tag type would throw outside the guard below, stopping the
 * player from logging in. The wide type puts the check inside the guard.
 */
public final class FlaskPlayerProvider implements ICapabilitySerializable<NBTBase> {

    private final FlaskPlayerData data = new FlaskPlayerData();

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == FlaskPlayerCapability.CAPABILITY;
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == FlaskPlayerCapability.CAPABILITY) {
            return FlaskPlayerCapability.CAPABILITY.cast(data);
        }
        return null;
    }

    @Override
    public NBTBase serializeNBT() {
        return data.serializeNBT();
    }

    @Override
    public void deserializeNBT(NBTBase nbt) {
        if (nbt instanceof NBTTagCompound) {
            data.deserializeNBT((NBTTagCompound) nbt);
        } else {
            EverfillingFlasksMod.LOGGER.warn(
                    "Stored flask data had unexpected NBT type {}; starting this player fresh",
                    nbt == null ? "null" : nbt.getClass().getSimpleName());
        }
    }
}
