package controller.editor;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.CodeSigner;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import alice.tuprologx.pj.model.Cons;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kordamp.ikonli.javafx.FontIcon;

import controller.editor.data.ConstraintDataRow;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import task.editor.TaskType;
import task.editor.TextInputTask;
import task.editor.VoiceInputTask;
import task.editor.VoiceInputTaskResult;
import treedata.TreeDataActivity;
import util.ConstraintUtils;
import util.ModelUtils;
import util.Parsers;

public class VoiceInputChatBox {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private ListView<HBox> chatListView;

	private EditorTabController editorTabController;

	private List<TreeDataActivity> currentRecordedActivities = new ArrayList<>();
	private List<ConstraintDataRow> currentRecordedConstraints = new ArrayList<>();

	private boolean activationConditionDone;
	private boolean correlationConditionDone;
	private boolean timeConditionDone;

	private ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

	public VoiceInputChatBox() {
		chatListView = new ListView<>();
		chatListView.getStyleClass().add("chat-box");
		writeIntroduction();
		askForConstraint(false);
	}

	public ListView<HBox> getRoot() {
		return chatListView;
	}

	private void writeIntroduction() {
		writeSystemMessage("Hi! I am Declo", false);
		writeSystemMessage("Ask me what you need, I can help you create a model or answer any question you have!", false);
	}

	private void writeSystemMessage(String message, boolean italic) {
		Label messageLabel = new Label(message);
		messageLabel.getStyleClass().add("message-label__system");
		if (italic) {
			messageLabel.setStyle("-fx-font-style: italic;");
		}

		HBox messageRow = new HBox(messageLabel);
		messageRow.getStyleClass().add("message-row__system");
		chatListView.getItems().add(messageRow);
		chatListView.scrollTo(chatListView.getItems().size()-1);
	}

	private void askForConstraint(boolean insertSeparator) {
		if (insertSeparator) {
			Separator separator = new Separator();
			HBox.setHgrow(separator, Priority.ALWAYS);
			chatListView.getItems().add(new HBox(separator));
		}

		activationConditionDone = false;
		correlationConditionDone = false;
		timeConditionDone = false;
		currentRecordedActivities.clear();
		currentRecordedConstraints.clear();

		createConstraintInputRow();
	}

	private void createConstraintInputRow() {
		HBox messageRow = createUserMessageRow();
		Button chatButton = createChatButton("Let's chat", messageRow);
		messageRow.getChildren().setAll(chatButton);
	}

	private HBox createUserMessageRow(Node... children) {
		HBox messageRow = new HBox(children);
		messageRow.getStyleClass().add("message-row__user");
		chatListView.getItems().add(messageRow);
		chatListView.scrollTo(chatListView.getItems().size()-1);
		return messageRow;
	}

	public void setEditorTabController(EditorTabController editorTabController) {
		this.editorTabController = editorTabController;
	}

// --------

	public class ChatForm{
		TextArea editInput;
		Button sendButton;
		Button confirmButton;
		Button searchFileButton;
		HBox buttonsRow;
		VBox editingBox;
		HBox messageRow;

		ChatForm(TextArea editInput, Button searchFileButton, Button sendButton, Button confirmButton){
			this.editInput = editInput;
			this.sendButton = sendButton;
			this.confirmButton = confirmButton;
			this.searchFileButton = searchFileButton;
			buttonsRow = new HBox(searchFileButton, sendButton, confirmButton);
			editingBox = new VBox(editInput, buttonsRow);
			messageRow = createUserMessageRow(editingBox);
		}

		public TextArea getEditInput() {
			return editInput;
		}

		public Button getSendButton() {
			return sendButton;
		}

		public Button getConfirmButton() {
			return confirmButton;
		}

		public Button getSearchFileButton() {
			return searchFileButton;
		}

		public HBox getButtonsRow() {
			return buttonsRow;
		}

		public VBox getEditingBox() {
			return editingBox;
		}

		public HBox getMessageRow() {
			return messageRow;
		}
	}

