package mekanism.api.transmitters;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.Nullable;
import mekanism.api.Coord4D;
import mekanism.api.IClientTicker;
import mekanism.api.Range4D;
import mekanism.api.text.IHasTextComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.thread.EffectiveSide;

public abstract class DynamicNetwork<ACCEPTOR, NETWORK extends DynamicNetwork<ACCEPTOR, NETWORK, BUFFER>, BUFFER> implements IClientTicker, INetworkDataHandler, IHasTextComponent {

    /**
     * Cached value of {@link Direction#values()}. DO NOT MODIFY THIS LIST.
     */
    private static final Direction[] DIRECTIONS = Direction.values();

    protected Set<IGridTransmitter<ACCEPTOR, NETWORK, BUFFER>> transmitters = new HashSet<>();
    protected Set<IGridTransmitter<ACCEPTOR, NETWORK, BUFFER>> transmittersToAdd = new HashSet<>();
    protected Set<IGridTransmitter<ACCEPTOR, NETWORK, BUFFER>> transmittersAdded = new HashSet<>();

    protected Set<Coord4D> possibleAcceptors = new HashSet<>();
    protected Map<Coord4D, EnumSet<Direction>> acceptorDirections = new HashMap<>();
    protected Map<IGridTransmitter<ACCEPTOR, NETWORK, BUFFER>, EnumSet<Direction>> changedAcceptors = new HashMap<>();
    protected Range4D packetRange = null;
    protected int capacity = 0;
    protected double doubleCapacity = 0;
    @Deprecated
    protected double meanCapacity = 0;
    protected boolean needsUpdate = false;
    protected int updateDelay = 0;
    protected boolean firstUpdate = true;
    @Nullable
    protected World world = null;
    private Set<DelayQueue> updateQueue = new LinkedHashSet<>();

    public void addNewTransmitters(Collection<IGridTransmitter<ACCEPTOR, NETWORK, BUFFER>> newTransmitters) {
        transmittersToAdd.addAll(newTransmitters);
    }

    public void commit() {
        if (!transmittersToAdd.isEmpty()) {
            for (IGridTransmitter<ACCEPTOR, NETWORK, BUFFER> transmitter : transmittersToAdd) {
                if (transmitter.isValid()) {
                    if (world == null) {
                        world = transmitter.world();
                    }

                    for (Direction side : DIRECTIONS) {
                        updateTransmitterOnSide(transmitter, side);
                    }

                    transmitter.setTransmitterNetwork((NETWORK) this);
                    absorbBuffer(transmitter);
                    transmitters.add(transmitter);
                }
            }

            updateCapacity();
            clampBuffer();
            queueClientUpdate(transmittersToAdd);
            transmittersToAdd.clear();
        }

        if (!changedAcceptors.isEmpty()) {
            for (Entry<IGridTransmitter<ACCEPTOR, NETWORK, BUFFER>, EnumSet<Direction>> entry : changedAcceptors.entrySet()) {
                IGridTransmitter<ACCEPTOR, NETWORK, BUFFER> transmitter = entry.getKey();
                if (transmitter.isValid()) {
                    //Update all the changed directions
                    for (Direction side : entry.getValue()) {
                        updateTransmitterOnSide(transmitter, side);
                    }
                }
            }
            changedAcceptors.clear();
        }
    }

    public void updateTransmitterOnSide(IGridTransmitter<ACCEPTOR, NETWORK, BUFFER> transmitter, Direction side) {
        ACCEPTOR acceptor = transmitter.getAcceptor(side);
        Coord4D acceptorCoord = transmitter.coord().offset(side);
        EnumSet<Direction> directions = acceptorDirections.get(acceptorCoord);

        if (acceptor != null) {
            possibleAcceptors.add(acceptorCoord);
            if (directions != null) {
                directions.add(side.getOpposite());
            } else {
                acceptorDirections.put(acceptorCoord, EnumSet.of(side.getOpposite()));
            }
        } else if (directions != null) {
            directions.remove(side.getOpposite());

            if (directions.isEmpty()) {
                possibleAcceptors.remove(acceptorCoord);
                acceptorDirections.remove(acceptorCoord);
            }
        } else {
            possibleAcceptors.remove(acceptorCoord);
            acceptorDirections.remove(acceptorCoord);
        }
    }

    @Nullable
    public BUFFER getBuffer() {
        return null;
    }

    public boolean isRemote() {
        //TODO: See if there is anyway to improve this so we don't have to call EffectiveSide.get
        return world == null ? EffectiveSide.get() == LogicalSide.CLIENT : world.isRemote;
    }

