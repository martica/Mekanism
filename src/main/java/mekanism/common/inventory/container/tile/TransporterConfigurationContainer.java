package mekanism.common.inventory.container.tile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import mekanism.common.base.ISideConfiguration;
import mekanism.common.inventory.container.IEmptyContainer;
import mekanism.common.inventory.container.MekanismContainerTypes;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.util.text.TextComponentUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;

public class TransporterConfigurationContainer<TILE extends TileEntityMekanism & ISideConfiguration> extends MekanismTileContainer<TILE> implements IEmptyContainer {

    public TransporterConfigurationContainer(int id, PlayerInventory inv, TILE tile) {
        super(MekanismContainerTypes.TRANSPORTER_CONFIGURATION, id, inv, tile);
    }

    public TransporterConfigurationContainer(int id, PlayerInventory inv, PacketBuffer buf) {
        //TODO
        this(id, inv, (TILE) getTileFromBuf(buf, TileEntityMekanism.class));
    }

    @Nullable
    @Override
    public Container createMenu(int i, @Nonnull PlayerInventory inv, @Nonnull PlayerEntity player) {
        return new TransporterConfigurationContainer<>(i, inv, tile);
    }

    @Nonnull
    @Override
    public ITextComponent getDisplayName() {
        return TextComponentUtil.translate("mekanism.container.transporter_configuration");
    }
}