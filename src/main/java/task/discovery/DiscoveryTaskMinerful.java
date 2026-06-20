package task.discovery;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.model.XLog;

import controller.discovery.data.DiscoveredActivity;
import controller.discovery.data.DiscoveredConstraint;
import javafx.concurrent.Task;
import minerful.MinerFulMinerLauncher;
import minerful.MinerFulSimplificationLauncher;
import minerful.concept.ProcessModel;
import minerful.concept.constraint.Constraint;
import minerful.miner.params.MinerFulCmdParameters;
import minerful.params.InputLogCmdParameters;
import minerful.params.SystemCmdParameters;
import minerful.postprocessing.params.PostProcessingCmdParameters;
import minerful.postprocessing.params.PostProcessingCmdParameters.PostProcessingAnalysisType;
import minerful.postprocessing.pruning.ThresholdsMarker;
import task.discovery.mp_enhancer.MpEnhancer;
import util.ConstraintTemplate;
import util.ConstraintUtils;
import util.LogUtils;
import util.TemplateUtils;

public class DiscoveryTaskMinerful extends Task<DiscoveryTaskResult> {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private File logFile;
	private double minSupport;
	private boolean vacuityAsViolation = true;
	private PostProcessingAnalysisType pruningType;
	private boolean considerLifecycle = false;
	private List<ConstraintTemplate> selectedTemplates;
	private MpEnhancer mpEnhancer;
	private boolean computeTimeDistances = false;

	public DiscoveryTaskMinerful() {
		super();
	}

	public void setLogFile(File logFile) {
		this.logFile = logFile;
	}
	
	public File getLogFile() {
		return logFile;
	}

	public void setMinSupport(double minSupport) {
		this.minSupport = minSupport;
	}

	public void setPruningType(PostProcessingAnalysisType pruningType) {
		this.pruningType = pruningType;
	}

	public void setSelectedTemplates(List<ConstraintTemplate> selectedTemplates) {
		this.selectedTemplates = selectedTemplates;
	}

	public void setMpEnhancer(MpEnhancer mpEnhancer) {
		this.mpEnhancer = mpEnhancer;
	}

	public void setComuputeTimeDistances(boolean computeTimeDistances) {
		this.computeTimeDistances = computeTimeDistances;
	}
	
	public boolean getComputeTimeDistances() {
		return computeTimeDistances;
	}
	
	public boolean getConsiderLifecycle() {
		return considerLifecycle;
	}
	
