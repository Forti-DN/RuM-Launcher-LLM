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

import RuntimeVerification.RVFalse;
import RuntimeVerification.RVTempFalse;
import RuntimeVerification.RVTempTrue;
import RuntimeVerification.RVTrue;
import RuntimeVerification.RVTruthValue;
import controller.common.eventcell.EventData;
import javafx.concurrent.Task;
import metaconstraints.Compensation;
import metaconstraints.ContexAbsence;
import metaconstraints.DeclareNames;
import metaconstraints.MetaConFormula;
import metaconstraints.ReactiveCompensation;
import rum.algorithms.mobuconldl.MoBuConLdlMonitor;
import rum.algorithms.mobuconldl.MonitoringState;
import rum.algorithms.mobuconldl.model.DeclareConstraint;
import rum.algorithms.mobuconldl.model.DeclareTemplate;
import rum.algorithms.mobuconldl.utils.ModelUtils;
import treedata.TreeDataMetaconstraint;
import util.LogUtils;

public class MonitoringTaskMobuconLdlRum extends Task<MonitoringTaskResult> {
	
	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private File declModel;
	private List<TreeDataMetaconstraint> treeDatametaconstraints;
	private File logFile;
	private boolean conflictCheck;

	private XConceptExtension xce = XConceptExtension.instance();

	private MonitoringTaskResult monitoringTaskResult;
	private BlockingDeque<String> traceNames;
	
	public MonitoringTaskMobuconLdlRum(BlockingDeque<String> traceNames) {
		super();
		this.traceNames = traceNames;
	}
	
	public void setDeclModel(File declModel) {
		this.declModel = declModel;
	}
	
