package com.mahghuuuls.everfillingflasks.plant;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

/**
 * The one piece of data a stem carries: the total world time at which it blooms again. It exists
 * only while the block is a stem; the block creates it on the change into a stem, and the world
 * removes it on the change back to a bloom. Not tickable: the block's random ticks read it.
 */
public final class TileEntityStem extends TileEntity {

    /** NBT key, a saved-world contract. */
    static final String KEY_REGROW_AT = "regrowAt";

    private long regrowAt;

    /** The total world time at which the stem blooms again. */
    public long regrowAt() {
        return regrowAt;
    }

    public void setRegrowAt(long regrowAt) {
        this.regrowAt = regrowAt;
        markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        regrowAt = compound.getLong(KEY_REGROW_AT);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setLong(KEY_REGROW_AT, regrowAt);
        return compound;
    }
}
