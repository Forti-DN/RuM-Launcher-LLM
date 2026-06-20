package controller.conformance.plannerTextBased.test;

public class RandomTestClass {

  public static void main(String[] args) {

    String test1 = "(del-activity24_complete-t2-t3 )";
    String test2 = "(sync-activity1_complete-ct3 )";
    String test3 = "(del-activity3-complete-t4-t5 )";

    System.out.println(findActivityName(test1));
    System.out.println(findActivityName(test2));
    System.out.println(findActivityName(test3));
  }

  private static String findActivityName(String sasPlanField) {
    String[] split = sasPlanField.split("-");
    String activityName = "";
    for (int i = 1; i < split.length - 1; i++) {
      activityName += split[i];
    }

    return activityName;
  }
}
