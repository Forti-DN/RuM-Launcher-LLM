package task.editor;

import java.lang.invoke.MethodHandles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.beldr.InfiniteSpeechRecognizer;

import declareextraction.constructs.DeclareModel;
import declareextraction.constructs.condition.TimeCondition;
import declareextraction.textprocessing.ConditionParser;
import declareextraction.textprocessing.DeclareConstructor;
import javafx.concurrent.Task;
import util.VoiceInputUtils;

public class VoiceInputTask extends Task<VoiceInputTaskResult> {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final DeclareConstructor constructor = new DeclareConstructor();

	private final TaskType taskType;

	public VoiceInputTask(TaskType taskType) {
		super();
		this.taskType = taskType;
	}

	@Override
	protected VoiceInputTaskResult call() throws Exception {
		try {
			long taskStartTime = System.currentTimeMillis();
			logger.info("{} ({}) started at: {}", this.getClass().getSimpleName(), this.hashCode(), taskStartTime);

			//Starting the recording and getting initial results
			InfiniteSpeechRecognizer.infiniteStreamingRecognize();
			String recordedString = InfiniteSpeechRecognizer.getLastRawString();
			logger.debug("Recorded raw string: {}", recordedString);

			String resultString = null;

			switch (taskType) {
			case CONSTRAINT:
				DeclareModel recordedModel = constructor.convertToDeclareModel(recordedString);
				resultString = VoiceInputUtils.postprocessRawModel(recordedModel);
				break;
			case ACTIVATION_CONDITION:
				String activationCondition = ConditionParser.parseActivationConditions(recordedString);
				resultString = VoiceInputUtils.processFloatToInt(activationCondition);
				break;
			case CORRELATION_CONDITION:
				String correlationCondition = ConditionParser.parseActivationOrCorrelationConditions(recordedString);
				resultString = VoiceInputUtils.processFloatToInt(correlationCondition);
				break;
			case TIME_CONDITION:
				TimeCondition timeCondition = ConditionParser.parseTimeCondition(recordedString);
				if (timeCondition != null) {
					resultString = timeCondition.toRuMString();
				}
				break;
			default:
				throw new Exception("Unhandled task type: " + taskType);
			}

			if (resultString == null || resultString.equals(""))
				logger.warn("No results found from the recorded text: {}", recordedString);

			//Creating the result object
			VoiceInputTaskResult voiceInputTaskResult = new VoiceInputTaskResult();
			voiceInputTaskResult.setRecordedString(recordedString);
			voiceInputTaskResult.setResultString(resultString);
			voiceInputTaskResult.setTaskType(taskType);

			logger.info("{} ({}) completed at: {} - total time: {}",
				this.getClass().getSimpleName(),
				this.hashCode(),
				System.currentTimeMillis(),
				(System.currentTimeMillis() - taskStartTime)
			);

			return voiceInputTaskResult;

		} catch (Exception e) {
			logger.error("{} ({}) failed", this.getClass().getSimpleName(), this.hashCode(), e);
			throw e;
		}
	}
}
