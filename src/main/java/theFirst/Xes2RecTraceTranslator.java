package theFirst;


import it.unibo.ai.rec.common.TimeGranularity;
import it.unibo.ai.rec.model.RecTrace;


import java.util.Iterator;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;

public class Xes2RecTraceTranslator {

    public static enum TimestampStrategy {
        ABSOLUTE, RELATIVE, QUALITATIVE;
    }

    private TimeGranularity granularity;
    private TimestampStrategy strategy;
    private int sequenceNumber;
    private long baseTimestamp;
    private final boolean contemporaryEventsAllowed;
    private static XConceptExtension conceptExtension = XConceptExtension.instance();
    private static XTimeExtension timeExtension = XTimeExtension.instance();

    public Xes2RecTraceTranslator(TimeGranularity granularity, TimestampStrategy strategy,
                                  boolean contemporaryEventsAllowed) {
        this.granularity = granularity;
        this.strategy = strategy;
        this.contemporaryEventsAllowed = contemporaryEventsAllowed;

    }

    public Xes2RecTraceTranslator(TimeGranularity granularity, TimestampStrategy strategy) {
        this(granularity, strategy, true);
    }

    public TimeGranularity getGranularity() {
        return granularity;
    }

    public void setGranularity(TimeGranularity granularity) {
        this.granularity = granularity;
    }

    public TimestampStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(TimestampStrategy strategy) {
        this.strategy = strategy;
    }

    public RecTrace translate(XTrace trace) throws Exception {
        RecTrace recTrace = new RecTrace(true);
        Iterator<XEvent> iterator = trace.iterator();
        sequenceNumber = 0;
        boolean first = true;
        long oldTimestamp = -1;
        while (iterator.hasNext()) {
            sequenceNumber++;
            XEvent ate = iterator.next();
            long ateTimestamp = timeExtension.extractTimestamp(ate).getTime();
            if (first) {
                switch (strategy) {
                    case ABSOLUTE:
                        baseTimestamp = 0;
                        break;
                    case RELATIVE:
                        baseTimestamp = ateTimestamp;
                        break;
                    case QUALITATIVE:
                        break;
                }
                first = false;
            }
            long timestamp = translateTime(ateTimestamp);
            if ((timestamp == oldTimestamp) && !contemporaryEventsAllowed) {
                throw new Exception("Wrong granularity: two XExtendedEvents at the same time (" + timestamp + ","
                        + sequenceNumber + ")");
            }
            recTrace.addHappenedEvent(translateEvent(ate), timestamp);
            oldTimestamp = timestamp;
        }
        return recTrace;
    }

    protected String translateEvent(XEvent e) {
        //basic implementation, no event type, no originator, no data!
        XAttributeMap a = e.getAttributes();
        String data = "with data ";
        for (String key : a.keySet()) {
            if (key.equals("concept:name") || key.equals("time:timestamp") || key.equals("lifecycle:transition") || key.equals("stream:lifecycle:trace-transition")) {
                continue;
            }
            data = data + ", " + key + ": " + a.get(key);
        }
        return "exec(" + PrologUtils.convertToTerm(conceptExtension.extractName(e)) + " " + data + ")";
    }

    private long translateTime(long timestamp) {
        if (strategy.equals(TimestampStrategy.QUALITATIVE)) {
            return sequenceNumber;
        } else {
            return (timestamp - baseTimestamp) / granularity.getFactor();
        }
    }
}