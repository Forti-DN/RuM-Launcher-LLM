package test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestRunner {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//String example = "Precedence[A1, A2] | |not (T.Type not in (T2, T3) and T.Price < 500) |";
		//System.out.println(getCondition(example,true));
		List<String> constraints = Arrays.asList("Response[A, B] |A.x < 25 |T.y == Lino", "Precedence[C, B] |A.x > 100 |T.y == Pino && A.x > 50");
		List<String> activations = new ArrayList<String>();
		List<String> correlations = new ArrayList<String>();
		obtainAttributes(constraints, activations, correlations);
		activations.addAll(correlations);
		Set<String> attributes = new HashSet<String>(activations);
		for(String s: attributes) System.out.print(s+", ");
		System.out.print("\n");
	}
	
	private static void obtainAttributes(List<String> constraints, List<String> activations, List<String> correlations) {
		Pattern pA = Pattern.compile(" ?A\\.(\\w+) (.*)");
		Pattern pT = Pattern.compile(" ?T\\.(\\w+) (.*)");
		for(String s: constraints) {
			Matcher mA = pA.matcher(s);
			Matcher mT = pT.matcher(s);
			while(mA.find()) {
				activations.add(mA.group(1));
				mA = pA.matcher(mA.group(2));
			}
			while(mT.find()) {
				correlations.add(mT.group(1));
				mT = pT.matcher(mT.group(2));
			}
		}
	}
	
	public Tuple startEnd(String str) {
		int left = str.indexOf('(');
		int right = str.indexOf(')');
		int comma = str.indexOf(',');
		if(comma != -1) {
			String start = str.substring(left+1,comma);
			String end = str.substring(comma+2, right);
			return new Tuple(start,end);
		}
		else {
			String start = str.substring(left+1,right);
			return new Tuple(start,"");
		}
	}
	
	private static String prepareActivityCondition(String s,boolean isSingleQuote) {
    	if(s.isEmpty()) return s;
    	String condition = s;
    	if(condition.contains(" = ")) condition = condition.replace("=", "==");
    	if(condition.contains(" is not ")) condition = condition.replace("is not", "!=");
    	if(condition.contains(" is ")) condition = condition.replace("is", "==");
    	if(condition.contains(" and ")) condition = condition.replace("and", "&&");
    	if(condition.contains(" or ")) condition = condition.replace("or", "||");
    	Matcher m = Pattern.compile("(.*) (<|>|<=|>=|==|!=) (.*)").matcher(condition);
    	m.find();
    	String data = m.group(3);
    	if(data.matches("^\\d+.\\d+$|^\\d+$")) {
    		return m.group(1)+" "+m.group(2)+" "+""+m.group(3)+"";
    	}
    	else if(isSingleQuote) {
    		return m.group(1)+" "+m.group(2)+" "+"'"+m.group(3)+"'";
    	}
    	else {
    		return m.group(1)+" "+m.group(2)+" "+"\""+m.group(3)+"\"";
    	}
    }
	
	private static String prepareConstraintCondition(String s,boolean isSingleQuote) {
    	if(s.isEmpty()) return s;
    	System.out.println("Ccondition: "+s);
    	String condition = "T."+s.substring(2);
    	if(condition.contains(" = ")) condition = condition.replace("=", "==");
    	if(condition.contains(" is not ")) condition = condition.replace("is not", "!=");
    	if(condition.contains(" is ")) condition = condition.replace("is", "==");
    	if(condition.contains(" and ")) condition = condition.replace("and", "&&");
    	if(condition.contains(" or ")) condition = condition.replace("or", "||");
    	Matcher m = Pattern.compile("(.*) (<|>|<=|>=|==|!=) (.*)").matcher(condition);
    	m.find();
    	String data = m.group(3);
    	if(data.matches("^\\d+.\\d+$|^\\d+$")) {
    		return m.group(1)+" "+m.group(2)+" "+""+m.group(3)+"";
    	}
    	else if(isSingleQuote) {
    		return m.group(1)+" "+m.group(2)+" "+"'"+m.group(3)+"'";
    	}
    	else {
    		return m.group(1)+" "+m.group(2)+" "+"\""+m.group(3)+"\"";
    	}
    }
	
	private static String prepareCondition(String s, boolean isSingleQuote) {
		if(s.isEmpty()) return s;
		Matcher mAnd = Pattern.compile("(.*) and (.*)").matcher(s);
		if(mAnd.find()) {
			String s2 = prepareCondition(mAnd.group(1),isSingleQuote) + " && " + prepareCondition(mAnd.group(2),isSingleQuote);
			return s2;
		}
		else {
			Matcher mOr = Pattern.compile("(.*) or (.*)").matcher(s);
			if(mOr.find()) {
				String s2 = prepareCondition(mOr.group(1),isSingleQuote) + " || " + prepareCondition(mOr.group(2),isSingleQuote);
				return s2;
			}
			else {
				Matcher mNot = Pattern.compile("^not \\((.*)\\)$").matcher(s);
				if(mNot.find()) {
					return "!("+prepareCondition(mNot.group(1),isSingleQuote)+")";
				}
				else {
				String s2 = s;
				if(s2.contains(" = ")) s2 = s2.replace("=", "==");
		    	if(s2.contains(" is not ")) s2 = s2.replace("is not", "!=");
		    	if(s2.contains(" is ")) s2 = s2.replace("is", "==");
		    	Matcher mSame = Pattern.compile("same (.*)").matcher(s2);
		    	if(mSame.find()) {
		    		s2 = "A."+mSame.group(1)+" == "+"T."+mSame.group(1);
		    	}
		    	Matcher mDiff = Pattern.compile("different (.*)").matcher(s2);
		    	if(mDiff.find()) {
		    		s2 = "A."+mDiff.group(1)+" != "+"T."+mDiff.group(1);
		    	}
		    	Matcher mNotIn = Pattern.compile("(\\w\\.\\w+) not in \\((.*)\\)").matcher(s2);
		    	if(mNotIn.find()) {
		    		String lhs = mNotIn.group(1);
		    		String[] rha = mNotIn.group(2).split(",");
		    		String ss2 = "";
		    		for(String r: rha) {
		    			if(isSingleQuote) {
		    				ss2 += lhs + " != '" + r.trim() + "' && ";
		    			}
		    			else {
		    				ss2 += lhs + " != \"" + r.trim() + "\" && ";
		    			}
		    		}
		    		s2 = ss2.substring(0, ss2.length()-4);
		    		return s2;
		    	}
		    	Matcher mIn = Pattern.compile("(\\w\\.\\w+) in \\((.*)\\)").matcher(s2);
		    	if(mIn.find()) {
		    		String lhs = mIn.group(1);
		    		String[] rha = mIn.group(2).split(",");
		    		String ss2 = "";
		    		for(String r: rha) {
		    			if(isSingleQuote) {
		    				ss2 += lhs + " == '" + r.trim() + "' || ";
		    			}
		    			else {
		    				ss2 += lhs + " == \"" + r.trim() + "\" || ";
		    			}
		    		}
		    		s2 = ss2.substring(0, ss2.length()-4);
		    		return s2;
		    	}
		    	Matcher m = Pattern.compile("(.*) (<|>|<=|>=|==|!=) (.*)").matcher(s2);
		    	m.find();
		    	String data = m.group(3);
		    	if(data.matches("^\\d+.\\d+$|^\\d+$")) {
		    		return m.group(1)+" "+m.group(2)+" "+""+m.group(3)+"";
		    	}
		    	else if(data.matches("^\\D\\..*$")) {
		    		return m.group(1)+" "+m.group(2)+" "+""+m.group(3)+"";
		    	}
		    	else if(isSingleQuote) {
		    		return m.group(1)+" "+m.group(2)+" "+"'"+m.group(3)+"'";
		    	}
		    	else {
		    		return m.group(1)+" "+m.group(2)+" "+"\""+m.group(3)+"\"";
		    	}
			}
			}
		}
	}
	
	private static String getCondition(String constraint,boolean isSingleQuote) {
		System.out.println(constraint);
    	Matcher m1 = Pattern.compile("\\|(.*) \\|(.*) \\|(.*)").matcher(constraint);
    	if(m1.find())
    		return "["+prepareCondition(m1.group(1),isSingleQuote)+"]"+"["+prepareCondition(m1.group(2),isSingleQuote)+"]"+"["+m1.group(3)+"]";
    	else {
    		m1 = Pattern.compile("\\|(.*)").matcher(constraint);
    		m1.find();
    		return "["+prepareCondition(m1.group(1),isSingleQuote)+"]"+"[]"+"[]";
    	}
	}

}
