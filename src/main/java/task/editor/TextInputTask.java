package task.editor;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

import controller.editor.Constraint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import declareextraction.constructs.DeclareModel;
import declareextraction.constructs.condition.TimeCondition;
import declareextraction.textprocessing.ConditionParser;
import declareextraction.textprocessing.DeclareConstructor;
import javafx.concurrent.Task;
import util.VoiceInputUtils;

public class TextInputTask extends Task<String> {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final DeclareConstructor constructor = new DeclareConstructor();

	private ArrayList<Constraint> constraint;
	private String inputText;
	private TaskType taskType;

	public TextInputTask(String inputText, TaskType taskType) {
		super();
		this.inputText = inputText;
		this.taskType = taskType;
	}

	public TextInputTask(ArrayList<Constraint> constraint, TaskType taskType) {
		super();
		this.constraint = constraint;
		this.taskType = taskType;
	}

	public ArrayList<Constraint> getConstraints() {
		return constraint;
	}

	public TaskType getTaskType() {
		return taskType;
	}

	@Override
	protected String call() throws Exception {
		try {
			long taskStartTime = System.currentTimeMillis();
			logger.info("{} ({}) started at: {}", this.getClass().getSimpleName(), this.hashCode(), taskStartTime);

			String resultString = null;

			switch (taskType) {
			case CONSTRAINT:
//				final DeclareModel generatedModel = constructor.convertToDeclareModel(inputText);
//				resultString = VoiceInputUtils.postprocessRawModel(generatedModel);
				break;
			case ACTIVATION_CONDITION:
				String activationCondition = ConditionParser.parseActivationConditions(inputText);
				resultString = VoiceInputUtils.processFloatToInt(activationCondition);
				break;
			case CORRELATION_CONDITION:
				String correlationCondition = ConditionParser.parseActivationOrCorrelationConditions(inputText);
				resultString = VoiceInputUtils.processFloatToInt(correlationCondition);
				break;
			case TIME_CONDITION:
				TimeCondition timeCondition = ConditionParser.parseTimeCondition(inputText);
				if (timeCondition != null) {
					resultString = timeCondition.toRuMString();
				}
				break;
			default:
				throw new Exception("Unhandled task type: " + taskType);
			}

			if (resultString == null) 
				logger.warn("No results found from the input text: {}", inputText);

			logger.info("{} ({}) completed at: {} - total time: {}",
				this.getClass().getSimpleName(),
				this.hashCode(),
				System.currentTimeMillis(),
				(System.currentTimeMillis() - taskStartTime)
			);
			
			return resultString;

		} catch (Exception e) {
			logger.error("{} ({}) failed", this.getClass().getSimpleName(), this.hashCode(), e);
			throw e;
		}
	}
}
