package task.discovery;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.model.XLog;
import org.processmining.plugins.declareminer.DeclareMiner;
import org.processmining.plugins.declareminer.DeclareMinerInput;
import org.processmining.plugins.declareminer.DeclareMinerNoHierarc;
import org.processmining.plugins.declareminer.DeclareMinerNoRed;
import org.processmining.plugins.declareminer.DeclareMinerNoTrans;
import org.processmining.plugins.declareminer.enumtypes.AprioriKnowledgeBasedCriteria;
import org.processmining.plugins.declareminer.enumtypes.DeclarePerspective;
import org.processmining.plugins.declareminer.enumtypes.DeclareTemplate;
import org.processmining.plugins.declareminer.enumtypes.MapTemplateConfiguration;
import org.processmining.plugins.declareminer.util.Configuration;
import org.processmining.plugins.declareminer.util.UnifiedLogger;
import org.processmining.plugins.declareminer.visualizing.DeclareMinerOutput;

import controller.discovery.DeclarePruningType;
import controller.discovery.data.DiscoveredActivity;
import controller.discovery.data.DiscoveredConstraint;
import javafx.concurrent.Task;
import minerful.concept.ProcessModel;
import minerful.concept.constraint.ConstraintsBag;
import task.discovery.mp_enhancer.MpEnhancer;
import util.ConstraintTemplate;
import util.ConstraintUtils;
import util.LogUtils;
import util.TemplateUtils;

public class DiscoveryTaskDeclare extends Task<DiscoveryTaskResult> {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private File logFile;
	private int minSupport;
	private boolean vacuityAsViolation;
	private boolean considerLifecycle = true;
	private DeclarePruningType pruningType;
	private List<ConstraintTemplate> selectedTemplates;
	private MpEnhancer mpEnhancer;
	private boolean computeTimeDistances = false;

	public DiscoveryTaskDeclare() {
		super();
	}

	public void setLogFile(File logFile) {
		this.logFile = logFile;
	}

	public void setMinSupport(int minSupport) {
		this.minSupport = minSupport;
	}

	public void setVacuityAsViolation(boolean vacuityAsViolation) {
		this.vacuityAsViolation = vacuityAsViolation;
	}
	
	public void setConsiderLifecycle(boolean considerLifecycle) {
		this.considerLifecycle = considerLifecycle;
	}

