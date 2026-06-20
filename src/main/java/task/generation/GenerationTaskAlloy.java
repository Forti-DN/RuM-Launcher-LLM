package task.generation;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.extension.XExtensionParser;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.deckfour.xes.model.impl.XAttributeMapImpl;
import org.deckfour.xes.model.impl.XLogImpl;

import core.AssemblyGenerationModes;
import core.Evaluator;
import javafx.concurrent.Task;
import treedata.TreeDataAttribute;
import util.FileUtils;

public class GenerationTaskAlloy extends Task<GenerationTaskResult> {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private File declModel;
	private int minTraceLength;
	private int maxTraceLength;
	private int numberOfPosTraces;
	private int percentageOfPosVacuousTraces;
	private int numberOfNegTraces;
	private int percentageOfNegVacuousTraces;
	private List<TreeDataAttribute> traceAttributes;

	private long taskStartTime;

	public GenerationTaskAlloy() {
		super();
	}

	public GenerationTaskAlloy(File declModel, int minTraceLength, int maxTraceLength, int numberOfTraces,
			int negativeTracesPrecentage, int vacuousTracesPrecentage) {
		super();
		this.declModel = declModel;
		this.minTraceLength = minTraceLength;
		this.maxTraceLength = maxTraceLength;
		this.numberOfPosTraces = numberOfTraces;
		this.numberOfNegTraces = negativeTracesPrecentage;
		this.percentageOfNegVacuousTraces = vacuousTracesPrecentage;
	}

	public void setDeclModel(File declModel) {
		this.declModel = declModel;
	}

	public void setMinTraceLength(int minTraceLength) {
		this.minTraceLength = minTraceLength;
	}

	public void setMaxTraceLength(int maxTraceLength) {
		this.maxTraceLength = maxTraceLength;
	}

	public void setNumberOfPositiveTraces(int numberOfPosTraces) {
		this.numberOfPosTraces = numberOfPosTraces;
	}
	
	public void setVacuousPositiveTracesPercentage(int percentageOfPosVacuousTraces) {
		this.percentageOfPosVacuousTraces = percentageOfPosVacuousTraces;
	}

	public void setNumberOfNegativeTraces(int numberOfNegTraces) {
		this.numberOfNegTraces = numberOfNegTraces;
	}

	public void setVacuousNegativeTracesPercentage(int percentageOfNegVacuousTraces) {
		this.percentageOfNegVacuousTraces = percentageOfNegVacuousTraces;
	}
	
	public void setTraceAttributes(List<TreeDataAttribute> traceAttributes) {
		this.traceAttributes = traceAttributes;
	}

