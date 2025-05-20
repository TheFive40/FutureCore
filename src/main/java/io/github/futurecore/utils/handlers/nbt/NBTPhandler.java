package io.github.futurecore.utils.handlers.nbt;

import net.minecraft.server.v1_7_R4.NBTTagCompound;
import net.minecraft.server.v1_7_R4.NBTTagInt;
import net.minecraft.server.v1_7_R4.NBTTagList;
import net.minecraft.server.v1_7_R4.NBTTagString;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public class NBTPhandler {
    private final Player player;
    private NBTTagCompound compound;
    private final net.minecraft.server.v1_7_R4.EntityPlayer nmsPlayer;

    public NBTPhandler(Player player) {
        this.player = player;
        this.nmsPlayer = ((CraftPlayer) player).getHandle();

        this.compound = new NBTTagCompound();
        this.nmsPlayer.e(this.compound);

        if (this.compound == null) {
            this.compound = new NBTTagCompound();
        }
    }

    public boolean hasNBT() {
        return this.compound != null && !this.compound.isEmpty();
    }

    public void setString(String key, String value) {
        this.compound.setString(key, value);
        saveChanges();
    }

    public String getString(String key) {
        return this.compound.getString(key);
    }

    public void setInteger(String key, int value) {
        this.compound.setInt(key, value);
        saveChanges();
    }

    public int getInteger(String key) {
        return this.compound.getInt(key);
    }

    public void setBoolean(String key, boolean value) {
        this.compound.setBoolean(key, value);
        saveChanges();
    }

    public boolean getBoolean(String key) {
        return this.compound.getBoolean(key);
    }

    public void setDouble(String key, double value) {
        this.compound.setDouble(key, value);
        saveChanges();
    }

    public double getDouble(String key) {
        return this.compound.getDouble(key);
    }

    public void setLong(String key, long value) {
        this.compound.setLong(key, value);
        saveChanges();
    }

    public long getLong(String key) {
        return this.compound.getLong(key);
    }

    public void setFloat(String key, float value) {
        this.compound.setFloat(key, value);
        saveChanges();
    }

    public float getFloat(String key) {
        return this.compound.getFloat(key);
    }

    public void removeKey(String key) {
        this.compound.remove(key);
        saveChanges();
    }

    public boolean hasKey(String key) {
        return this.compound.hasKey(key);
    }

    public void addCompound(String key) {
        NBTTagCompound newComp = new NBTTagCompound();
        this.setCompound(key, newComp);
    }

    public void setCompound(String key, NBTTagCompound compound) {
        this.compound.set(key, compound);
        saveChanges();
    }

    public NBTTagCompound getCompound(String key) {
        return this.compound.getCompound(key);
    }

    public void saveChanges() {
        try {
            this.nmsPlayer.f(this.compound);

            File playerDir = new File(nmsPlayer.world.getDataManager().getDirectory(), "playerdata");
            if (!playerDir.exists()) {
                playerDir.mkdirs();
            }

            File playerFile = new File(playerDir, nmsPlayer.getUniqueID().toString() + ".dat");

            java.io.DataOutputStream dos = new java.io.DataOutputStream(
                    new java.io.FileOutputStream(playerFile));
            net.minecraft.server.v1_7_R4.NBTCompressedStreamTools.a(this.compound, (OutputStream) dos );
            dos.close();
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage("Error saving NBT data for player " +
                    player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setAttribute(String attributeName, double amount, int operation) {
        NBTTagList modifiers = this.compound.getList("AttributeModifiers", 10);
        if (modifiers == null) {
            modifiers = new NBTTagList();
        }

        NBTTagCompound attribute = new NBTTagCompound();
        attribute.set("AttributeName", new NBTTagString(attributeName));
        attribute.set("Name", new NBTTagString(attributeName));
        attribute.set("Amount", new NBTTagInt((int) amount));
        attribute.set("Operation", new NBTTagInt(operation));
        attribute.set("UUIDMost", new NBTTagInt(player.getUniqueId().hashCode()));
        attribute.set("UUIDLeast", new NBTTagInt(player.getUniqueId().hashCode()));

        modifiers.add(attribute);
        this.compound.set("AttributeModifiers", modifiers);
        saveChanges();
    }
}