	@Override
	protected DiscoveryTaskResult call() throws Exception {
		try {
			long taskStartTime = System.currentTimeMillis();
			logger.info("{} ({}) started at: {}", this.getClass().getSimpleName(), this.hashCode(), taskStartTime);

			//Prepare configuration for discovery
			InputLogCmdParameters inputParams = new InputLogCmdParameters();
			inputParams.inputLogFile = logFile;
			
			MinerFulCmdParameters minerFulParams = new MinerFulCmdParameters();
			SystemCmdParameters systemParams = new SystemCmdParameters();
			
			// This also sets pruning type to None
			PostProcessingCmdParameters postParams = PostProcessingCmdParameters.makeParametersForNoPostProcessing();
			postParams.supportThreshold = minSupport;
			
			MinerFulMinerLauncher miFuMiLa = new MinerFulMinerLauncher(inputParams, minerFulParams, postParams, systemParams);
			ProcessModel pmForActSupp = miFuMiLa.mine();
			
			// Minerful discovery doesn't mark constraints below support threshold from the model if the pruning type is set to None, so it is done here
			ThresholdsMarker mrkr = new ThresholdsMarker(pmForActSupp.bag);
			pmForActSupp.bag = mrkr.markConstraintsBelowSupportThreshold(minSupport);
			
			// Setting redundancy of unused constraints
			for (Constraint constraint : pmForActSupp.getAllConstraints()) {
				// Skipping AtMost3 as it is not currently used in RuM
				if (constraint.getTemplateName().equals("AtMost3")) { 
					constraint.setRedundant(true);
					continue;
				}
				
				ConstraintTemplate template = TemplateUtils.getConstraintTemplateFromMinerful(constraint.getTemplateName());
				if (!selectedTemplates.contains(template))
					constraint.setRedundant(true);
			}
			
			// Removing marked constraints because they are below the support threshold or they are not selected/supported
			pmForActSupp.bag.removeMarkedConstraints();
			
			// Setting to zero confidence and interest factor to avoid a mismatch between minerful and declare models in the pruning section
			pmForActSupp.bag.getAllConstraints().forEach(constraint -> {
				constraint.setConfidence(Constraint.DEFAULT_CONFIDENCE);
				constraint.setInterestFactor(Constraint.DEFAULT_INTEREST_FACTOR);
			});

			// After useless constraints removal, the pruning can be executed
			postParams.postProcessingAnalysisType = pruningType;
			postParams.cropRedundantAndInconsistentConstraints = true;
			MinerFulSimplificationLauncher miFuSiLa = new MinerFulSimplificationLauncher(pmForActSupp, postParams);
			ProcessModel processModel = miFuSiLa.simplify();
			
			// Sorting activities based on support
			XLog log = LogUtils.convertToXlog(logFile);
			Set<DiscoveredActivity> allActivities = ConstraintUtils.getAllActivitiesFromLog(log, considerLifecycle);
			List<DiscoveredActivity> sortedActivities = allActivities.stream().sorted((i1,i2) -> {
				double s1 = i1.getActivitySupport();
				double s2 = i2.getActivitySupport();
				if(s2 > s1) return 1;
				else if(s1 > s2) return -1;
				else return 0;
			}).collect(Collectors.toList());
			
			List<DiscoveredConstraint> discoveredConstraints = ConstraintUtils.extractConstraintsFromMinerfulModel(log, processModel, sortedActivities);
			
			if (mpEnhancer != null) {
				logger.info("{} ({}) running MpEnhancer ({})", this.getClass().getSimpleName(), this.hashCode(), mpEnhancer.hashCode());
				mpEnhancer.performMPDiscovery(log, discoveredConstraints, vacuityAsViolation, considerLifecycle);
			
			} else {
				// Need to compute support only when MP-Discovery isn't performed
				for (DiscoveredConstraint c : discoveredConstraints)
					c.setConstraintSupport((float) ConstraintUtils.computeTraceBasedSupport(log, c, vacuityAsViolation));
			}
			
			// Sorting constraints based on support
			List<DiscoveredConstraint> sortedConstraints = discoveredConstraints.stream().sorted((i1,i2) -> {
				double s1 = i1.getConstraintSupport();
				double s2 = i2.getConstraintSupport();
				if(s2 > s1) return 1;
				else if(s1 > s2) return -1;
				else return 0;
			}).collect(Collectors.toList());
			
			if (computeTimeDistances) {
				ConstraintUtils.initializeTDVariables();
				
				for (DiscoveredConstraint c : sortedConstraints) {
					ConstraintUtils.setConstraintTDs(log, c);
				}
			}
			
			DiscoveryTaskResult discoveryTaskResult = new DiscoveryTaskResult();
			discoveryTaskResult.setActivities(sortedActivities);
			discoveryTaskResult.setConstraints(sortedConstraints);
			discoveryTaskResult.setDiscoveryModel(processModel);

			logger.info("{} ({}) completed at: {} - total time: {}",
				this.getClass().getSimpleName(),
				this.hashCode(),
				System.currentTimeMillis(),
				(System.currentTimeMillis() - taskStartTime)
			);
			
			return discoveryTaskResult;

		} catch (Exception e) {
			logger.error("{} ({}) failed", this.getClass().getSimpleName(), this.hashCode(), e);
			throw e;
		}
	}
}
