package items;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

// Guarda inventarios (mochilas, monedero, eventos) como texto en la base de datos. El Base64 es el de Java: el Base64Coder
// de SnakeYAML ya no viene en Paper 26.2 y cada guardado fallaba. El decodificador MIME lee igual lo guardado antes
// (con saltos de línea)
public class ItemSerializer {

    public static String serialize(ItemStack[] items) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            dataOutput.writeInt(items.length);

            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }

            dataOutput.close();
            return Base64.getMimeEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static ItemStack[] deserialize(String data) {
        if (data == null || data.isEmpty()) return new ItemStack[0];
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getMimeDecoder().decode(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);

            ItemStack[] items = new ItemStack[dataInput.readInt()];

            for (int i = 0; i < items.length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }

            dataInput.close();
            return items;
        } catch (Exception e) {
            e.printStackTrace();
            return new ItemStack[0];
        }
    }
}
