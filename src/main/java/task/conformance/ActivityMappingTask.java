package task.conformance;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.classification.XEventClass;
import org.processmining.plugins.DeclareConformance.ReplayableActivityDefinition;
import org.processmining.plugins.dataawaredeclarereplayer.Runner;

import javafx.concurrent.Task;

public class ActivityMappingTask extends Task<ActivityMappingResult> {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private File xmlModel;
	private File logFile;

	private List<String> allActivities = new ArrayList<>(); //logEvents old RuM
	private List<String> matchedActivities = new ArrayList<>(); //matched old RuM
	private List<String> unmatchedActivities = new ArrayList<>(); //unmatched old RuM

	private long taskStartTime;

	public ActivityMappingTask() {
		super();
	}

	public ActivityMappingTask(File xmlModel, File logFile) {
		super();
		this.xmlModel = xmlModel;
		this.logFile = logFile;
	}

	public void setXmlModel(File xmlModel) {
		this.xmlModel = xmlModel;
	}

	public void setLogFile(File logFile) {
		this.logFile = logFile;
	}

	public void setTaskStartTime(long taskStartTime) {
		this.taskStartTime = taskStartTime;
	}

	@Override
	protected ActivityMappingResult call() throws Exception {
		try {
			this.taskStartTime = System.currentTimeMillis();
			logger.info("{} ({}) started at: {}", this.getClass().getSimpleName(), this.hashCode(), taskStartTime);

			ActivityMappingResult activityMappingResult = new ActivityMappingResult();

			Map<ReplayableActivityDefinition,XEventClass> activityMapping = Runner.getMapping(logFile.getAbsolutePath(), xmlModel.getAbsolutePath());
			fillActivityCategoryLists(activityMapping);
			activityMappingResult.setActivityMapping(activityMapping);
			activityMappingResult.setAllActivities(allActivities);
			activityMappingResult.setMatchedActivities(matchedActivities);
			activityMappingResult.setUnmatchedActivities(unmatchedActivities);

			logger.info("{} ({}) completed at: {} - total time: {}",
				this.getClass().getSimpleName(),
				this.hashCode(),
				System.currentTimeMillis(),
				(System.currentTimeMillis() - taskStartTime)
			);

			return activityMappingResult;

		} catch (Exception e) {
			logger.error("{} ({}) failed", this.getClass().getSimpleName(), this.hashCode(), e);
			throw e;
		}
	}

	private void fillActivityCategoryLists(Map<ReplayableActivityDefinition,XEventClass> activityMapping) {
		activityMapping.forEach((k,v) -> {
			if (!k.getLabel().equals("TICK") && v != null) {
				allActivities.add(k.getLabel());
				matchedActivities.add(k.getLabel());
			}
			else if (k.getLabel().equals("TICK") && v != null) {
				String logE = v.getId();
				int lp = logE.lastIndexOf('+');
				if(lp != -1) {
					String leftPlus = logE.substring(0, lp);
					String rightPlus = logE.substring(lp+1);
					allActivities.add(leftPlus+"-"+rightPlus);
				} else {
					allActivities.add(v.getId());
				}
			} else if (v == null) {
				unmatchedActivities.add(k.getLabel());
				allActivities.add(k.getLabel());
			}
		});
		Collections.sort(allActivities);
		Collections.sort(matchedActivities);
	}

	@Override
	public String toString() {
		return "ActivityMappingTask [xmlModel=" + xmlModel + ", logFile=" + logFile + "]";
	}
}
