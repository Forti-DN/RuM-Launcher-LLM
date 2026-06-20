package util;

import java.math.BigDecimal;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import core.AlloyRunner;
import javafx.css.PseudoClass;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import treedata.TreeDataActivity;

public class ValidationUtils {

	final public static PseudoClass errorClass = PseudoClass.getPseudoClass("error");

	public static Function<String, String> checkMandatoryString = new Function<String, String>() {
		@Override
		public String apply(String value) {
			if (value == null || value.isBlank()) {
				return "A non-empty value is required";
			} else {
				return null;
			}
		}
	};

	public static Function<BigDecimal, String> checkMandatoryPositiveDecimal = new Function<BigDecimal, String>() {
		@Override
		public String apply(BigDecimal value) {
			if (value == null || value.signum() == -1) {
				return "A positive decimal value is required";
			} else {
				return null;
			}
		}
	};

	public static Function<ConstraintTemplate, String> checkMandatoryConstraintTemplate = new Function<ConstraintTemplate, String>() {
		@Override
		public String apply(ConstraintTemplate value) {
			if (value == null) {
				return "A constraint template is required";
			} else {
				return null;
			}
		}
	};

	public static Function<TreeDataActivity, String> checkMandatoryActivity = new Function<TreeDataActivity, String>() {
		@Override
		public String apply(TreeDataActivity value) {
			if (value == null) {
				return "An activity is required";
			} else {
				return null;
			}
		}
	};

	public static Function<String, String> checkConstraintDataCondition = new Function<String, String>() {
		@Override
		public String apply(String value) {
			if (value != null && !value.isBlank() && !AlloyRunner.isValidDataExpression(value)) {
				//AlloyRunner.isValidDataExpression gives wrong results (for example "A.atr > 5.5" is considered invalid and "foobar" is considered valid)
				return "Invalid data condition";
			} else {
				return null;
			}
		}
	};

	public static Function<String, String> checkConstraintTimeCondition = new Function<String, String>() {
		@Override
		public String apply(String value) {
			if (value != null && !value.isBlank() && !value.matches("^\\d+,\\d+,[s,m,h,d]$")) {
				return "Time condition must have the following format: [0-9]+,[0-9]+[s,m,h,d]$\nFor example: 8,12,h";
			} else {
				return null;
			}
		}
	};