	@Override
	protected GenerationTaskResult call() throws Exception {
		try {
			this.taskStartTime = System.currentTimeMillis();
			logger.info("{} ({}) started at: {}", this.getClass().getSimpleName(), this.hashCode(), taskStartTime);

			int nPositiveVacuousTraces = (int) Math.round(numberOfPosTraces * (percentageOfPosVacuousTraces / 100d));
			int nPositiveTraces = numberOfPosTraces - nPositiveVacuousTraces;
			
			int nNegativeVacuousTraces = (int) Math.round(numberOfNegTraces * (percentageOfNegVacuousTraces / 100d));
			int nNegativeTraces = numberOfNegTraces - nNegativeVacuousTraces;
			
			String preprocessedModel = FileUtils.preprocessModel(declModel, true);
			String declare = Files.readString(Path.of(preprocessedModel));
			
			/*
			Note that AlloyRunner.generateLog was used originally.
			AssemblyGenerationModes.getLog is used instead to get the XLog object
			Using it directly does also allow for more configuration options
			*/
			XLog positiveLog = AssemblyGenerationModes.getLog(
					minTraceLength,
					maxTraceLength,
					nPositiveVacuousTraces, 	// nPositiveVacuousTraces and nPositiveTraces have been switched,
					nPositiveTraces,			// because the results do not make sense otherwise.
					0,
					0,
					1,
					false,	// Even length distribution
					2,
					1,
					declare,
					"log_gen_temp.als",
					LocalDateTime.now(),
					Duration.ofHours(4),
					Evaluator::getLogSingleRun);
			
			XLog negativeLog = AssemblyGenerationModes.getLog(
					minTraceLength,
					maxTraceLength,
					0, 	
					0,			
					nNegativeVacuousTraces,		// nNegativeVacuousTraces and nNegativeTraces have been switched,
					nNegativeTraces,			// because the results do not make sense otherwise.
					1,
					false,	// Even length distribution
					2,
					1,
					declare,
					"log_gen_temp.als",
					LocalDateTime.now(),
					Duration.ofHours(4),
					Evaluator::getLogSingleRun);

			
			Map<String, XLog> logs = new HashMap<>();
			logs.put("positive", positiveLog);
			logs.put("negative", negativeLog);
			
			
			XLog generatedLog = new XLogImpl(new XAttributeMapImpl());
			addExtensions(generatedLog);
			
			for (Map.Entry<String, XLog> entry : logs.entrySet()) {
				for (XTrace trace : entry.getValue()) {
					XAttributeMap attributeMap = trace.getAttributes();
		
					for (TreeDataAttribute att : traceAttributes) {
						String rndValue = null;
						
						switch (att.getAttributeType()) {
						case ENUMERATION:
							int rndIndex = ThreadLocalRandom.current().nextInt(0, att.getPossibleValues().size());
							rndValue = att.getPossibleValues().get(rndIndex);
							break;
							
						case FLOAT:
							Random r = new Random();
							rndValue = String.valueOf( att.getValueFrom().floatValue() + r.nextFloat() * (att.getValueTo().floatValue() - att.getValueFrom().floatValue()) );
							break;
							
						case INTEGER:
							rndValue = String.valueOf( ThreadLocalRandom.current().nextInt(att.getValueFrom().intValue(), att.getValueTo().intValue() + 1) );
							break;
						}
						
						attributeMap.put(att.getAttributeName(), new XAttributeLiteralImpl(att.getAttributeName(), rndValue));
					}
					
					attributeMap.put("trace:type", new XAttributeLiteralImpl("trace:type", entry.getKey()));
					generatedLog.add(trace);
				}
			}
			
			Collections.reverse(generatedLog);	// Reverting traces to display at first positive ones
			
			// Preparing the result object
			GenerationTaskResult generationTaskResult = new GenerationTaskResult();
			generationTaskResult.setGeneratedLog(generatedLog);

			logger.info("{} ({}) completed at: {} - total time: {}",
				this.getClass().getSimpleName(),
				this.hashCode(),
				System.currentTimeMillis(),
				(System.currentTimeMillis() - taskStartTime)
			);
			
			return generationTaskResult;
		
		} catch (Exception e) {
			logger.error("{} ({}) failed", this.getClass().getSimpleName(), this.hashCode(), e);
			throw e;
		}
	}

	// Taken from core.AlloyRunner (AlloyGenerator) and added code from method writeTracesAsLogFile
	private void addExtensions(XLog log) {
		try {
			log.getExtensions().add(XExtensionParser.instance().parse(new URI("http://www.xes-standard.org/lifecycle.xesext")));
			log.getExtensions().add(XExtensionParser.instance().parse(new URI("http://www.xes-standard.org/org.xesext")));
			log.getExtensions().add(XExtensionParser.instance().parse(new URI("http://www.xes-standard.org/time.xesext")));
			log.getExtensions().add(XExtensionParser.instance().parse(new URI("http://www.xes-standard.org/concept.xesext")));
			log.getExtensions().add(XExtensionParser.instance().parse(new URI("http://www.xes-standard.org/semantic.xesext")));
			log.getGlobalTraceAttributes().add(new XAttributeLiteralImpl("concept:name", "__INVALID__"));
			log.getGlobalEventAttributes().add(new XAttributeLiteralImpl("concept:name", "__INVALID__"));
			log.getAttributes().put("source", new XAttributeLiteralImpl("source", "DAlloy"));
			log.getAttributes().put("concept:name", new XAttributeLiteralImpl("concept:name", "Artificial Log"));
			log.getAttributes().put("lifecycle:model", new XAttributeLiteralImpl("lifecycle:model", "standard"));
		
		} catch (Exception e) {
			logger.error("No log extensions will be written. Log itself is untouched", e);
		}
	}

	@Override
	public String toString() {
		return "GenerationTaskAlloy [declModel=" + declModel 
				+ ", minTraceLength=" + minTraceLength + ", maxTraceLength=" + maxTraceLength 
				+ ", numberOfPosTraces=" + numberOfPosTraces + ", percentageOfPosVacuousTraces=" + percentageOfPosVacuousTraces
				+ ", numberOfNegTraces=" + numberOfNegTraces + ", percentageOfNegVacuousTraces=" + percentageOfNegVacuousTraces 
				+ ", taskStartTime=" + taskStartTime + "]";
	}
}
