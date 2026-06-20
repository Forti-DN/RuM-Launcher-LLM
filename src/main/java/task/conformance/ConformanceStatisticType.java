package task.conformance;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import controller.conformance.ConformanceMethod;

public enum ConformanceStatisticType {
  NAME("Name"), //Not really a statistic, but having it here allows for convenient sorting
  INSERTIONS("Insertions"),
  DELETIONS("Deletions"),
  FITNESS("Fitness"),
  ACTIVATIONS("Activations"),
  FULFILLMENTS("Fulfilments"),
  VIOLATIONS("Violations"),
  VACUOUS_FULFILLMENTS("Vacuous Fulfilments"),
  VACUOUS_VIOLATIONS("Vacuous Violations"),
  DATA_DIFFERENCES("Data-differences");

  private static final Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

  private String displayText;

  private ConformanceStatisticType(String displayText) {
    this.displayText = displayText;
  }

  public String getDisplayText() {
    return displayText;
  }

  public static List<ConformanceStatisticType> values(ConformanceMethod conformanceMethod) {
    switch (conformanceMethod) {
      case ANALYZER:
        return Arrays.asList(NAME, ACTIVATIONS, FULFILLMENTS, VIOLATIONS, VACUOUS_FULFILLMENTS, VACUOUS_VIOLATIONS);
      case REPLAYER:
      case PLAN_BASED:
        return Arrays.asList(NAME, INSERTIONS, DELETIONS, FITNESS);
      case DATA_REPLAYER:
        return Arrays.asList(NAME, INSERTIONS, DELETIONS, FITNESS, DATA_DIFFERENCES);
      default:
        logger.error("Undandled conformance method statisitcs: {}", conformanceMethod);
        return Arrays.asList(NAME);
    }
  }
}