	public void setPruningType(DeclarePruningType pruningType) {
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


	@Override
	protected DiscoveryTaskResult call() throws Exception {
		try {
			long taskStartTime = System.currentTimeMillis();
			logger.info("{} ({}) started at: {}", this.getClass().getSimpleName(), this.hashCode(), taskStartTime);

			//Prepare configuration for discovery
			Configuration configuration = new Configuration();
			configuration.log = LogUtils.convertToXlog(logFile);

			DeclareMinerInput input = new DeclareMinerInput();
			input.setMinSupport(minSupport);
			input.setAlpha(vacuityAsViolation ? 0 : 100);

			HashSet<DeclareTemplate> selectedDeclareTemplates = new HashSet<>();
			selectedTemplates.forEach(item -> selectedDeclareTemplates.add(TemplateUtils.getDeclareTemplate(item)));
			input.setSelectedDeclareTemplateSet(selectedDeclareTemplates);

			Set<DeclarePerspective> persp_set = Collections.singleton(DeclarePerspective.Control_Flow);
			input.setDeclarePerspectiveSet(persp_set);

			Map<String, DeclareTemplate> templateNameStringDeclareTemplateMap = new HashMap<>(); //This seems to be needed for some kind of internal mapping in Declare Miner (template name -> condec name)?
			for (ConstraintTemplate constraintTemplate : selectedTemplates) {
				DeclareTemplate declareTemplate = TemplateUtils.getDeclareTemplate(constraintTemplate);
				
				String templateNameString;
				if (declareTemplate == DeclareTemplate.Choice)
					templateNameString = "choice 1 of 2";
				else
					templateNameString = declareTemplate.name().replaceAll("_", " ").toLowerCase();
					
				templateNameStringDeclareTemplateMap.put(templateNameString, declareTemplate);
			}
			
			input.setDeclareTemplateConstraintTemplateMap(DeclareMiner.readConstraintTemplates(templateNameStringDeclareTemplateMap));
			input.setMapTemplateConfiguration(MapTemplateConfiguration.valueOf("DiscoverProvidedTemplatesAcrossAllActivitesInLog"));
			
			Set<AprioriKnowledgeBasedCriteria> apriori_set;
			if (considerLifecycle)
				apriori_set = new HashSet<>(Collections.singleton(AprioriKnowledgeBasedCriteria.valueOf("AllActivitiesWithEventTypes")));
			else
				apriori_set = new HashSet<>(Collections.singleton(AprioriKnowledgeBasedCriteria.valueOf("AllActivitiesIgnoringEventTypes")));

			input.setAprioriKnowledgeBasedCriteriaSet(apriori_set);
			input.setVerbose(false);
			input.setThreadNumber(4);
						
			configuration.input = input;			
			
			UnifiedLogger.unified_log_path = "./output/all_results.log";
			UnifiedLogger.unified_memory_log_path = "./output/mem.log";
			configuration.setUnifiedLoggerPrunerType("replayers"); // will be obsolete after testing is done

			//Run model discovery
			DeclareMinerOutput declareMinerOutput;
			switch (pruningType) {
			case ALL_REDUCTIONS:
				declareMinerOutput = DeclareMiner.mineDeclareConstraints(null, configuration.log, configuration.input);
				break;
			case HIERARCHY_BASED:
				declareMinerOutput = DeclareMinerNoTrans.mineDeclareConstraints(null, configuration.log, configuration.input);
				break;
			case TRANSITIVE_CLOSURE:
				declareMinerOutput = DeclareMinerNoHierarc.mineDeclareConstraints(null, configuration.log, configuration.input);
				break;
			case NONE:
				declareMinerOutput = DeclareMinerNoRed.mineDeclareConstraints(null, configuration.log, configuration.input);
				break;
			default:
				throw new Exception("Unhandled pruning type: " + pruningType);
			}
			
			
			// Building a map for an easy identification in constraints discovery
			XLog log = LogUtils.convertToXlog(logFile);
			Map<String, DiscoveredActivity> activityMap = new HashMap<>();
			for (DiscoveredActivity act : ConstraintUtils.getAllActivitiesFromLog(log, considerLifecycle) ) {
				String identifier = act.getActivityFullName();
				activityMap.put(identifier, act);
			}
			
			List<DiscoveredConstraint> discoveredConstraints = new ArrayList<>();
			
			declareMinerOutput.getVisiblesupportRule().forEach((key, constraintSupport) -> {
				ConstraintTemplate template = TemplateUtils.getConstraintTemplate(declareMinerOutput.getTemplate().get(key));
				List<String> parametersList = declareMinerOutput.getVisibleConstraintParametersMap().get(key);
				
				DiscoveredActivity activationActivity = activityMap.get(parametersList.get(0));
				DiscoveredActivity targetActivity = null;
				
				if (template.getIsBinary()) {
					if (template.getReverseActivationTarget()) {
						activationActivity = activityMap.get(parametersList.get(1));
						targetActivity = activityMap.get(parametersList.get(0));
					} else {
						targetActivity = activityMap.get(parametersList.get(1));
					}
				}
				
				discoveredConstraints.add(new DiscoveredConstraint(template, activationActivity, targetActivity));
			});
			
						
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
			
			// Sorting activities based on support
			List<DiscoveredActivity> sortedActivities = activityMap.values().stream().sorted((i1,i2) -> {
				double s1 = i1.getActivitySupport();
				double s2 = i2.getActivitySupport();
				if(s2 > s1) return 1;
				else if(s1 > s2) return -1;
				else return 0;
			}).collect(Collectors.toList());
			
			// Process model discovery results
			DiscoveryTaskResult discoveryTaskResult = new DiscoveryTaskResult();
			discoveryTaskResult.setActivities(sortedActivities);
			discoveryTaskResult.setConstraints(sortedConstraints);
			
			/* TODO: Uncomment this part when MINERFUL will support Choice and Exclusive Choice
			// This is done because the algorithm translating from declare model to minerful one reads the support from the getText method
			for (ConstraintDefinition cd : declareMinerOutput.getModel().getModel().getConstraintDefinitions()) {
				// Setting also the correct name for minerful choice template translation
				if (cd.getName().equals("choice 1 of 2"))
					cd.setName("Choice");
				
				int index = declareMinerOutput.getAllDiscoveredConstraints().indexOf(cd) + 1;
				double support = declareMinerOutput.getSupportRule().get(index).doubleValue();
				
				cd.setText("support;" + support);
			}
			
			ProcessModel discoveryModel = new ProcessModelLoader().loadProcessModel( declareMinerOutput.getModel().getModel() ); 
			
			// This is done because the translation algorithm doesn't set the constraintWhichThisShouldBeBasedUpon variable,
			// causing a mismatch between the declare model and the relative translated minerful model
			for (Constraint c : discoveryModel.bag.getAllConstraints())
				if (!c.getTemplateName().equals("Exactly1") && !c.getTemplateName().equals("Exactly2")) // TODO: Remove this line when Minerful discovery will support Exactly1 and Exactly2
					if (c.suggestConstraintWhichThisShouldBeBasedUpon() != null)
						c.setConstraintWhichThisIsBasedUpon(c.suggestConstraintWhichThisShouldBeBasedUpon());
			*/
			ProcessModel discoveryModel = new ProcessModel(new ConstraintsBag());
			discoveryTaskResult.setDiscoveryModel(discoveryModel);
			
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