	private ChatForm formSetup(){
		ChatForm chatForm = new ChatForm(new TextArea(null), new Button("select File"), new Button("send"), new Button("confirm"));
		chatForm.getEditInput().setStyle("-fx-max-height: 60px;");
		chatForm.getEditInput().setWrapText(true);

		chatForm.getSearchFileButton().getStyleClass().add("small-button");
		chatForm.getSendButton().getStyleClass().add("small-button");
		chatForm.getConfirmButton().getStyleClass().add("small-button");

		chatForm.getButtonsRow().setStyle("-fx-alignment: center_right;");
		chatForm.getButtonsRow().setSpacing(3d);

		return chatForm;
	}


	private String fetchResponse(String input) throws IOException {
		URL url = URI.create("http://localhost:8080/api/v1/llm/chatOllama").toURL();
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.setRequestProperty("Content-Type", "application/json");

		String inputForm = "{ \"query\":" + "\"" + input + "\"" + "}";

		try (DataOutputStream os = new DataOutputStream(conn.getOutputStream())){
			os.writeBytes(inputForm);
		}

		String line = "";
		String fullLine = "";

		try (BufferedReader bf = new BufferedReader(new InputStreamReader(conn.getInputStream()))){
			while ((line = bf.readLine()) != null){
				fullLine = line;
			}
		}

        return fullLine.substring(32, fullLine.length() - 2);
	}

	private String fetchGroqResponse(String input) throws IOException{
		URL url = URI.create("http://localhost:8080/api/v1/llm/chatGroq").toURL();
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.setRequestProperty("Content-Type", "application/json");

		String inputForm = "{ \"query\":" + "\"" + input + "\"" + "}";

		try (DataOutputStream os = new DataOutputStream(conn.getOutputStream())){
			os.writeBytes(inputForm);
		}

		String line = "";
		String fullLine = "";

		try (BufferedReader bf = new BufferedReader(new InputStreamReader(conn.getInputStream()))){
			while ((line = bf.readLine()) != null){
				fullLine = line;
			}
		}
		System.out.println(fullLine.substring(32, fullLine.length() - 2));
		return fullLine.substring(32, fullLine.length() - 2);
	}

	public ArrayList<Constraint> parseActivities(String response){
		StringBuilder constraint = new StringBuilder();
		ArrayList<Constraint> constraints = new ArrayList<>();
		String[] elements;
		Pattern pattern = Pattern.compile("(.*?)\\| |(.*?)\\. |(.*?)\\.");
		Matcher match = pattern.matcher(response);
		while (match.find()) {
			constraint.append(match.group());
		}

		elements = constraint.toString().split("(\\| )|( \\.)");

		for(String ele: elements){
			System.out.println(ele);
		}

		for(String element: elements){
			if (element.matches("(.*?\\(.*?\\) )|(.*?\\(.*?\\))|(.*?\\(.*?\\)\\.)")){
				System.out.println("CONSTRAINT " + element);
				String[] blocks = element.split("(\\()|, |,|(\\))");

				System.out.println("template " + blocks[0]);
				System.out.println("activity_1 " + blocks[1]);

				if (blocks[2].isBlank() || blocks[2].equals(".")) {
					Constraint constr = new Constraint(blocks[0], blocks[1], "");
					constraints.add(constr);
				} else {
					System.out.println("activity_2 " + blocks[2]);
					Constraint constr = new Constraint(blocks[0], blocks[1], blocks[2]);
					constraints.add(constr);
				}
			} else {
				return null;
			}
		}
		return constraints;
	}

	String current_response = "";
	ArrayList<Constraint> list = new ArrayList<>();


