package mekanism.api.heat;

import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import mekanism.api.NBTConstants;
import mekanism.api.math.FloatingLong;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.INBTSerializable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface IHeatCapacitor extends INBTSerializable<CompoundNBT> {

    FloatingLong getTemperature();

    FloatingLong getInverseConductionCoefficient();

    FloatingLong getInsulationCoefficient();

    FloatingLong getHeatCapacity();

    void handleTemperatureChange(TemperaturePacket transfer);

    @Override
    default CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.putString(NBTConstants.STORED, getTemperature().toString());
        return nbt;
    }
}
