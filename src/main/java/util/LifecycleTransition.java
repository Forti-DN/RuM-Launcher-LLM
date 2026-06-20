package util;

public enum LifecycleTransition {
 complete,
 assign,
 ate_abort,
 suspend,
 autoskip,
 manualskip,
 pi_abort,
 reassign,
 resume,
 schedule,
 start,
 unknown,
 withdraw;
 
 public static boolean isEndingValid(String s) {
	 for(LifecycleTransition l: values()) {
		if(l.name().equals(s.toLowerCase())) return true; 
	 }
	 return false;
 }

}
