package mekanism.common.block;

import java.util.Random;
import javax.annotation.Nonnull;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.text.EnumColor;
import mekanism.common.MekanismLang;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.BlockTypeTile;
import mekanism.common.registries.MekanismBlockTypes;
import mekanism.common.registries.MekanismParticleTypes;
import mekanism.common.tile.TileEntityRadioactiveWasteBarrel;
import mekanism.common.util.MekanismUtils;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

public class BlockRadioactiveWasteBarrel extends BlockTile<TileEntityRadioactiveWasteBarrel, BlockTypeTile<TileEntityRadioactiveWasteBarrel>> {

    public BlockRadioactiveWasteBarrel() {
        super(MekanismBlockTypes.RADIOACTIVE_WASTE_BARREL);
    }

    @Override
    public void animateTick(@Nonnull BlockState state, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull Random random) {
        TileEntityRadioactiveWasteBarrel tile = MekanismUtils.getTileEntity(TileEntityRadioactiveWasteBarrel.class, world, pos);
        if (tile != null) {
            int count = (int) (10 * tile.getGasScale());
            if (count > 0) {
                for (int i = 0; i < random.nextInt(count); i++) {
                    double randX = pos.getX() - 0.1 + random.nextDouble() * 1.2;
                    double randY = pos.getY() - 0.1 + random.nextDouble() * 1.2;
                    double randZ = pos.getZ() - 0.1 + random.nextDouble() * 1.2;
                    world.addParticle((BasicParticleType) MekanismParticleTypes.RADIATION.getParticleType(), randX, randY, randZ, 0, 0, 0);
                }
            }
        }
    }

    @Override
    @Deprecated
    public float getPlayerRelativeBlockHardness(@Nonnull BlockState state, @Nonnull PlayerEntity player, @Nonnull IBlockReader world, @Nonnull BlockPos pos) {
        float speed = super.getPlayerRelativeBlockHardness(state, player, world, pos);
        TileEntityRadioactiveWasteBarrel tile = MekanismUtils.getTileEntity(TileEntityRadioactiveWasteBarrel.class, world, pos);
        return tile != null && tile.getGasScale() > 0 ? speed / 5F : speed;
    }

    @Nonnull
    @Override
    @Deprecated
    public ActionResultType onBlockActivated(@Nonnull BlockState state, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull PlayerEntity player, @Nonnull Hand hand,
          @Nonnull BlockRayTraceResult hit) {
        if (!player.isSneaking()) {
            return ActionResultType.PASS;
        }
        TileEntityRadioactiveWasteBarrel tile = MekanismUtils.getTileEntity(TileEntityRadioactiveWasteBarrel.class, world, pos);
        if (tile == null) {
            return ActionResultType.PASS;
        }
        if (!world.isRemote()) {
            GasStack stored = tile.getGas();
            ITextComponent text;
            if (stored.isEmpty()) {
                text = MekanismLang.NO_GAS.translateColored(EnumColor.GRAY);
            } else {
                text = MekanismLang.STORED.translateColored(EnumColor.ORANGE, EnumColor.ORANGE, stored, EnumColor.GRAY, stored.getAmount());
            }
            player.sendMessage(text);
        }
        return ActionResultType.SUCCESS;
    }
}
