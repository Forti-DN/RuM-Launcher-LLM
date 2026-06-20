package task.discovery.mp_enhancer;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.model.XLog;

import controller.discovery.DataConditionType;
import controller.discovery.data.DiscoveredConstraint;

public class MpEnhancer {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private DataConditionType conditionType;
	private double minSupport;

	public MpEnhancer() {
		super();
	}

	public void setConditionType(DataConditionType conditionType) {
		this.conditionType = conditionType;
	}
	
	public void setMinSupport(double minSupport) {
		this.minSupport = minSupport;
	}

	public void performMPDiscovery(XLog xlog, List<DiscoveredConstraint> discoveredConstraints, boolean vacuityAsViolation, boolean considerLifecycle) throws Exception {
		try {
			long taskStartTime = System.currentTimeMillis();
			logger.info("{} ({}) started at: {}", this.getClass().getSimpleName(), this.hashCode(), taskStartTime);
			
			// Adds data conditions to the DiscoveredConstraint objects
			CorrelationMiner.findCorrelations(xlog, discoveredConstraints, conditionType, minSupport, vacuityAsViolation, considerLifecycle);

			logger.info("{} ({}) completed at: {} - total time: {}",
				this.getClass().getSimpleName(),
				this.hashCode(),
				System.currentTimeMillis(),
				(System.currentTimeMillis() - taskStartTime)
			);
		
		} catch (Exception e) {
			logger.error("{} ({}) failed", this.getClass().getSimpleName(), this.hashCode(), e);
			throw e;
		}
	}
}
