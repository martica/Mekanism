package mekanism.common.block.basic;

import javax.annotation.Nonnull;
import mekanism.api.block.IHasInventory;
import mekanism.api.block.IHasTileEntity;
import mekanism.api.block.ISupportsComparator;
import mekanism.common.MekanismLang;
import mekanism.common.base.ILangEntry;
import mekanism.common.block.interfaces.IHasDescription;
import mekanism.common.block.interfaces.IHasGui;
import mekanism.common.inventory.container.ContainerProvider;
import mekanism.common.inventory.container.tile.ThermoelectricBoilerContainer;
import mekanism.common.tile.TileEntityBoilerValve;
import mekanism.common.tile.base.MekanismTileEntityTypes;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.tileentity.TileEntityType;

public class BlockBoilerValve extends BlockBasicMultiblock implements IHasInventory, IHasTileEntity<TileEntityBoilerValve>, ISupportsComparator, IHasDescription,
      IHasGui<TileEntityBoilerValve> {

    @Override
    public INamedContainerProvider getProvider(TileEntityBoilerValve tile) {
        return new ContainerProvider(MekanismLang.BOILER, (i, inv, player) -> new ThermoelectricBoilerContainer(i, inv, tile));
    }

    @Override
    public TileEntityType<TileEntityBoilerValve> getTileType() {
        return MekanismTileEntityTypes.BOILER_VALVE.getTileEntityType();
    }

    @Nonnull
    @Override
    public ILangEntry getDescription() {
        return MekanismLang.DESCRIPTION_BOILER_VALVE;
    }
}