    public abstract void absorbBuffer(IGridTransmitter<ACCEPTOR, NETWORK, BUFFER> transmitter);

    public abstract void clampBuffer();

    public void invalidate() {
        //Remove invalid transmitters first for share calculations
        transmitters.removeIf(transmitter -> !transmitter.isValid());

        //Clamp the new buffer
        clampBuffer();

        //Update all shares
        for (IGridTransmitter<ACCEPTOR, NETWORK, BUFFER> transmitter : transmitters) {
            transmitter.updateShare();
        }

        //Now invalidate the transmitters
        for (IGridTransmitter<ACCEPTOR, NETWORK, BUFFER> transmitter : transmitters) {
            invalidateTransmitter(transmitter);
        }

        transmitters.clear();
        deregister();
    }

    public void invalidateTransmitter(IGridTransmitter<ACCEPTOR, NETWORK, BUFFER> transmitter) {
        if (!isRemote() && transmitter.isValid()) {
            transmitter.takeShare();
            transmitter.setTransmitterNetwork(null);
            TransmitterNetworkRegistry.registerOrphanTransmitter(transmitter);
        }
    }

    public void acceptorChanged(IGridTransmitter<ACCEPTOR, NETWORK, BUFFER> transmitter, Direction side) {
        EnumSet<Direction> directions = changedAcceptors.get(transmitter);
        if (directions != null) {
            directions.add(side);
        } else {
            changedAcceptors.put(transmitter, EnumSet.of(side));
        }
        TransmitterNetworkRegistry.registerChangedNetwork(this);
    }

    public void adoptTransmittersAndAcceptorsFrom(NETWORK net) {
        for (IGridTransmitter<ACCEPTOR, NETWORK, BUFFER> transmitter : net.transmitters) {
            transmitter.setTransmitterNetwork((NETWORK) this);
            transmitters.add(transmitter);
            transmittersAdded.add(transmitter);
        }

        transmittersToAdd.addAll(net.transmittersToAdd);
        possibleAcceptors.addAll(net.possibleAcceptors);

        for (Entry<Coord4D, EnumSet<Direction>> entry : net.acceptorDirections.entrySet()) {
            Coord4D coord = entry.getKey();
            if (acceptorDirections.containsKey(coord)) {
                acceptorDirections.get(coord).addAll(entry.getValue());
            } else {
                acceptorDirections.put(coord, entry.getValue());
            }
        }
    }

    public Range4D getPacketRange() {
        return packetRange == null ? genPacketRange() : packetRange;
    }

    protected Range4D genPacketRange() {
        if (getSize() == 0) {
            deregister();
            return null;
        }

        IGridTransmitter<ACCEPTOR, NETWORK, BUFFER> initTransmitter = transmitters.iterator().next();
        Coord4D initCoord = initTransmitter.coord();

        int minX = initCoord.x;
        int minY = initCoord.y;
        int minZ = initCoord.z;
        int maxX = initCoord.x;
        int maxY = initCoord.y;
        int maxZ = initCoord.z;

        for (IGridTransmitter<ACCEPTOR, NETWORK, BUFFER> transmitter : transmitters) {
            Coord4D coord = transmitter.coord();
            if (coord.x < minX) {
                minX = coord.x;
            } else if (coord.x > maxX) {
                maxX = coord.x;
            }
            if (coord.y < minY) {
                minY = coord.y;
            } else if (coord.y > maxY) {
                maxY = coord.y;
            }
            if (coord.z < minZ) {
                minZ = coord.z;
            } else if (coord.x > maxZ) {
                maxZ = coord.z;
            }
        }
        return new Range4D(minX, minY, minZ, maxX, maxY, maxZ, initTransmitter.world().getDimension().getType());
    }

    public void register() {
        if (!isRemote()) {
            TransmitterNetworkRegistry.getInstance().registerNetwork(this);
        } else {
            MinecraftForge.EVENT_BUS.post(new ClientTickUpdate(this, (byte) 1));
        }
    }

    public void deregister() {
        transmitters.clear();
        transmittersToAdd.clear();
        transmittersAdded.clear();

        if (!isRemote()) {
            TransmitterNetworkRegistry.getInstance().removeNetwork(this);
        } else {
            MinecraftForge.EVENT_BUS.post(new ClientTickUpdate(this, (byte) 0));
        }
    }

    public int getSize() {
        return transmitters.size();
    }

    public int getAcceptorSize() {
        return possibleAcceptors.size();
    }

