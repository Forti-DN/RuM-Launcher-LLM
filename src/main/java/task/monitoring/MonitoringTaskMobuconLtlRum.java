package task.monitoring;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import controller.common.eventcell.EventData;
import javafx.concurrent.Task;
import rum.algorithms.mobuconltl.MoBuConLtlMonitorLocal;
import util.LogUtils;

public class MonitoringTaskMobuconLtlRum extends Task<MonitoringTaskResult> {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private File declModel;
	private File logFile;
	private boolean conflictCheck;

	private XConceptExtension xce = XConceptExtension.instance();

	private MonitoringTaskResult monitoringTaskResult;
	private BlockingDeque<String> traceNames;

	public MonitoringTaskMobuconLtlRum(BlockingDeque<String> traceNames) {
		super();
		this.traceNames = traceNames;
	}

	public void setDeclModel(File declModel) {
		this.declModel = declModel;
	}

	public void setLogFile(File logFile) {
		this.logFile = logFile;
	}

	public void setConflictCheck(boolean conflictCheck) {
		this.conflictCheck = conflictCheck;
	}

	@Override
	protected MonitoringTaskResult call() throws Exception {
		try {
			long taskStartTime = System.currentTimeMillis();
			logger.info("{} ({}) started at: {}", this.getClass().getSimpleName(), this.hashCode(), taskStartTime);

			MoBuConLtlMonitorLocal monitor = new MoBuConLtlMonitorLocal(conflictCheck);
			monitor.setModel(declModel);

			XLog xlog = LogUtils.convertToXlog(logFile);

			// Lists for gathering the results
			List<String> traceNamesList = xlog.stream().map(trace -> xce.extractName(trace)).collect(Collectors.toList());
			traceNames.addAll(traceNamesList);
			List<List<EventData>> traceEventsData = new ArrayList<>(Collections.nCopies(traceNames.size(), (List<EventData>) null));

			// Process the log one event at a time
			while (!traceNames.isEmpty()) {
				String traceName = traceNames.take();
				XTrace xtrace = xlog.stream().filter(trace -> traceName.equals(xce.extractName(trace))).findFirst().get();
				List<EventData> eventDataList = LogUtils.createEventDataList(xtrace, null, false, false);
				
				if (isCancelled()) break;

				for (int i=0; i < xtrace.size()-1; i++) {
					XEvent xevent = xtrace.get(i);
					Map<String, String> constraintStates = monitor.processNextEvent(xevent, false);
					eventDataList.get(i).setConstraintStates(constraintStates);
				}
				XEvent xevent = xtrace.get(xtrace.size()-1);
				Map<String, String> constraintStates = monitor.processNextEvent(xevent, true);
				eventDataList.get(xtrace.size()-1).setConstraintStates(constraintStates);
				
				int index = traceNamesList.indexOf( xce.extractName(xtrace) );
				traceEventsData.set(index, eventDataList);
				
				// Updating the result object
				monitoringTaskResult = new MonitoringTaskResult();
				monitoringTaskResult.setXlog(xlog);
				monitoringTaskResult.setTraceNames(traceNamesList);
				monitoringTaskResult.setTraceEventsData(traceEventsData);
				updateValue(monitoringTaskResult);
			}

			if (!isCancelled()) {
				logger.info("{} ({}) completed at: {} - total time: {}",
					this.getClass().getSimpleName(),
					this.hashCode(),
					System.currentTimeMillis(),
					(System.currentTimeMillis() - taskStartTime)
				);

			} else {
				logger.info("{} ({}) canceled at: {} - total time: {}",
					this.getClass().getSimpleName(),
					this.hashCode(),
					System.currentTimeMillis(),
					(System.currentTimeMillis() - taskStartTime)
				);
			}

			return monitoringTaskResult;

		} catch (InterruptedException e) {
			if (!isCancelled()) throw e;
			return monitoringTaskResult;

		} catch (Exception e) {
			logger.error("{} ({}) failed", this.getClass().getSimpleName(), this.hashCode(), e);
			throw e;
		}

	}

}
