package io.github.futurecore.utils;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class ItemSerializationUtil {

    public static String serializeItem(ItemStack item) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOut = new BukkitObjectOutputStream(out)) {

            dataOut.writeObject(item);
            return Base64.getEncoder().encodeToString(out.toByteArray());

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ItemStack deserializeItem(String base64) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
             BukkitObjectInputStream dataIn = new BukkitObjectInputStream(in)) {

            return (ItemStack) dataIn.readObject();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void saveItemsToFile(File file, List<ItemStack> items) {
        List<String> serialized = new ArrayList<>();
        for (ItemStack item : items) {
            serialized.add(serializeItem(item));
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (String line : serialized) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<ItemStack> loadItemsFromFile(File file) {
        List<ItemStack> items = new ArrayList<>();
        if (!file.exists()) return items;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                ItemStack item = deserializeItem(line);
                if (item != null) items.add(item);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return items;
    }
}

