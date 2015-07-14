package edu.uci.ics.luci.utility.webserver.event;

import edu.uci.ics.luci.utility.webserver.event.result.EventResult;

public class EventVoid extends Event {

	@Override
	public void set(Event copyMe) {
	}

	@Override
	public EventResult onEvent() {
		return new EventResult();
	}

}
