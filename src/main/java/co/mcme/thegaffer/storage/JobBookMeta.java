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
package co.mcme.thegaffer.storage;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.meta.BookMeta;

public class JobBookMeta {

    @Getter
    @Setter
    private String author;
    @Getter
    @Setter
    private List<String> pages;
    @Getter
    @Setter
    private String title;

    public JobBookMeta(BookMeta meta) {
        this.author = meta.getAuthor();
        this.pages = meta.getPages();
        this.title = meta.getTitle();
    }

    public JobBookMeta() {

    }
}
