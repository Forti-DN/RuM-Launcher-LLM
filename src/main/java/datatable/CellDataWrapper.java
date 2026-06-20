package datatable;

import java.util.function.Function;

import datatable.AbstractDataRow.RowStatus;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;

public class CellDataWrapper<T> extends ReadOnlyObjectWrapper<CellDataWrapper<T>> {

	private T savedValue;
	private T editingValue;

	private AbstractDataRow dataRow;
	private Function <T, String> cellValidationFunction;

	private boolean isValidatedAndValid = false;
	private ReadOnlyStringWrapper editingError = new ReadOnlyStringWrapper();

	//CellDataWrapper instances must be accessible from AbstractDataRow via reflection using XProperty() getter where X is the attribute name
	//cellValidationFunction must perform all cell level validations; can be null
	public CellDataWrapper(AbstractDataRow dataRow, Function <T, String> cellValidationFunction) {
		super.setValue(this);

		this.dataRow = dataRow;
		dataRow.rowStatusProperty().addListener((observable, oldValue, newValue) -> {
			this.fireValueChangedEvent(); //Allows observers to update every time when row status changes
		});

		editingError.addListener((observable, oldValue, newValue) -> {
			this.fireValueChangedEvent(); //Allows observers to update every time when row errors change
		});

		this.cellValidationFunction = cellValidationFunction;
	}


	public RowStatus getRowStatus() {
		return dataRow.rowStatusProperty().getValue();
	}


	public T getSavedValue() {
		return savedValue;
	}
	public void invalidateSavedValue() {
		T invalidatedSavedValue = this.savedValue;
		this.savedValue = null;
		if (dataRow.rowStatusProperty().getValue() == RowStatus.SAVED || invalidatedSavedValue.equals(this.editingValue)) {
			//If a different editing value is already selected then setting this error may be confusing for the user
			this.setEditingError("Saved value was invalidated");
		}
		isValidatedAndValid = false;
		dataRow.invalidateSavedRow();
	}

	public T getEditingValue() {
		return editingValue;
	}
	public void setEditingValue(T editingValue) {
		this.editingValue = editingValue; //Invalid editing values are allowed by design
		isValidatedAndValid = false;
	}

	public String getEditingError() {
		return editingError.getValue();
	}
	public void setEditingError(String editingError) { //Must be public to allow setting table level errors
		this.editingError.set(editingError);
		if (editingError != null) {
			isValidatedAndValid = false;
		}
	}

	protected void startCellEdit() {
		this.editingValue = savedValue;
	}

	public boolean validateCellEdit() {
		if (cellValidationFunction != null) {
			editingError.set(cellValidationFunction.apply(editingValue));
			isValidatedAndValid = editingError.getValue() == null;
		} else {
			editingError.set(null);
			isValidatedAndValid = true;
		}
		return isValidatedAndValid;
	}

	protected boolean confirmCellEdit() {
		if (isValidatedAndValid) { //validateCellEdit must always be called before saving
			savedValue = editingValue;
			editingValue = null;
			return true;
		} else {
			return false;
		}
	}

	protected void cancelCellEdit() {
		editingError.set(null);
		editingValue = null;
		isValidatedAndValid = true;
	}


	@Override
	public String toString() {
		//Removing toString override would cause an infinite loop because of extending SimpleObjectProperty and calling super.setValue(this)
		return "CellDataWrapper [savedValue=" + savedValue + ", editingValue=" + editingValue + ", rowStatus="
		+ dataRow.rowStatusProperty() + ", isValidatedAndValid=" + isValidatedAndValid + ", editingError=" + editingError + "]";
	}

}