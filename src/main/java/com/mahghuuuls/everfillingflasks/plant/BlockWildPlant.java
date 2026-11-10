package com.mahghuuuls.everfillingflasks.plant;

import com.mahghuuuls.everfillingflasks.Tags;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * A wild plant in the world: one block instance per {@link WildPlant}. It is found, never
 * placed: no item block, no pick block, no bone meal, no drops through the normal harvest path.
 * Every pick and every stage change goes through {@link PlantLifecycle}.
 */
public final class BlockWildPlant extends Block {

    public static final PropertyEnum<PlantStage> STAGE = PropertyEnum.create("stage", PlantStage.class);

    /** The click and outline box of a stem: the short stub and its leaves. */
    private static final AxisAlignedBB STEM_AABB = new AxisAlignedBB(0.25D, 0.0D, 0.25D, 0.75D, 0.35D, 0.75D);

    /** About one and a half seconds to break by hand. */
    private static final float STEM_HARDNESS = 1.0F;

    private final WildPlant plant;

    /** The click and outline box of a bloom: the widest model's reach, as tall as this plant's model. */
    private final AxisAlignedBB bloomBox;

    public BlockWildPlant(WildPlant plant) {
        super(Material.PLANTS);
        this.plant = plant;
        this.bloomBox = new AxisAlignedBB(0.15D, 0.0D, 0.15D, 0.85D, plant.bloomTop(), 0.85D);
        setRegistryName(Tags.MOD_ID, plant.key());
        setTranslationKey(Tags.MOD_ID + "." + plant.key());
        setTickRandomly(true);
        setHardness(0.0F);
        setSoundType(SoundType.PLANT);
        setDefaultState(blockState.getBaseState().withProperty(STAGE, PlantStage.BLOOM));
    }

    public WildPlant plant() {
        return plant;
    }

    /** Whether a plant of this kind may stand at this position: on its ground. */
    public boolean canGrowAt(World world, BlockPos pos) {
        return plant.ground().accepts(world.getBlockState(pos.down()));
    }

    // Stage property and metadata.

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, STAGE);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(STAGE, PlantStage.fromMeta(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(STAGE).ordinal();
    }

    // Tile entity only while a stem.

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return state.getValue(STAGE) == PlantStage.STEM;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityStem();
    }

    // Ticks and picking.

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (world.isRemote) {
            return;
        }
        if (!canGrowAt(world, pos)) {
            world.setBlockToAir(pos);
            return;
        }
        PlantLifecycle.tick(world, pos, state);
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        return PlantLifecycle.lightFor(plant, state.getValue(STAGE));
    }

    /** Now and then a small purple sparkle above a sparkling plant's bloom, drawn on the client only. */
    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random random) {
        if (!plant.sparkles() || state.getValue(STAGE) != PlantStage.BLOOM || random.nextInt(3) != 0) {
            return;
        }
        double x = pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.25D;
        double y = pos.getY() + 0.85D + random.nextDouble() * 0.15D;
        double z = pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.25D;
        world.spawnParticle(EnumParticleTypes.SPELL_WITCH, x, y, z, 0.0D, 0.0D, 0.0D);
    }

    /**
     * A bloom breaks at once; a stem takes a held break. Without the split, a click
     * held for a few ticks would pick the bloom and then remove the stem the client still saw as
     * an instant-break block.
     */
    @Override
    public float getBlockHardness(IBlockState state, World world, BlockPos pos) {
        return state.getValue(STAGE) == PlantStage.STEM ? STEM_HARDNESS : 0.0F;
    }

    /**
     * The pick path. A bloom becomes a stem and stays: returning false keeps the block in place
     * and skips the vanilla harvest, so the lifecycle owns the drop. A stem is removed for good
     * and drops nothing, because {@link #getDrops} adds nothing.
     */
    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
        boolean drop = !player.capabilities.isCreativeMode;
        if (PlantLifecycle.onBroken(world, pos, state, plant, drop)) {
            onBlockHarvested(world, pos, state, player);
            return world.setBlockState(pos, Blocks.AIR.getDefaultState(), world.isRemote ? 11 : 3);
        }
        return false;
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        // The only drop is the pick's, made by the lifecycle; every other removal gives nothing.
    }

    @Override
    public void onBlockExploded(World world, BlockPos pos, Explosion explosion) {
        world.setBlockToAir(pos);
        onExplosionDestroy(world, pos, explosion);
    }

    @Override
    protected boolean canSilkHarvest() {
        return false;
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return ItemStack.EMPTY;
    }

    // Support.

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return super.canPlaceBlockAt(world, pos) && canGrowAt(world, pos);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        if (!world.isRemote && !canGrowAt(world, pos)) {
            world.setBlockToAir(pos);
        }
    }

    // Shape and rendering.

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return state.getValue(STAGE) == PlantStage.STEM ? STEM_AABB : bloomBox;
    }

    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return NULL_AABB;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }
}
