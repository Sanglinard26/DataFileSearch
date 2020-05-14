/*
 * Creation : 15 mars 2018
 */
package aif;

import java.util.ArrayList;
import java.util.List;

public final class Traceur {

    private final List<Item> listItems;

    public Traceur() {
        this.listItems = new ArrayList<Item>();
    }
    
    public final void addItem(Item item)
    {
    	if(!this.listItems.contains(item))
    	{
    		this.listItems.add(item);
    	}
    }
    
    public final List<Item> getListItems() {
		return listItems;
	}
}
