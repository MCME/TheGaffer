package co.mcme.jobs.util;

import com.google.gson.Gson;
import java.util.Map;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

public class ItemUtil {

    static String quote = "\"";
    static Gson gson = new Gson();

    public static String getJsonItems(PlayerInventory inv) {
        StringBuilder out = new StringBuilder();
        out.append("[{");
        for (ItemStack is : inv.getContents()) {
            for (int slot : inv.all(is).keySet()) {
                ItemStack is2 = inv.getItem(slot);
                Map<String, Object> inf = is2.serialize();
                out.append(quote).append(slot).append(quote).append(": [{");
                out.append(quote).append("itemid").append(quote).append(": ").append(quote).append(is2.getTypeId()).append(quote).append(",");
                out.append(quote).append("amount").append(quote).append(": ").append(quote).append(is2.getAmount()).append(quote).append(",");
                out.append(quote).append("damage").append(quote).append(": ").append(quote).append(is2.getData().getData()).append(quote);
                if (is2.hasItemMeta()) {
                    out.append(",").append(quote).append("meta").append(quote).append(": ").append(getMetaForItem(is2));
                }
                out.append("}]");
                out.append(",");
            }
        }
        String without = out.substring(0, out.toString().length() - 1);
        out = new StringBuilder();
        out.append(without);
        out.append("}]");
        return out.toString();
    }

    private static String getMetaForItem(ItemStack is) {
        StringBuilder out = new StringBuilder();
        ItemMeta meta = is.getItemMeta();
        boolean other = false;
        out.append("{");
        if (meta instanceof BookMeta) {
            other = true;
            BookMeta bmeta = (BookMeta) meta;
            out.append(quote).append("type").append(quote).append(": ").append(quote).append("book").append(quote).append(",");
            if (bmeta.hasTitle()) {
                out.append(quote).append("title").append(quote).append(": ").append(quote).append(bmeta.getTitle()).append(quote).append(",");
            }
            if (bmeta.hasAuthor()) {
                out.append(quote).append("author").append(quote).append(": ").append(quote).append(bmeta.getAuthor()).append(quote).append(",");
            }
            if (bmeta.hasPages()) {
                out.append(quote).append("pages").append(quote).append(": ").append("{");
                int i = 1;
                for (String page : bmeta.getPages()) {
                    out.append(quote).append(bmeta.getPages().indexOf(page)).append(quote).append(": ").append(gson.toJson(page));
                    if (i < bmeta.getPageCount()) {
                        out.append(",");
                    }
                    i++;
                }
                out.append("}");
            }
        }
        if (meta instanceof LeatherArmorMeta) {
            other = true;
            LeatherArmorMeta lmeta = (LeatherArmorMeta) meta;
            out.append(quote).append("type").append(quote).append(": ").append(quote).append("armor").append(quote).append(",");
            out.append(quote).append("color").append(quote).append(": ").append("{");
            out.append(quote).append("red").append(quote).append(": ").append(lmeta.getColor().getRed()).append(",");
            out.append(quote).append("green").append(quote).append(": ").append(lmeta.getColor().getGreen()).append(",");
            out.append(quote).append("blue").append(quote).append(": ").append(lmeta.getColor().getBlue()).append("}");
        }
        if (meta.hasDisplayName()) {
            if (other) {
                out.append(",");
            }
            out.append(quote).append("displayname").append(quote).append(": ").append(quote).append(meta.getDisplayName()).append(quote);
        }
        if (meta.hasLore()) {
            if (other) {
                out.append(",");
            }
            out.append(quote).append("lore").append(quote).append(": ").append("{");
            int i = 1;
            for (String lore : meta.getLore()) {
                out.append(quote).append(meta.getLore().indexOf(lore)).append(quote).append(": ").append(gson.toJson(lore));
                if (i < meta.getLore().size()) {
                    out.append(",");
                }
                i++;
            }
            out.append("}");
        }
        out.append("}");
        return out.toString();
    }
}
