package mcjty.xnet.apiimpl.fluids;

import com.google.common.collect.ImmutableSet;
import mcjty.lib.tools.FluidTools;
import mcjty.lib.tools.ItemStackTools;
import mcjty.xnet.api.channels.IConnectorSettings;
import mcjty.xnet.api.channels.RSMode;
import mcjty.xnet.api.gui.IEditorGui;
import mcjty.xnet.api.gui.IndicatorIcon;
import mcjty.xnet.blocks.controller.GuiController;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

public class FluidConnectorSettings implements IConnectorSettings {

    public static final String TAG_FACING = "facing";
    public static final String TAG_MODE = "mode";
    public static final String TAG_RS = "rs";
    public static final String TAG_RATE = "rate";
    public static final String TAG_MINMAX = "minmax";
    public static final String TAG_PRIORITY = "priority";
    public static final String TAG_FILTER = "flt";
    public static final String TAG_SPEED = "speed";

    public static final int MAXRATE_ADVANCED = 5000;
    public static final int MAXRATE_NORMAL = 1000;

    enum FluidMode {
        INS,
        EXT
    }

    private final boolean advanced;

    private FluidMode fluidMode = FluidMode.INS;
    private RSMode rsMode = RSMode.IGNORED;

    @Nullable private Integer priority = 0;
    @Nullable private Integer rate = null;
    @Nullable private Integer minmax = null;
    private int speed = 2;

    @Nonnull private final EnumFacing side;
    @Nullable private EnumFacing facingOverride = null; // Only available on advanced connectors
    private ItemStack filter = ItemStackTools.getEmptyStack();

    public FluidConnectorSettings(boolean advanced, @Nonnull EnumFacing side) {
        this.advanced = advanced;
        this.side = side;
    }

    @Nonnull
    public EnumFacing getFacing() {
        return facingOverride == null ? side : facingOverride;
    }

    public FluidMode getFluidMode() {
        return fluidMode;
    }

    public int getSpeed() {
        return speed;
    }

    @Nonnull
    public Integer getPriority() {
        return priority == null ? 0 : priority;
    }

    @Nonnull
    public Integer getRate() {
        return rate == null ? MAXRATE_NORMAL : rate;
    }

    @Nullable
    public Integer getMinmax() {
        return minmax;
    }

    public RSMode getRsMode() {
        return rsMode;
    }


    @Nullable
    @Override
    public IndicatorIcon getIndicatorIcon() {
        switch (fluidMode) {
            case INS:
                return new IndicatorIcon(GuiController.iconGuiElements, 0, 70, 13, 10);
            case EXT:
                return new IndicatorIcon(GuiController.iconGuiElements, 13, 70, 13, 10);
        }
        return null;
    }

    @Override
    @Nullable
    public String getIndicator() {
        return null;
    }

    @Override
    public void createGui(IEditorGui gui) {
        String[] speeds;
        int maxrate;
        if (advanced) {
            speeds = new String[] { "10", "20", "60", "100", "200" };
            maxrate = MAXRATE_ADVANCED;
        } else {
            speeds = new String[] { "20", "60", "100", "200" };
            maxrate = MAXRATE_NORMAL;
        }

        gui
                .choices(TAG_FACING, "Side from which to operate", facingOverride == null ? side : facingOverride, EnumFacing.VALUES)
                .choices(TAG_MODE, "Insert or extract mode", fluidMode, FluidMode.values())
                .choices(TAG_SPEED, "Number of ticks for each operation", Integer.toString(speed * 10), speeds)
                .shift(10)
                .redstoneMode(TAG_RS, rsMode).nl()

                .label("Pri").integer(TAG_PRIORITY, "Insertion priority", priority).nl()

                .label("Rate")
                .integer(TAG_RATE, fluidMode == FluidMode.EXT ? "Fluid extraction rate (max " + maxrate + "mb)" : "Fluid insertion rate (max " + maxrate + "mb)", rate)
                .shift(10)
                .label(fluidMode == FluidMode.EXT ? "Min" : "Max")
                .integer(TAG_MINMAX, fluidMode == FluidMode.EXT ? "Disable extraction if fluid is too low" : "Disable insertion if fluid is too high", minmax)
                .nl()
                .label("Filter")
                .ghostSlot(TAG_FILTER, filter);

    }

