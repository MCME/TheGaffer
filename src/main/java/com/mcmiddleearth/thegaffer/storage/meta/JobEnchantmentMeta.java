/*  This file is part of TheGaffer.
 * 
 *  TheGaffer is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  TheGaffer is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with TheGaffer.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mcmiddleearth.thegaffer.storage.meta;

import java.util.HashMap;
import java.util.Map.Entry;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.ItemMeta;

public class JobEnchantmentMeta {

    @Getter
    @Setter
    private HashMap<String, Integer> enchants = new HashMap();

    public JobEnchantmentMeta(ItemMeta meta) {
        for (Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
            enchants.put(entry.getKey().getName(), entry.getValue());
        }
    }
    
    public JobEnchantmentMeta() {
        
    }
}
