package datatable;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyProperty;

public abstract class AbstractDataRow {

	private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public enum RowStatus {
		NEW, //A row that has not been saved before
		EDITING, //A row that has been saved before and is now being edited
		SAVED; //A row that is currently not being edited
	}

	//All rows are initially in state new and can have only editing values until saved for the first time
	private ReadOnlyObjectWrapper<RowStatus> rowStatus = new ReadOnlyObjectWrapper<>(RowStatus.NEW);

	private boolean isValidatedAndValid = false; //Tracks cell and row level validation

	public ReadOnlyProperty<RowStatus> rowStatusProperty() {
		return rowStatus.getReadOnlyProperty();
	}
	public RowStatus getRowStatus() {
		return rowStatus.get();
	}

	//Invalidates saved row (used if for example if an object referenced by this row is deleted)
	public void invalidateSavedRow() {
		if (rowStatus.get() == RowStatus.SAVED) {
			setCellsToEditing();
		}
		rowStatus.set(RowStatus.NEW);
		isValidatedAndValid = false;
	}


	//Starts row editing
	public final boolean startRowEdit() {
		if (rowStatus.get() == RowStatus.SAVED) {
			setCellsToEditing();
			rowStatus.set(RowStatus.EDITING);
			isValidatedAndValid = false;
			return true;
		} else {
			return false;
		}
	}

	//Prepares all row cells for editing
	private void setCellsToEditing() {
		for (Field field : this.getClass().getDeclaredFields()) {
			if (CellDataWrapper.class.isAssignableFrom(field.getType()))  {
				CellDataWrapper<?> thisFieldWrapper = getCellDataWrapper(field.getName(), this);
				if (thisFieldWrapper != null) {
					thisFieldWrapper.startCellEdit();
				}
			}
		}
	}

	public final boolean validateRowEdit() {
		isValidatedAndValid = rowStatus.get() == RowStatus.SAVED || (validateCellEdits() && validateDaraRow());
		return isValidatedAndValid;
	}


	//Confirms row editing if row editing values are valid
	public final boolean confirmRowEdit() {
		if (rowStatus.get() != RowStatus.SAVED && isValidatedAndValid) { //validateRowEdit must always be called before saving
			confirmCellEdits();
			rowStatus.set(RowStatus.SAVED);
			return true;
		} else {
			return false;
		}
	}

	//Performs all cell level validations and returns false if at least one error is found
	private boolean validateCellEdits() {
		boolean allCellsValid = true;
		for (Field field : this.getClass().getDeclaredFields()) {
			if (CellDataWrapper.class.isAssignableFrom(field.getType()))  {
				CellDataWrapper<?> thisFieldWrapper = getCellDataWrapper(field.getName(), this);
				if (thisFieldWrapper != null && !thisFieldWrapper.validateCellEdit()) {
					allCellsValid = false;
				}
			}
		}
		return allCellsValid;
	}

	//Confirms current editing values of all row cells
	private void confirmCellEdits() {
		for (Field field : this.getClass().getDeclaredFields()) {
			if (CellDataWrapper.class.isAssignableFrom(field.getType()))  {
				CellDataWrapper<?> thisFieldWrapper = getCellDataWrapper(field.getName(), this);
				if (thisFieldWrapper != null) {
					thisFieldWrapper.confirmCellEdit();
				}
			}
		}
	}


	//Cancels row editing
	public final boolean cancelRowEdit() {
		if (rowStatus.get() == RowStatus.EDITING) {
			cancelCellEdits();
			rowStatus.set(RowStatus.SAVED);
			isValidatedAndValid = true;
			return true;
		} else {
			return false;
		}
	}

	//Cancels editing of all row cells
	private void cancelCellEdits() {
		for (Field field : this.getClass().getDeclaredFields()) {
			if (CellDataWrapper.class.isAssignableFrom(field.getType()))  {
				CellDataWrapper<?> thisFieldWrapper = getCellDataWrapper(field.getName(), this);
				if (thisFieldWrapper != null) {
					thisFieldWrapper.cancelCellEdit();
				}
			}
		}
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null) {
			return false;
		} else if(!this.getClass().isAssignableFrom(obj.getClass())) {
			return false;
		} else {
			for (Field field : this.getClass().getDeclaredFields()) { //Returns only the fields defined in the implementing class
				CellDataWrapper<?> thisFieldWrapper = getCellDataWrapper(field.getName(), this);
				CellDataWrapper<?> compareToFieldWrapper = getCellDataWrapper(field.getName(), obj);

				if (thisFieldWrapper != null && compareToFieldWrapper != null) {
					Object thisFieldValue;
					if (this.getRowStatus() == RowStatus.NEW) {
						thisFieldValue = thisFieldWrapper.getEditingValue();
					} else {
						thisFieldValue = thisFieldWrapper.getSavedValue();
					}

					Object compareToFieldValue;
					if (((AbstractDataRow)obj).getRowStatus() == RowStatus.NEW) {
						compareToFieldValue = compareToFieldWrapper.getEditingValue();
					} else {
						compareToFieldValue = compareToFieldWrapper.getSavedValue();
					}

					if (!Objects.equals(thisFieldValue, compareToFieldValue)) {
						return false;
					}
				}
			}
			return true;
		}
	}

	private CellDataWrapper<?> getCellDataWrapper(String fieldName, Object obj) {
		try {
			Method fieldPropertyGetter = this.getClass().getMethod(fieldName+"Property");
			return ((CellDataWrapper<?>)fieldPropertyGetter.invoke(obj));
		} catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
			logger.error("Can not access field: {} of class {} form class {}", fieldName, obj.getClass(), this.getClass(), e);
			return null;
		}
	}

	//Implementation must perform all row level validations
	protected abstract boolean validateDaraRow();
}