    private static Set<String> INSERT_TAGS = ImmutableSet.of(TAG_MODE, TAG_RS, TAG_RATE, TAG_MINMAX, TAG_PRIORITY, TAG_FILTER);
    private static Set<String> EXTRACT_TAGS = ImmutableSet.of(TAG_MODE, TAG_RS, TAG_RATE, TAG_MINMAX, TAG_PRIORITY, TAG_FILTER, TAG_SPEED);

    @Override
    public boolean isEnabled(String tag) {
        if (fluidMode == FluidMode.INS) {
            if (tag.equals(TAG_FACING)) {
                return advanced;
            }
            return INSERT_TAGS.contains(tag);
        } else {
            if (tag.equals(TAG_FACING)) {
                return false;           // We cannot extract from different sides
            }
            return EXTRACT_TAGS.contains(tag);
        }
    }

    @Nullable
    public FluidStack getMatcher() {
        // @todo optimize/cache this?
        if (ItemStackTools.isValid(filter)) {
            return FluidTools.convertBucketToFluid(filter);
        } else {
            return null;
        }
    }


    @Override
    public void update(Map<String, Object> data) {
        fluidMode = FluidMode.valueOf(((String)data.get(TAG_MODE)).toUpperCase());
        rsMode = RSMode.valueOf(((String)data.get(TAG_RS)).toUpperCase());
        rate = (Integer) data.get(TAG_RATE);
        int maxrate;
        if (advanced) {
            maxrate = MAXRATE_ADVANCED;
        } else {
            maxrate = MAXRATE_NORMAL;
        }
        if (rate != null && rate > maxrate) {
            rate = maxrate;
        }
        minmax = (Integer) data.get(TAG_MINMAX);
        priority = (Integer) data.get(TAG_PRIORITY);
        String facing = (String) data.get(TAG_FACING);
        facingOverride = facing == null ? null : EnumFacing.byName(facing);
        speed = Integer.parseInt((String) data.get(TAG_SPEED)) / 10;
        if (speed == 0) {
            speed = 2;
        }
        filter = (ItemStack) data.get(TAG_FILTER);
        if (filter == null) {
            filter = ItemStackTools.getEmptyStack();
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        fluidMode = FluidMode.values()[tag.getByte("fluidMode")];
        rsMode = RSMode.values()[tag.getByte("rsMode")];
        if (tag.hasKey("priority")) {
            priority = tag.getInteger("priority");
        } else {
            priority = null;
        }
        if (tag.hasKey("rate")) {
            rate = tag.getInteger("rate");
        } else {
            rate = null;
        }
        if (tag.hasKey("minmax")) {
            minmax = tag.getInteger("minmax");
        } else {
            minmax = null;
        }
        if (tag.hasKey("facingOverride")) {
            facingOverride = EnumFacing.VALUES[tag.getByte("facingOverride")];
        }
        speed = tag.getInteger("speed");
        if (speed == 0) {
            speed = 2;
        }
        if (tag.hasKey("filter")) {
            NBTTagCompound itemTag = tag.getCompoundTag("filter");
            filter = ItemStackTools.loadFromNBT(itemTag);
        } else {
            filter = ItemStackTools.getEmptyStack();
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        tag.setByte("fluidMode", (byte) fluidMode.ordinal());
        tag.setByte("rsMode", (byte) rsMode.ordinal());
        if (priority != null) {
            tag.setInteger("priority", priority);
        }
        if (rate != null) {
            tag.setInteger("rate", rate);
        }
        if (minmax != null) {
            tag.setInteger("minmax", minmax);
        }
        if (facingOverride != null) {
            tag.setByte("facingOverride", (byte) facingOverride.ordinal());
        }
        tag.setInteger("speed", speed);
        if (ItemStackTools.isValid(filter)) {
            NBTTagCompound itemTag = new NBTTagCompound();
            filter.writeToNBT(itemTag);
            tag.setTag("filter", itemTag);
        }
    }
}