	public static void addMandatoryBehavior(TextField... textFields) {
		for (TextField textField : textFields) {
			//Adds PseudoClass "error" on focus lost if no field is empty
			textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
				if (newValue == false) {
					if (validateMandatory(textField.getText())) {
						textField.pseudoClassStateChanged(errorClass, false);
					} else {
						textField.pseudoClassStateChanged(errorClass, true);
					}
				}
			});
		}

	}

	@SafeVarargs
	public static void addMandatoryBehavior(ChoiceBox<String>...choiceBoxes) {
		for (ChoiceBox<String> choiceBox : choiceBoxes) {
			//Adds PseudoClass "error" on focus lost if no text is selected
			choiceBox.focusedProperty().addListener((observable, oldValue, newValue) -> {
				if (newValue == false) {
					if (validateMandatory(choiceBox.getSelectionModel().getSelectedItem())) {
						choiceBox.pseudoClassStateChanged(errorClass, false);
					} else {
						choiceBox.pseudoClassStateChanged(errorClass, true);
					}
				}
			});
		}
	}

	public static boolean validateMandatory(String value) {
		if (value != null && !value.strip().equals("")) {
			return true;
		} else {
			return false;
		}
	}

	@SafeVarargs
	public static void addMandatoryPositiveIntegerBehavior(Spinner<Integer>...spinners) {
		for (Spinner<Integer> spinner : spinners) {
			//Accepts inputs that contain only numeric characters and spaces
			UnaryOperator<Change> filter = new UnaryOperator<TextFormatter.Change>() {
				@Override
				public Change apply(Change change) {
					if (change.getText().matches("[0-9]*")) {
						return change;
					} else {
						return null;
					}
				}
			};
			TextField textField = spinner.getEditor();
			textField.setTextFormatter(new TextFormatter<Integer>(filter));

			//Adds PseudoClass "error" on focus lost if spinner value is null
			spinner.focusedProperty().addListener((observable, oldValue, newValue) -> {
				if (newValue == false) {
					if (spinner.getValue() == null) {
						textField.pseudoClassStateChanged(errorClass, true);
					} else {
						textField.pseudoClassStateChanged(errorClass, false);
					}
				} else {
					//A workaround for the case when spinner is refocused after a validation error has occurred
					textField.pseudoClassStateChanged(errorClass, false);
				}
			});
		}
	}

	public static void addMandatoryIntegerBehavior(TextField...textFields) {
		for (TextField textField : textFields) {
			//Accepts inputs that contain only numeric characters and spaces
			UnaryOperator<Change> filter = new UnaryOperator<TextFormatter.Change>() {
				@Override
				public Change apply(Change change) {
					if (change.getControlNewText().matches("[-]?[0-9]{0,6}")) {
						return change;
					} else {
						return null;
					}
				}
			};
			textField.setTextFormatter(new TextFormatter<Integer>(filter));

			//Adds PseudoClass "error" on focus lost if the text can not be converted to integer
			textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
				if (newValue == false) {
					try {
						Integer integerValue = Integer.valueOf(textField.getText()); //Serves also as a mandatory check
						textField.setText(integerValue.toString()); //Sets the text to correspond to the integer value
						textField.pseudoClassStateChanged(errorClass, false);
					} catch (NumberFormatException e) {
						textField.pseudoClassStateChanged(errorClass, true);
					}
				}
			});
		}
	}

	public static void addMandatoryPositiveIntegerBehavior(TextField...textFields) {
		for (TextField textField : textFields) {
			//Accepts inputs that contain only numeric characters and spaces
			UnaryOperator<Change> filter = new UnaryOperator<TextFormatter.Change>() {
				@Override
				public Change apply(Change change) {
					if (change.getControlNewText().matches("[0-9]{0,6}")) {
						return change;
					} else {
						return null;
					}
				}
			};
			textField.setTextFormatter(new TextFormatter<Integer>(filter));

			//Adds PseudoClass "error" on focus lost if the text can not be converted to integer
			textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
				if (newValue == false) {
					try {
						Integer integerValue = Integer.valueOf(textField.getText()); //Serves also as a mandatory check
						textField.setText(integerValue.toString()); //Sets the text to correspond to the integer value
						textField.pseudoClassStateChanged(errorClass, false);
					} catch (NumberFormatException e) {
						textField.pseudoClassStateChanged(errorClass, true);
					}
				}
			});
		}
	}

	public static void addMandatoryDecimalBehavior(TextField...textFields) {
		for (TextField textField : textFields) {
			//Accepts inputs that contain only numeric characters and spaces
			UnaryOperator<Change> filter = new UnaryOperator<TextFormatter.Change>() {
				@Override
				public Change apply(Change change) {
					if (change.getControlNewText().matches("[-]?[.][0-9]{0,6}|[-]?[0-9]{0,6}|[-]?[0-9]{1,6}[.]|[-]?[0-9]{1,6}[.][0-9]{0,6}")) {
						return change;
					} else {
						return null;
					}
				}
			};
			textField.setTextFormatter(new TextFormatter<BigDecimal>(filter));

			//Adds PseudoClass "error" on focus lost if the text can not be converted to integer
			textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
				if (newValue == false) {
					if (textField.getText().length() == 0) {
						textField.pseudoClassStateChanged(errorClass, true);
					} else {
						textField.pseudoClassStateChanged(errorClass, false);
					}
				}
			});
		}
	}

	public static void addMandatoryPositiveDecimalBehavior(TextField...textFields) {
		for (TextField textField : textFields) {
			//Accepts inputs that contain only numeric characters and spaces
			UnaryOperator<Change> filter = new UnaryOperator<TextFormatter.Change>() {
				@Override
				public Change apply(Change change) {
					if (change.getControlNewText().matches("[.][0-9]{0,6}|[0-9]{0,6}|[0-9]{1,6}[.]|[0-9]{1,6}[.][0-9]{0,6}")) {
						return change;
					} else {
						return null;
					}
				}
			};
			textField.setTextFormatter(new TextFormatter<BigDecimal>(filter));

			//Adds PseudoClass "error" on focus lost if the text can not be converted to integer
			textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
				if (newValue == false) {
					if (textField.getText().length() == 0) {
						textField.pseudoClassStateChanged(errorClass, true);
					} else {
						textField.pseudoClassStateChanged(errorClass, false);
					}
				}
			});
		}
	}

	public static void addMandatoryPrecentageBehavior(String precentageFormat, double upperBound, TextField...textFields) {
		for (TextField textField : textFields) {
			UnaryOperator<Change> filter = new UnaryOperator<TextFormatter.Change>() {
				@Override
				public Change apply(Change change) {
					if (change.getControlNewText().matches("[.][0-9]{0,1}|[0-9]{0,3}|[0-9]{1,3}[.]|[0-9]{1,3}[.][0-9]?")) {
						return change;
					} else {
						return null;
					}
				}
			};
			textField.setTextFormatter(new TextFormatter<Integer>(filter));

			textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
				if (newValue == false) {
					try {
						Double doubleValue = Double.valueOf(textField.getText());
						if (doubleValue <= upperBound) {
							textField.setText(String.format(precentageFormat, doubleValue));
						} else {
							textField.setText(String.format(precentageFormat, upperBound));
						}
					} catch (NumberFormatException e) {
						textField.setText(String.format(precentageFormat, upperBound));
					}
				}
			});
		}
	}


	public static boolean validateDataCondition(String dataCondition) {
		return AlloyRunner.isValidDataExpression(dataCondition); //Gives wrong results (for example "A.atr > 5.5" is considered invalid and "foobar" is considered valid)
	}

	public static boolean validateTimeCondition(String timeCondition) {
		return timeCondition.matches("^\\d+,\\d+,[s,m,h,d]$");
	}
}