	public void setMetaconstraints(List<TreeDataMetaconstraint> treeDatametaconstraints) {
		this.treeDatametaconstraints = treeDatametaconstraints;
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
		
			MoBuConLdlMonitor monitor = new MoBuConLdlMonitor(conflictCheck);
			
			List<MetaConFormula> metaconstraints = new ArrayList<MetaConFormula>();
			
			for (TreeDataMetaconstraint treeDataMetaconstraint : treeDatametaconstraints) {
				DeclareConstraint firstConstraint = ModelUtils.readConstraintString(treeDataMetaconstraint.getFirstConstraint());
				if (firstConstraint.getTemplate() == null) {
					logger.error("Skipping metaconstraint with unsupported template: {}", firstConstraint.getTemplate());
					continue;
				}
				DeclareConstraint secondConstraint;
				
				switch(treeDataMetaconstraint.getMetaconstraintTemplate()) {
				case CONTEXTUAL_ABSENCE:
					ContexAbsence contexAbsence = new ContexAbsence(getDeclareName(firstConstraint.getTemplate()), firstConstraint.getActivationActivity(), firstConstraint.getTargetActivity(), getRVTruthValue(treeDataMetaconstraint.getConstraintStatus()), treeDataMetaconstraint.getActivity());
					contexAbsence.setMetaconDescription(treeDataMetaconstraint.getMetaconstraintText());
					metaconstraints.add(contexAbsence);
					break;
				case REACTIVE_COMPENSATION:
					secondConstraint = ModelUtils.readConstraintString(treeDataMetaconstraint.getSecondConstraint());
					if (secondConstraint.getTemplate() == null) {
						logger.error("Skipping metaconstraint with unsupported template: {}", secondConstraint.getTemplate());
						continue;
					}
					ReactiveCompensation reactiveCompensation = new ReactiveCompensation(getDeclareName(firstConstraint.getTemplate()), firstConstraint.getActivationActivity(), firstConstraint.getTargetActivity(), getDeclareName(secondConstraint.getTemplate()), secondConstraint.getActivationActivity(), secondConstraint.getTargetActivity());
					reactiveCompensation.setMetaconDescription(treeDataMetaconstraint.getMetaconstraintText());
					metaconstraints.add(reactiveCompensation);
					break;
				case COMPENSATION:
					secondConstraint = ModelUtils.readConstraintString(treeDataMetaconstraint.getSecondConstraint());
					if (secondConstraint.getTemplate() == null) {
						logger.error("Skipping metaconstraint with unsupported template: {}", secondConstraint.getTemplate());
						continue;
					}
					Compensation compensation = new Compensation(getDeclareName(firstConstraint.getTemplate()), firstConstraint.getActivationActivity(), firstConstraint.getTargetActivity(), getDeclareName(secondConstraint.getTemplate()), secondConstraint.getActivationActivity(), secondConstraint.getTargetActivity());
					compensation.setMetaconDescription(treeDataMetaconstraint.getMetaconstraintText());
					metaconstraints.add(compensation);
					break;
				default:
					logger.error("Skipping unsupported metaconstraint template: {}", treeDataMetaconstraint.getMetaconstraintTemplate());
					break;
				}
			}
			
			
			//metaconstraints.add(new ContexAbsence(DeclareNames.RESPONDED_EXISTENCE, "pay", "acc", new RVTempFalse(), "get")); //Matches submission draft
			//metaconstraints.add(new ReactiveCompensation(DeclareNames.NOT_COEXISTENCE, "cancel", "get", DeclareNames.INITIAL, "complete", null)); //Matches submission draft
			//metaconstraints.add(new Compensation(DeclareNames.NOT_COEXISTENCE, "cancel", "get", DeclareNames.RESPONSE, "pay", "get")); //Does not match submission draft - using same parapeters as used in draft for conflict and preference
			
			monitor.setModel(declModel, metaconstraints);
			
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
	
	private DeclareNames getDeclareName(DeclareTemplate template) {
		switch (template) {
		case Absence:
			return DeclareNames.ABSENCE;
		case Absence2:
			return DeclareNames.ABSENCE2;
		case Existence:
			return DeclareNames.EXISTENCE;
		case Response:
			return DeclareNames.RESPONSE;
		case Responded_Existence:
			return DeclareNames.RESPONDED_EXISTENCE;
		case Precedence:
			return DeclareNames.PRECEDENCE;
		case Alternate_Response:
			return DeclareNames.ALTERNATING_RESPONSE;
		case Alternate_Precedence:
			return DeclareNames.ALTERNATING_PRECEDENCE;
		case Chain_Response:
			return DeclareNames.CHAIN_RESPONSE;
		case Chain_Precedence:
			return DeclareNames.CHAIN_PRECEDENCE;
		case Not_CoExistence:
			return DeclareNames.NOT_COEXISTENCE;
		case Not_Succession:
			return DeclareNames.NEGATED_SUCCESSION;
		case Not_Chain_Succession:
			return DeclareNames.NEGATED_CHAIN_SUCCESSION;
		case Init:
			return DeclareNames.INITIAL;
		case End:
			return DeclareNames.FINAL;
		
		//The following templates are not supported for MoBuConLDL
		case Alternate_Succession:
			return null;
		case Chain_Succession:
			return null;
		case Choice:
			return null;
		case CoExistence:
			return null;
		case Exactly1:
			return null;
		case Exactly2:
			return null;
		case Exclusive_Choice:
			return null;
		case Existence2:
			return null;
		case Existence3:
			return null;
		case Not_Chain_Precedence:
			return null;
		case Not_Chain_Response:
			return null;
		case Not_Precedence:
			return null;
		case Not_Responded_Existence:
			return null;
		case Not_Response:
			return null;
		case Succession:
			return null;
		case Absence3:
			return null;
		default:
			return null;
		}
	}
	
	private RVTruthValue getRVTruthValue(MonitoringState constraintStatus) {
		switch (constraintStatus) {
		case INIT:
			logger.error("{} can not be used for defining metaconstraints", constraintStatus);
			return null;
		case POSS_SAT:
			return new RVTempTrue();
		case POSS_VIOL:
			return new RVTempFalse();
		case SAT:
			return new RVTrue();
		case VIOL:
			return new RVFalse();
		default:
			logger.error("Skipping unsupported monitoring state: {}", constraintStatus);
			return null;
		}
	}
}
