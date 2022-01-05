package thut.concrete.block;

import java.util.Random;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thut.api.block.flowing.IFlowingBlock;
import thut.concrete.Concrete;

public class RebarBlock extends CrossCollisionBlock implements SimpleWaterloggedBlock, IFlowingBlock
{
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 16);

    protected int tickRateFall = 150;
    protected int tickRateFlow = 10;
    public float hardenRate = 0.025f;

    Supplier<Block> wetConcrete;
    Supplier<Block> dry_layer;
    Supplier<Block> dry_full;

    protected DyeColor colour;

    public RebarBlock(Properties properties)
    {
        super(2.0F, 2.0F, 16.0f, 16.0f, 16.0f, properties);
        initStateDefinition();
        this.tickRateFall = 1;
        this.tickRateFlow = 1;
        this.colour = DyeColor.LIGHT_GRAY;
    }

    protected void initStateDefinition()
    {
        registerDefaultState(getStateDefinition().any().setValue(IFlowingBlock.WATERLOGGED, false).setValue(LEVEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(NORTH, EAST, WEST, SOUTH, IFlowingBlock.WATERLOGGED, LEVEL);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state)
    {
        return getAmount(state) > 0;
    }

    @Override
    @Deprecated
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos)
    {
        int amt = getAmount(state);
        if (amt > 0) return IFlowingBlock.SHAPES[amt - 1];
        return super.getOcclusionShape(state, level, pos);
    }

    @Override
    public VoxelShape getVisualShape(BlockState p_53311_, BlockGetter p_53312_, BlockPos p_53313_,
            CollisionContext p_53314_)
    {
        return this.getShape(p_53311_, p_53312_, p_53313_, p_53314_);
    }

    public boolean connectsTo(BlockState p_53330_, boolean p_53331_, Direction p_53332_)
    {
        boolean flag = this.isSameFence(p_53330_);
        return flag;
    }

    private boolean isSameFence(BlockState state)
    {
        return state.getBlock() instanceof RebarBlock || state.getBlock() instanceof ReinforcedConcreteBlock.FullDry;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_53304_)
    {
        BlockGetter blockgetter = p_53304_.getLevel();
        BlockPos blockpos = p_53304_.getClickedPos();
        FluidState fluidstate = p_53304_.getLevel().getFluidState(p_53304_.getClickedPos());
        BlockPos blockpos1 = blockpos.north();
        BlockPos blockpos2 = blockpos.east();
        BlockPos blockpos3 = blockpos.south();
        BlockPos blockpos4 = blockpos.west();
        BlockState blockstate = blockgetter.getBlockState(blockpos1);
        BlockState blockstate1 = blockgetter.getBlockState(blockpos2);
        BlockState blockstate2 = blockgetter.getBlockState(blockpos3);
        BlockState blockstate3 = blockgetter.getBlockState(blockpos4);
        return super.getStateForPlacement(p_53304_)
                .setValue(NORTH, Boolean.valueOf(this.connectsTo(blockstate, true, Direction.SOUTH)))
                .setValue(EAST, Boolean.valueOf(this.connectsTo(blockstate1, true, Direction.WEST)))
                .setValue(SOUTH, Boolean.valueOf(this.connectsTo(blockstate2, true, Direction.NORTH)))
                .setValue(WEST, Boolean.valueOf(this.connectsTo(blockstate3, true, Direction.EAST)))
                .setValue(IFlowingBlock.WATERLOGGED, Boolean.valueOf(fluidstate.getType() == Fluids.WATER));
    }

    @Deprecated
    @Override
    public BlockState updateShape(BlockState p_53323_, Direction p_53324_, BlockState p_53325_, LevelAccessor p_53326_,
            BlockPos p_53327_, BlockPos p_53328_)
    {
        return p_53324_.getAxis().getPlane() == Direction.Plane.HORIZONTAL
                ? p_53323_.setValue(PROPERTY_BY_DIRECTION.get(p_53324_),
                        Boolean.valueOf(this.connectsTo(p_53325_, true, p_53324_.getOpposite())))
                : super.updateShape(p_53323_, p_53324_, p_53325_, p_53326_, p_53327_, p_53328_);
    }

    @Override
    public Block getAlternate()
    {
        return this;
    }

    @Override
    public int getFlowRate()
    {
        return tickRateFlow;
    }

    @Override
    public int getFallRate()
    {
        return tickRateFall;
    }

    @Override
    public FluidState getFluidState(BlockState state)
    {
        if (isFalling(state)) return Fluids.EMPTY.defaultFluidState();
        if (state.hasProperty(IFlowingBlock.WATERLOGGED) && state.getValue(IFlowingBlock.WATERLOGGED))
            return Fluids.WATER.getSource(false);
        if (!flows(state)) return Fluids.EMPTY.defaultFluidState();
        int amt = this.getAmount(state);
        if (amt == 0) return super.getFluidState(state);
        if (amt < 2) amt = 2;
        if (amt > 16) amt = 16;
        return Concrete.CONCRETE_FLUID_FLOWING.get().defaultFluidState().setValue(FlowingFluid.LEVEL, amt / 2);
    }

    @Override
    public boolean flows(BlockState state)
    {
        if (state.hasProperty(LEVEL)) return state.getValue(LEVEL) > 0;
        return IFlowingBlock.super.flows(state);
    }

    @Override
    public int getAmount(BlockState state)
    {
        if (state.hasProperty(LEVEL)) return state.getValue(LEVEL);
        return IFlowingBlock.super.getAmount(state);
    }

    @Override
    public boolean isFalling(BlockState state)
    {
        if (this.getAmount(state) <= 0) return false;
        return IFlowingBlock.super.isFalling(state);
    }

    @Override
    public BlockState setAmount(BlockState state, int amt)
    {
        if (state.hasProperty(LEVEL)) return state.setValue(LEVEL, amt);
        return IFlowingBlock.super.setAmount(state, amt);
    }

    @Override
    public BlockState getMergeResult(BlockState mergeFrom, BlockState mergeInto, BlockPos posTo, ServerLevel level)
    {
        if (!(mergeInto.getBlock() instanceof IFlowingBlock))
        {
            mergeInto = Concrete.WET_LAYER.get().defaultBlockState();
            mergeInto = IFlowingBlock.copyValidTo(mergeFrom, mergeInto);
            mergeInto = this.setAmount(mergeInto, this.getExistingAmount(mergeFrom, posTo, level));
        }
        BlockState ret = IFlowingBlock.super.getMergeResult(mergeFrom, mergeInto, posTo, level);
        if (ret.getBlock() instanceof RebarBlock)
        {
            ret = IFlowingBlock.copyValidTo(mergeFrom, ret);
            ret = this.setAmount(ret, this.getExistingAmount(ret, posTo, level));
        }
        return ret;
    }

    @Override
    public BlockState empty(BlockState state)
    {
        if (!(state.getBlock() instanceof RebarBlock)) return IFlowingBlock.super.empty(state);
        BlockState empty = Concrete.REBAR_BLOCK.get().defaultBlockState();
        empty = IFlowingBlock.copyValidTo(state, empty);
        empty = empty.setValue(LEVEL, 0);
        return empty;
    }

    @Override
    public void neighborChanged(BlockState us, Level level, BlockPos here, Block other, BlockPos changed, boolean bool)
    {
        if (level instanceof ServerLevel slevel) reScheduleTick(us, slevel, here);
    }

    @Override
    public boolean canReplace(BlockState state)
    {
        if (state.getBlock() instanceof RebarBlock) return true;
        return IFlowingBlock.super.canReplace(state);
    }

    @Override
    public BlockState makeFalling(BlockState state, boolean falling)
    {
        if (state.getBlock() == this) return state;
        return IFlowingBlock.super.makeFalling(state, falling);
    }

    @Override
    public void onStableTick(BlockState state, ServerLevel level, BlockPos pos, Random random)
    {
        if (random.nextDouble() > hardenRate)
        {
            reScheduleTick(state, level, pos);
            return;
        }
        int index = colour.ordinal();
        int amt = this.getAmount(state);
        Block blockTo = amt == 16 ? Concrete.REF_BLOCK[index].get() : Concrete.REF_LAYER[index].get();
        BlockState stateTo = IFlowingBlock.copyValidTo(state, blockTo.defaultBlockState());
        if (amt != 16) stateTo = setAmount(stateTo, amt);
        level.setBlock(pos, stateTo, 3);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, Random random)
    {
        this.doTick(state, level, pos, random);
    }

    @Override
    public boolean isScaffolding(BlockState state, LevelReader world, BlockPos pos, LivingEntity entity)
    {
        return true;
    }

    @Override
    public boolean isLadder(BlockState state, LevelReader world, BlockPos pos, LivingEntity entity)
    {
        int i = this.getAABBIndex(state);
        if (i == 0) return false;
        return super.isLadder(state, world, pos, entity);
    }
}
