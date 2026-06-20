package util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public class EventFinder {
	
	public static Optional<List<String>> getEventAttributes(XLog traces, String traceName, int pos) {
		if(traces == null) return Optional.empty();
		else {
			for(XTrace xt: traces) {
				XAttribute xa = xt.getAttributes().get("concept:name");
				if(xa != null && xa.toString().equals(traceName)) {
					if(pos >= xt.size()) {
						return Optional.empty();
					}
					XEvent xe = xt.get(pos);
					List<String> attributes = new ArrayList<String>();
					xe.getAttributes().forEach((k,v) -> {
						String str = k.toString()+": "+v.toString();
						attributes.add(str);
					});
					return Optional.of(attributes);
				}
			}
			return Optional.empty();
		}
	}

}