	private void askForTextualQuery(){
		ChatForm chatForm = formSetup();

		chatForm.getSearchFileButton().setOnAction(event -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Text Files", "*.txt", "*.pdf"));
			File selectedFile = fileChooser.showOpenDialog(editorTabController.getStage());

			if (selectedFile != null){
				Scanner scan = null;
				try {
					scan = new Scanner(selectedFile);
				} catch (FileNotFoundException e) {
					throw new RuntimeException(e);
				}
				StringBuilder sb = new StringBuilder();

				while (scan.hasNextLine()) {
					sb.append(scan.nextLine());
				}

				scan.close();
				String fileText = sb.toString();

				try {
					String output = "";
					if (editorTabController.getIsGroqToggleEnabled() && editorTabController.getIsValidAPIKey()) {
						output = fetchGroqResponse(fileText);
					} else{
						output = fetchResponse(fileText);
					}
					current_response = output;

					updateUserMessage("", chatForm.getMessageRow(), false);
					writeSystemMessage(output, true);
						askForTextualQuery();

				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});

		chatForm.getSendButton().setOnAction(event -> {
			try {
				if (!(chatForm.getEditInput().getText() == null)){

					String output = "";
					if (editorTabController.getIsGroqToggleEnabled() && editorTabController.getIsValidAPIKey()) {
						output = fetchGroqResponse(chatForm.getEditInput().getText());
					} else{
						output = fetchResponse(chatForm.getEditInput().getText());
					}
					current_response = output;

					updateUserMessage(chatForm.getEditInput().getText(), chatForm.getMessageRow(), false);
					writeSystemMessage(output, true);
					askForTextualQuery();
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});

		chatForm.getConfirmButton().setOnAction(event -> {
			ArrayList<Constraint> constraints = parseActivities(current_response);

			if(constraints == null){
				updateUserMessage("", chatForm.getMessageRow(), false);
				writeSystemMessage("the response may not be in the correct form. " +
						"(try to say \"follow the template\" to fix the issue. " +
						"the constraint must be of the form: constraint() | constraint() and must end with a \".\" ).", true);
				askForTextualQuery();
			}else{
				TextInputTask textInputTask = createTextInputTask(parseActivities(current_response), TaskType.CONSTRAINT,true);
				textInputTask.runningProperty().addListener((obs, oldVal, newVal) -> chatForm.getMessageRow().setDisable(newVal));
				executorService.execute(textInputTask);
			}
		});

		ChangeListener<Scene> sceneListener = new ChangeListener<Scene>() {
			@Override
			public void changed(ObservableValue<? extends Scene> observable, Scene oldValue, Scene newValue) {
				if (newValue != null) {
					chatForm.getEditInput().requestFocus();
					chatForm.getEditInput().sceneProperty().removeListener(this);
				}
			}
		};
		chatForm.getEditInput().sceneProperty().addListener(sceneListener);
	}
// ---------

	private TextInputTask createTextInputTask(ArrayList<Constraint> constraint, TaskType taskType, boolean done) {
		TextInputTask textInputTask = new TextInputTask(constraint, taskType);

		textInputTask.setOnSucceeded(event -> {
//			if (done && (textInputTask.getValue() == null || textInputTask.getValue().equals(""))) {
//				updateUserMessage(editedText, messageRow, true);
//				if (taskType == TaskType.CONSTRAINT) {
//					writeSystemMessage("I did not find any constraints from this sentence, but we can try again.", false);
//					createConstraintInputRow();
//				} else {
//					writeSystemMessage("I did not find a condition from this sentence, but we can try again.", false);
//					createConditionInputRow(false);
//				}
//			} else {
//				switch (textInputTask.getTaskType()) {
//				case CONSTRAINT:
					processConstraintTaskType(textInputTask.getConstraints());
//					break;
//				case ACTIVATION_CONDITION:
//					processActivationTaskType(textInputTask.getValue());
//					break;
//				case CORRELATION_CONDITION:
//					processCorrelationTaskType(textInputTask.getValue());
//					break;
//				case TIME_CONDITION:
//					processTimeTaskType(textInputTask.getValue());
//					break;
//				default:
//					logger.error("Unhandled task type for creating text input task: {}", textInputTask.getTaskType());
//					break;
//				}

//				if (done) {
//					updateUserMessage(editedText, messageRow, true);
//					if (taskType == TaskType.CONSTRAINT) {
//						askForCondition();
//					} else {
//						if (taskType == TaskType.ACTIVATION_CONDITION) {
//							activationConditionDone = true;
//						} else if (taskType == TaskType.CORRELATION_CONDITION) {
//							correlationConditionDone = true;
//						} else if (taskType == TaskType.TIME_CONDITION) {
//							timeConditionDone = true;
//						}
//						logger.error("Unhandled task type for confirming text input task: {}", textInputTask.getTaskType());
//						askForConditionType(false);
//					}
//				}
//			}
		});

//		textInputTask.setOnFailed(event -> {
//			handleInputTaskFailure(false, taskType, messageRow);
//		});

		return textInputTask;
	}

	private void updateUserMessage(String message, HBox messageRow, boolean isFreeform) {
		Label messageLabel = new Label();
		if (isFreeform) {
			message = "\"" + message + "\"";
			messageLabel.setStyle("-fx-font-style: italic;");
		}
		if(!message.isEmpty()){
			messageLabel.setText(message);
			messageLabel.getStyleClass().add("message-label__user");
			messageRow.getChildren().setAll(messageLabel);
		}else{
			messageRow.getChildren().setAll();
		}
	}

	private Button createChatButton(String buttonText, HBox messageRow){
		Button button = new Button(buttonText);
		button.getStyleClass().add("small-button");
		FontIcon buttonFontIcon = new FontIcon("fa-pencil");
		buttonFontIcon.getStyleClass().add("small-button__icon");
		button.setGraphic(buttonFontIcon);

		button.setOnAction(event -> {
			updateUserMessage(buttonText, messageRow, false);
			askForTextualQuery();
		});

        return button;
    }

//	private Button createCancelRecordButton() {
//		Button cancelRecordButton = new Button("Cancel");
//		cancelRecordButton.getStyleClass().add("small-button");
//		FontIcon recordFontIcon = new FontIcon("fa-microphone-slash");
//		recordFontIcon.getStyleClass().add("small-button__icon");
//		cancelRecordButton.setGraphic(recordFontIcon);
//		return cancelRecordButton;
//	}

	private void processConstraintTaskType(ArrayList<Constraint> constraints) {
		for (Constraint constr: constraints) {
			logger.debug("Processing constraint task results: {}", constr.toString());
		}

		removeCurrentRecordedDataRows();

		List<TreeDataActivity> treeDataActivities = new ArrayList<>();
		editorTabController.getActivitiesRoot().getChildren().forEach(item -> treeDataActivities.add((TreeDataActivity)item));

		for (Constraint constr : constraints) {
			boolean isDuplicate_one = false;
			boolean isDuplicate_two = false;
			for (TreeDataActivity treeDataActivity : treeDataActivities) {
				if (treeDataActivity.getActivityName().equals(constr.getActivity_one())) {
					isDuplicate_one = true;
				}

				if (treeDataActivity.getActivityName().equals(constr.getActivity_two())) {
					isDuplicate_two = true;
				}
			}

			if (isDuplicate_one) {
				logger.debug("Skipped adding duplicate activity to voice recording layer: {}", constr.getActivity_one());
			} else {
				TreeDataActivity treeDataAct = new TreeDataActivity(constr.getActivity_one());
				editorTabController.getActivitiesRoot().getChildren().add(treeDataAct);
				currentRecordedActivities.add(treeDataAct);
				treeDataActivities.add(treeDataAct);
				logger.debug("Added valid activity to voice recording layer: {}", constr.getActivity_one());
			}

			if (isDuplicate_two){
				logger.debug("Skipped adding duplicate activity to voice recording layer: {}", constr.getActivity_two());
			} else if (!constr.getActivity_two().isEmpty()){
				TreeDataActivity treeDataAct2 = new TreeDataActivity(constr.getActivity_two());
				editorTabController.getActivitiesRoot().getChildren().add(treeDataAct2);
				currentRecordedActivities.add(treeDataAct2);
				treeDataActivities.add(treeDataAct2);
				logger.debug("Added valid activity to voice recording layer: {}", constr.getActivity_two());
			}
		}

//		List<String> constraintsList = ModelUtils.getConstraintsList(taskResultString);

//		for(String constr: constraintsList) {
//			System.out.println("Constraints -> " + constr);
//		}

		for (Constraint constr : constraints) {
			ConstraintDataRow detectedConstraint = ConstraintUtils.getConstraintDataRow(constr, treeDataActivities);
			if (editorTabController.getConstraintRows().contains(detectedConstraint)) {
				detectedConstraint.invalidateSavedRow();
				logger.debug("Added duplicate constraint to voice recording layer ({}): {}", detectedConstraint.hashCode(), constr.toString());
			} else if (detectedConstraint.validateRowEdit()) {
				detectedConstraint.confirmRowEdit();
				logger.debug("Added valid constraint to voice recording layer ({}): {}", detectedConstraint.hashCode(), constr.toString());
			} else {
				logger.debug("Added invalid constraint to voice recording layer ({}): {}", detectedConstraint.hashCode(), constr.toString());
			}
			editorTabController.getConstraintRows().add(detectedConstraint);
			currentRecordedConstraints.add(detectedConstraint);
		}

		editorTabController.scrollTablesToEnd();
		editorTabController.updateVisualization();

		logger.info("Recording results processed");
	}

//	private void processActivationTaskType(String taskResultString) {
//		logger.debug("Processing activation task results: {}", taskResultString);
//		for (ConstraintDataRow constraintDataRow : currentRecordedConstraints) {
//			constraintDataRow.startRowEdit();
//			constraintDataRow.activationConditionProperty().setEditingValue(taskResultString);
//			if (constraintDataRow.validateRowEdit()) {
//				constraintDataRow.confirmRowEdit();
//
//				//Initial implementation for detecting attributes from conditions
////				String conditionAttribute = taskResultString.substring(taskResultString.indexOf(".")+1, taskResultString.indexOf(" "));
////				TreeDataAttribute existingAttribute = null;
////
////				if (unconfirmedAttribute != null) {
////					for (int i = unconfirmedAttribute.getActivitiesUnmodifiable().size()-1; i >= 0; i--) {
////						unconfirmedAttribute.removeActivity(unconfirmedAttribute.getActivitiesUnmodifiable().get(i));
////					}
////				}
////				for (TreeDataAttribute treeDataAttribute : editorTabController.getAllAttributes()) {
////					if (treeDataAttribute.getAttributeName().equals(conditionAttribute)) {
////						existingAttribute = treeDataAttribute;
////						break;
////					}
////				}
////				if (existingAttribute == null) {
////					unconfirmedAttribute = new TreeDataAttribute();
////					unconfirmedAttribute.setAttributeName(conditionAttribute);
////
////					String conditionalPart = taskResultString.substring(taskResultString.indexOf(".")+1 + conditionAttribute.length()).strip();
////					if (conditionalPart.startsWith("is ")) {
////						unconfirmedAttribute.setAttributeType(AttributeType.ENUMERATION);
////						List<String> possibleValues = new ArrayList<String>();
////						possibleValues.add(conditionalPart.substring(3));
////						if (possibleValues.contains("true")) {
////							possibleValues.add("false");
////						} else if (possibleValues.contains("false")) {
////							possibleValues.add("true");
////						}
////						unconfirmedAttribute.setPossibleValues(possibleValues);
////					} else if (conditionalPart.startsWith("is not ")) {
////						unconfirmedAttribute.setAttributeType(AttributeType.ENUMERATION);
////						List<String> possibleValues = new ArrayList<String>();
////						possibleValues.add(conditionalPart.substring(7));
////						if (possibleValues.contains("true")) {
////							possibleValues.add("false");
////						} else if (possibleValues.contains("false")) {
////							possibleValues.add("true");
////						}
////						unconfirmedAttribute.setPossibleValues(possibleValues);
////					} else if (conditionalPart.startsWith("in (")) {
////						unconfirmedAttribute.setAttributeType(AttributeType.ENUMERATION);
////						List<String> possibleValues = Arrays.asList(conditionalPart.substring(4, conditionalPart.length()-1).split(", "));
////						unconfirmedAttribute.setPossibleValues(possibleValues);
////					} else if (conditionalPart.startsWith("not in (")) {
////						unconfirmedAttribute.setAttributeType(AttributeType.ENUMERATION);
////						List<String> possibleValues = Arrays.asList(conditionalPart.substring(8, conditionalPart.length()-1).split(", "));
////						unconfirmedAttribute.setPossibleValues(possibleValues);
////					} else {
////						unconfirmedAttribute.setAttributeType(AttributeType.INTEGER);
////						unconfirmedAttribute.setValueFrom(new BigDecimal(1));
////						unconfirmedAttribute.setValueTo(new BigDecimal(100));
////					}
////
////					constraintDataRow.getActivationActivity().addAttribute(unconfirmedAttribute);
////					//constraintDataRow.getTargetActivity().addAttribute(unconfirmedAttribute);
////				} else {
////					constraintDataRow.getActivationActivity().addAttribute(existingAttribute);
////					//constraintDataRow.getTargetActivity().addAttribute(existingAttribute);
////				}
//
//			}
//		}
//		editorTabController.scrollTablesToEnd();
//	}

//	private void processCorrelationTaskType(String taskResultString) {
//		logger.debug("Processing correlation task results: {}", taskResultString);
//		for (ConstraintDataRow constraintDataRow : currentRecordedConstraints) {
//			if (constraintDataRow.getTemplate().getIsBinary()) {
//				constraintDataRow.startRowEdit();
//				constraintDataRow.correlationConditionProperty().setEditingValue(taskResultString);
//				if (constraintDataRow.validateRowEdit()) {
//					constraintDataRow.confirmRowEdit();
//
//					//Initial implementation for detecting attributes from conditions
////					String conditionAttribute = taskResultString.substring(taskResultString.lastIndexOf(" ")+1);
////					TreeDataAttribute existingAttribute = null;
////
////					if (unconfirmedAttribute != null) {
////						for (int i = unconfirmedAttribute.getActivitiesUnmodifiable().size()-1; i >= 0; i--) {
////							unconfirmedAttribute.removeActivity(unconfirmedAttribute.getActivitiesUnmodifiable().get(i));
////						}
////					}
////					for (TreeDataAttribute treeDataAttribute : editorTabController.getAllAttributes()) {
////						if (treeDataAttribute.getAttributeName().equals(conditionAttribute)) {
////							existingAttribute = treeDataAttribute;
////							break;
////						}
////					}
////					if (existingAttribute == null) {
////						unconfirmedAttribute = new TreeDataAttribute();
////						unconfirmedAttribute.setAttributeName(conditionAttribute);
////						unconfirmedAttribute.setAttributeType(AttributeType.INTEGER);
////						unconfirmedAttribute.setValueFrom(new BigDecimal(1));
////						unconfirmedAttribute.setValueTo(new BigDecimal(100));
////						constraintDataRow.getActivationActivity().addAttribute(unconfirmedAttribute);
////						constraintDataRow.getTargetActivity().addAttribute(unconfirmedAttribute);
////					} else {
////						constraintDataRow.getActivationActivity().addAttribute(existingAttribute);
////						constraintDataRow.getTargetActivity().addAttribute(existingAttribute);
////					}
//				}
//			}
//		}
//		editorTabController.scrollTablesToEnd();
//	}

	private void removeCurrentRecordedDataRows() {
		for (ConstraintDataRow recordedConstraint : currentRecordedConstraints) {
			for (int i = editorTabController.getConstraintRows().size()-1; i >= 0; i--) {
				if (editorTabController.getConstraintRows().get(i)==recordedConstraint) {
					//Checking by reference to make sure no wrong rows are removed
					editorTabController.getConstraintRows().remove(i);
				}
			}
		}
		currentRecordedConstraints.clear();

		for (TreeDataActivity recordedActivity : currentRecordedActivities) {
			editorTabController.getActivitiesRoot().getChildren().remove(recordedActivity);
		}
		currentRecordedActivities.clear();

		//TODO: To be removed after changing the visualisation mechanism
		editorTabController.updateVisualization();
	}
}
