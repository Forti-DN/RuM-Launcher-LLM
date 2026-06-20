package util;

import java.util.List;

import declareextraction.constructs.ConstraintType;
import declareextraction.constructs.DeclareConstraint;
import declareextraction.constructs.DeclareModel;

public final class VoiceInputUtils {
	//Private constructor to avoid unnecessary instantiation of the class
	private VoiceInputUtils() {
	}

	public static String postprocessRawModel(DeclareModel rawModel) {
		StringBuilder modelBuilder = new StringBuilder();
		if (rawModel != null && !rawModel.getConstraints().isEmpty()) {
			List<DeclareConstraint> constraints = rawModel.getConstraints();
			for (DeclareConstraint constraint: constraints) {
				if (constraint.getType().equals(ConstraintType.ABSENCE)) {
					//Absence is the "negative" positive constraint, "not" not needed in action
					modelBuilder.append("activity ").append(constraint.getActionA().baseStr()).append("\n");
				} else {
					//actionStr includes parts of the action like "not", "immediately"
					modelBuilder.append("activity ").append(constraint.getActionA().actionStr()).append("\n");
					if (constraint.getActionB() != null) {
						//baseStr is used as "not" and "immediately" are extracted into the constraint type, no need to duplicate
						modelBuilder.append("activity ").append(constraint.getActionB().baseStr()).append("\n");
					}
				}
			}

			for (DeclareConstraint constraint: constraints) {
				modelBuilder.append(constraint.toRuMString());
				modelBuilder.append("\n");
			}
		}
		return modelBuilder.toString();
	}

	public static String processFloatToInt(String processedCondition) {
		if (processedCondition != null) {
			return processedCondition.replaceAll("(?<![AT])[.][0-9]+", "");
		}
		return null;
	}
}
