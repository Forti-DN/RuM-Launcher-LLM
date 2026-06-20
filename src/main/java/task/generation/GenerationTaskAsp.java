package task.generation;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;

import generator.AspGenerator;
import javafx.concurrent.Task;
import treedata.TreeDataAttribute;

public class GenerationTaskAsp extends Task<GenerationTaskResult> {

    private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
    
    private File declModel;
	private int minTraceLength;
	private int maxTraceLength;
	private int numberOfPosTraces;
	private List<TreeDataAttribute> traceAttributes;

	public GenerationTaskAsp() {
		super();
	}

	public GenerationTaskAsp(File declModel, int minTraceLength, int maxTraceLength, int numberOfTraces) {
		super();
		this.declModel = declModel;
		this.minTraceLength = minTraceLength;
		this.maxTraceLength = maxTraceLength;
		this.numberOfPosTraces = numberOfTraces;
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
	
	public void setTraceAttributes(List<TreeDataAttribute> traceAttributes) {
		this.traceAttributes = traceAttributes;
	}

    @Override
    protected GenerationTaskResult call() throws Exception {
		long taskStartTime = System.currentTimeMillis();
		logger.info("{} ({}) started at: {}", this.getClass().getSimpleName(), this.hashCode(), taskStartTime);

		try {
			XLog generatedLog = AspGenerator.generateLog(
				declModel.toPath(),
				minTraceLength,
				maxTraceLength,
				numberOfPosTraces,
				LocalDateTime.now(),
				Duration.ofHours(4)
			);

			for (XTrace trace : generatedLog) {
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
				
				attributeMap.put("trace:type", new XAttributeLiteralImpl("trace:type", "positive"));
			}
			
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
}