    public synchronized void updateCapacity() {
        doubleCapacity = transmitters.stream().mapToDouble(IGridTransmitter::getCapacity).sum();
        capacity = doubleCapacity > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) doubleCapacity;
    }

    public int getCapacity() {
        return capacity;
    }

    public double getCapacityAsDouble() {
        return doubleCapacity;
    }

    @Nullable
    public World getWorld() {
        return world;
    }

    public void tick() {
        onUpdate();
    }

    public void onUpdate() {
        if (!isRemote()) {
            Iterator<DelayQueue> i = updateQueue.iterator();

            try {
                while (i.hasNext()) {
                    DelayQueue q = i.next();
                    if (q.delay > 0) {
                        q.delay--;
                    } else {
                        transmittersAdded.addAll(transmitters);
                        updateDelay = 1;
                        i.remove();
                    }
                }
            } catch (Exception ignored) {
            }

            if (updateDelay > 0) {
                updateDelay--;
                if (updateDelay == 0) {
                    MinecraftForge.EVENT_BUS.post(new TransmittersAddedEvent(this, firstUpdate, (Collection) transmittersAdded));
                    firstUpdate = false;
                    transmittersAdded.clear();
                    needsUpdate = true;
                }
            }
        }
    }

    @Override
    public boolean needsTicks() {
        return getSize() > 0;
    }

    @Override
    public void clientTick() {
    }

    public void queueClientUpdate(Collection<IGridTransmitter<ACCEPTOR, NETWORK, BUFFER>> newTransmitters) {
        transmittersAdded.addAll(newTransmitters);
        updateDelay = 5;
    }

    public void addUpdate(PlayerEntity player) {
        updateQueue.add(new DelayQueue(player));
    }

    public boolean isCompatibleWith(NETWORK other) {
        return true;
    }

    public boolean compatibleWithBuffer(BUFFER buffer) {
        return true;
    }

    public Set<IGridTransmitter<ACCEPTOR, NETWORK, BUFFER>> getTransmitters() {
        return transmitters;
    }

    public boolean addTransmitter(IGridTransmitter<ACCEPTOR, NETWORK, BUFFER> transmitter) {
        return transmitters.add(transmitter);
    }

    public boolean removeTransmitter(IGridTransmitter<ACCEPTOR, NETWORK, BUFFER> transmitter) {
        boolean removed = transmitters.remove(transmitter);
        if (transmitters.isEmpty()) {
            deregister();
        }
        return removed;
    }

    public IGridTransmitter<ACCEPTOR, NETWORK, BUFFER> firstTransmitter() {
        return transmitters.iterator().next();
    }

    public int transmittersSize() {
        return transmitters.size();
    }

    public Set<IGridTransmitter<ACCEPTOR, NETWORK, BUFFER>> getTransmittersToAdd() {
        return transmittersToAdd;
    }

    public Set<IGridTransmitter<ACCEPTOR, NETWORK, BUFFER>> getTransmittersAdded() {
        return transmittersAdded;
    }

    public Set<Coord4D> getPossibleAcceptors() {
        return possibleAcceptors;
    }

    public Map<Coord4D, EnumSet<Direction>> getAcceptorDirections() {
        return acceptorDirections;
    }

    public Map<IGridTransmitter<ACCEPTOR, NETWORK, BUFFER>, EnumSet<Direction>> getChangedAcceptors() {
        return changedAcceptors;
    }

    public static class TransmittersAddedEvent extends Event {

        public DynamicNetwork<?, ?, ?> network;
        public boolean newNetwork;
        public Collection<IGridTransmitter<?, ?, ?>> newTransmitters;

        public TransmittersAddedEvent(DynamicNetwork<?, ?, ?> net, boolean newNet, Collection<IGridTransmitter<?, ?, ?>> added) {
            network = net;
            newNetwork = newNet;
            newTransmitters = added;
        }
    }

    public static class ClientTickUpdate extends Event {

        public DynamicNetwork<?, ?, ?> network;
        public byte operation; /*0 remove, 1 add*/

        public ClientTickUpdate(DynamicNetwork<?, ?, ?> net, byte b) {
            network = net;
            operation = b;
        }
    }

    public static class NetworkClientRequest extends Event {

        public TileEntity tile;

        public NetworkClientRequest(TileEntity tile) {
            this.tile = tile;
        }
    }

    public static class DelayQueue {

        public PlayerEntity player;
        public int delay;

        public DelayQueue(PlayerEntity p) {
            player = p;
            delay = 5;
        }

        @Override
        public int hashCode() {
            return player.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof DelayQueue && ((DelayQueue) o).player.equals(this.player);
        }
    }
}