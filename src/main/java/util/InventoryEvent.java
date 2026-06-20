package util;

import global.InventorySavedElement;
import javafx.event.Event;
import javafx.event.EventType;

public class InventoryEvent extends Event{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4123188440357460323L;
	
	public static final EventType<InventoryEvent> INVENTORY_EVENT_TYPE = new EventType<>(Event.ANY, "INVENTORY_EVENT");
	
	private PageType pageType;
	private InventorySavedElement savedElement;

	public InventoryEvent(PageType pageType, InventorySavedElement savedElement) {
		super(INVENTORY_EVENT_TYPE);
		
		this.pageType = pageType;
		this.savedElement = savedElement;
	}

	public PageType getPageType() {
		return pageType;
	}

	public InventorySavedElement getSavedElement() {
		return savedElement;
	}

}
