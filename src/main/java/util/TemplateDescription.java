package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TemplateDescription {

	
	public static String get(ConstraintTemplate template, String... params) {
		String description = "";
		
		switch(template) {
		case Absence:
			description = String.format("%s does not occur", params[0]);
			break;
		case Absence2:
			description = String.format("%s occurs at most once", params[0]);
			break;
		case Absence3:
			description = String.format("%s occurs at most twice", params[0]);
			break;
		case Exactly1:
			description = String.format("%s occurs exactly once", params[0]);
			break;
		case Exactly2:
			description = String.format("%s occurs exactly twice", params[0]);
			break;
		case Existence:
			description = String.format("%s occurs at least once", params[0]);
			break;
		case Existence2:
			description = String.format("%s occurs at least twice", params[0]);
			break;
		case Existence3:
			description = String.format("%s occurs at least three times", params[0]);
			break;
		case Init:
			description = String.format("%s occurs first", params[0]);
			break;
		case End:
			description = String.format("%s occurs last", params[0]);
			break;
		case Responded_Existence:
			description = String.format("If %s occurs then %s occurs as well", params[0], params[1]);
			break;
		case Response:
			description = String.format("If %s occurs then %s occurs after %s", params[0], params[1], params[0]);
			break;
		case Alternate_Response:
			description = String.format("Each time %s occurs, then %s occurs afterwards before %s recurs", params[0], params[1], params[0]);
			break;
		case Chain_Response:
			description = String.format("Each time %s occurs, then %s occurs immediately afterwards", params[0], params[1]);
			break;
		case Precedence:
			description = String.format("%s occurs if preceded by %s", params[0], params[1]);
			break;
		case Alternate_Precedence:
			description = String.format("Each time %s occurs, it is preceded by %s and no other %s can recur in between", params[0], params[1], params[0]);
			break;
		case Chain_Precedence:
			description = String.format("Each time %s occurs, then %s occurs immediately beforehand", params[0], params[1]);
			break;
		case CoExistence:
			description = String.format("%s and %s occur together", params[0], params[1]);
			break;
		case Succession:
			description = String.format("%s occurs if and only if it is followed by %s", params[0], params[1]);
			break;
		case Alternate_Succession:
			description = String.format("%s and %s occur together if and only if the latter follows the former, and they alternate each other", params[0], params[1]);
			break;
		case Chain_Succession:
			description = String.format("%s and %s occur together if and only if the latter immediately follows the former", params[0], params[1]);
			break;
		case Choice:
			description = String.format("%s or %s must occur at least once", params[0], params[1]);
			break;
		case Exclusive_Choice:
			description = String.format("%s or %s must occur at least once and they exclude each other", params[0], params[1]);
			break;
		case Not_Chain_Succession:
			description = String.format("%s and %s occur together if and only if the latter does not immediately follow the former", params[0], params[1]);
			break;
		case Not_Succession:
			description = String.format("%s can never occur before %s", params[0], params[1]);
			break;
		case Not_CoExistence:
			description = String.format("%s and %s never occur together", params[0], params[1]);
			break;
		default:
			description = "Not defined";
		}
		
		return description;
	}
	
	public static String getWithExamples(ConstraintTemplate template, String... params) {
		// Unused letters then used to build examples
		List<String> l = new ArrayList<>(Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"));
		
		for (String p : params)
			l.remove(p);
		
		String description = get(template, params) + "\n";
		String posEx = "";
		String negEx = "";
		
		switch(template) {
		case Absence:
			posEx = "Positive examples: " // BC, BBCCDD
					+ l.get(0) + l.get(1) + ", "
					+ l.get(0) + l.get(0) + l.get(1) + l.get(1) + l.get(2) + l.get(2) + "\n"; 
			negEx = "Negative examples: " // A, BCDAA
					+ params[0] + ", "
					+ l.get(0) + l.get(1) + l.get(2) + params[0] + params[0];
			
			break;
		case Absence2:
			posEx = "Positive examples: " // BA, BBCCDA
					+ l.get(0) + params[0] + ", "
					+ l.get(0) + l.get(0) + l.get(1) + l.get(1) + l.get(2) + params[0] + "\n"; 
			negEx = "Negative examples: " // AA, BCDAAA
					+ params[0] + params[0] + ", "
					+ l.get(0) + l.get(1) + l.get(2) + params[0] + params[0] + params[0];
			
			break;
		case Absence3:
			posEx = "Positive examples: " // BA, BBCCDAA
					+ l.get(0) + params[0] + ", "
					+ l.get(0) + l.get(0) + l.get(1) + l.get(1) + l.get(2) + params[0] + params[0] + "\n"; 
			negEx = "Negative examples: " // AAA, ABCDAAA
					+ params[0] + params[0] + params[0] + ", "
					+ params[0] + l.get(0) + l.get(1) + l.get(2) + params[0] + params[0] + params[0];
			
			break;
		case Exactly1:
			posEx = "Positive examples: " // BA, BBCCDA
					+ l.get(0) + params[0] + ", "
					+ l.get(0) + l.get(0) + l.get(1) + l.get(1) + l.get(2) + params[0] + "\n"; 
			negEx = "Negative examples: " // AAA, ABCDA, EDBC
					+ params[0] + params[0] + params[0] + ", "
					+ params[0] + l.get(0) + l.get(1) + l.get(2) + params[0] + ", "
					+ l.get(3) + l.get(2) + l.get(0) + l.get(1);
			
			break;
		case Exactly2:
			posEx = "Positive examples: " // BAA, BBCCDAA
					+ l.get(0) + params[0] + params[0] + ", "
					+ l.get(0) + l.get(0) + l.get(1) + l.get(1) + l.get(2) + params[0] + params[0] + "\n"; 
			negEx = "Negative examples: " // AAA, ABCD, EDBC
					+ params[0] + params[0] + params[0] + ", "
					+ params[0] + l.get(0) + l.get(1) + l.get(2) + ", "
					+ l.get(3) + l.get(2) + l.get(0) + l.get(1);
			
			break;
		case Existence:
			posEx = "Positive examples: " // BAA, BBCCDA
					+ l.get(0) + params[0] + params[0] + ", "
					+ l.get(0) + l.get(0) + l.get(1) + l.get(1) + l.get(2) + params[0] + "\n";
			negEx = "Negative examples: " // BCC, BCD, EDBC
					+ l.get(0) + l.get(1) + l.get(1) + ", "
					+ l.get(0) + l.get(1) + l.get(2) + ", "
					+ l.get(3) + l.get(2) + l.get(0) + l.get(1);
			
			break;
		case Existence2:
			posEx = "Positive examples: " // BAA, BBCCDAAA
					+ l.get(0) + params[0] + params[0] + ", "
					+ l.get(0) + l.get(0) + l.get(1) + l.get(1) + l.get(2) + params[0] + params[0] + params[0] + "\n"; 
			negEx = "Negative examples: " // BCCA, BCD, EDBC
					+ l.get(0) + l.get(1) + l.get(1) + params[0] + ", "
					+ l.get(0) + l.get(1) + l.get(2) + ", "
					+ l.get(3) + l.get(2) + l.get(0) + l.get(1);
			
			break;
		case Existence3:
			posEx = "Positive examples: " // BAAA, BABCCDAAA
					+ l.get(0) + params[0] + params[0] + params[0] + ", "
					+ l.get(0) + params[0] + l.get(0) + l.get(1) + l.get(1) + l.get(2) + params[0] + params[0] + params[0] + "\n"; 
			negEx = "Negative examples: " // BCCA, BAACD, EDBC
					+ l.get(0) + l.get(1) + l.get(1) + params[0] + ", "
					+ l.get(0) + params[0] + params[0] + l.get(1) + l.get(2) + ", "
					+ l.get(3) + l.get(2) + l.get(0) + l.get(1);
			
			break;
		case Init:
			posEx = "Positive examples: " // AA, ABCCDAAA
					+ params[0] + params[0] + ", "
					+ params[0] + l.get(0) + l.get(1) + l.get(1) + l.get(2) + params[0] + params[0] + params[0] + "\n"; 
			negEx = "Negative examples: " // BCCA, BAACD, EDBC
					+ l.get(0) + l.get(1) + l.get(1) + params[0] + ", "
					+ l.get(0) + params[0] + params[0] + l.get(1) + l.get(2) + ", "
					+ l.get(3) + l.get(2) + l.get(0) + l.get(1);
			
			break;
		case End:
			posEx = "Positive examples: " // BCA, DCEAA
					+ l.get(0) + l.get(1) + params[0] + ", "
					+ l.get(2) + l.get(1) + l.get(3) + params[0] + params[0];
			negEx = "Negative examples: " // CADD, ABCD
					+ l.get(1) + params[0] + l.get(2) + l.get(2) + ", "
					+ params[0] + l.get(0) + l.get(1) + l.get(2);
			
			break;
		case Responded_Existence:
			posEx = "Positive examples: " // AB, ABCCDAAA
					+ params[0] + params[1] + ", "
					+ params[0] + params[1] + l.get(0) + l.get(0) + l.get(1) + params[0] + params[0] + params[0] + "\n"; 
			negEx = "Negative examples: " // CCA, AACD, EDAAAC
					+ l.get(0) + l.get(0) + params[0] + ", "
					+ params[0] + params[0] + l.get(0) + l.get(1) + ", "
					+ l.get(2) + l.get(1) + params[0] + params[0] + params[0] + l.get(0);
			
			break;
		case Response:
			posEx = "Positive examples: " // ABCD, AAAAB, BCCD
					+ params[0] + params[1] + l.get(0) + l.get(1) + ", "
					+ params[0] + params[0] + params[0] + params[1] + ", "
					+ params[1] + l.get(0) + l.get(0) + l.get(1) + "\n"; 
			negEx = "Negative examples: " // CCA, AACD, EDAAAC
					+ l.get(0) + l.get(0) + params[0] + ", "
					+ params[0] + params[0] + l.get(0) + l.get(1) + ", "
					+ l.get(2) + l.get(1) + params[0] + params[0] + params[0] + l.get(0);
			
			break;
		case Alternate_Response:
			posEx = "Positive examples: " // ABCADB ,ACDEB
					+ params[0] + params[1] + l.get(0) + params[0] + l.get(1) + params[1] + ", "
					+ params[0] + l.get(0) + l.get(1) + l.get(2) + params[1] + "\n"; 
			negEx = "Negative examples: " // CCAA, AABCD, EDBAAAC
					+ l.get(1) + l.get(1) + params[0] + params[0] + ", "
					+ params[0] + params[0] + params[1] + l.get(0) + l.get(1) + ", "
					+ l.get(2) + l.get(1) + params[1] + params[0] + params[0] + params[0] + l.get(0);
			
			break;
		case Chain_Response:
			posEx = "Positive examples: " // ABCAB, CDEAB
					+ params[0] + params[1] + l.get(0) + params[0] + params[1] + ", "
					+ l.get(0) + l.get(1) + l.get(2) + params[0] + params[1] + "\n"; 
			negEx = "Negative examples: " // CCAACB, EDBAAAC
					+ l.get(1) + l.get(1) + params[0] + params[0] + l.get(0) + params[1] + ", "
					+ l.get(2) + l.get(1) + params[1] + params[0] + params[0] + params[0] + l.get(0);
			
			break;
		case Precedence:
			posEx = "Positive examples: " // ABCD, AAAAB, AACCD
					+ params[1] + params[0] + l.get(0) + l.get(1) + ", "
					+ params[1] + params[1] + params[1] + params[1] + params[0] + ", "
					+ params[1] + params[1] + l.get(0) + l.get(0) + l.get(1) + "\n"; 
			negEx = "Negative examples: " // CCBA, BBCD, EDBAC
					+ l.get(0) + l.get(0) + params[0] + params[1] + ", "
					+ params[0] + params[0] + l.get(0) + l.get(1) + ", "
					+ l.get(2) + l.get(1) + params[0] + params[1] + l.get(0);
			
			break;
		case Alternate_Precedence:
			posEx = "Positive examples: " // ABCD ,ABACAAB, AACCD
					+ params[1] + params[0] + l.get(0) + l.get(1) + ", "
					+ params[1] + params[0] + params[1] + l.get(0) + params[1] + params[1] + params[0] + ", "
					+ params[1] + params[1] + l.get(0) + l.get(0) + l.get(1) + "\n"; 
			negEx = "Negative examples: " // CACBBA, ABBABCB
					+ l.get(0) + params[1] + l.get(0) + params[0] + params[0] + params[1] + ", "
					+ params[1] + params[0] + params[0] + params[1] + params[0] + l.get(0) + params[0];
			
			break;
		case Chain_Precedence:
			posEx = "Positive examples: " // ABCABAA ,CDEABAB
					+ params[1] + params[0] + l.get(0) + params[1] + params[0] + params[1] + params[1] + ", "
					+ l.get(0) + l.get(1) + l.get(2) + params[1] + params[0] + params[1] + params[0] + "\n"; 
			negEx = "Negative examples: " // CCAACBB, EDBAAAC
					+ l.get(1) + l.get(1) + params[1] + params[1] + l.get(0) + params[0] + params[0] + ", "
					+ l.get(2) + l.get(1) + params[0] + params[1] + params[1] + params[1] + l.get(0);
			
			break;
		case CoExistence:
			posEx = "Positive examples: " // AB, ABCCDAAA, CDE
					+ params[0] + params[1] + ", "
					+ params[0] + params[1] + l.get(0) + l.get(0) + l.get(1) + params[1] + params[1] + params[1] + ", "
					+ l.get(0) + l.get(1) + l.get(2) + "\n"; 
			negEx = "Negative examples: " // CCA, BBCD, EDAAAC
					+ l.get(0) + l.get(0) + params[0] + ", "
					+ params[1] + params[1] + l.get(0) + l.get(1) + ", "
					+ l.get(2) + l.get(1) + params[0] + params[0] + params[0] + l.get(0);
			
			break;
		case Succession:
			posEx = "Positive examples: " // AB, ABCCDBB
					+ params[0] + params[1] + ", "
					+ params[0] + params[1] + l.get(0) + l.get(0) + l.get(1) + params[0] + params[0] + "\n"; 
			negEx = "Negative examples: " // BCCA, BBCD, EDBAC
					+ params[1] + l.get(0) + l.get(0) + params[0] + ", "
					+ params[1] + params[1] + l.get(0) + l.get(1) + ", "
					+ l.get(2) + l.get(1) + params[1] + params[0] + l.get(0);
			
			break;
		case Alternate_Succession:
			posEx = "Positive examples: " // ACDBACB, ABCCABD
					+ params[0] + l.get(0) + l.get(1) + params[1] + params[0] + l.get(0) + params[1] + ", " 
					+ params[0] + params[1] + l.get(0) + l.get(0) + params[0] + params[1] + l.get(1) + "\n"; 
			negEx = "Negative examples: " // AABCCA, BBCDAA, EDBAC
					+ params[0] + params[0] + params[1] + l.get(0) + l.get(0) + params[0] + ", "
					+ params[1] + params[1] + l.get(0) + l.get(1) + params[0] + params[0] + ", "
					+ l.get(2) + l.get(1) + params[1] + params[0] + l.get(0);
			
			break;
		case Chain_Succession:
			posEx = "Positive examples: " // ABABCC, CCD
					+ params[0] + params[1] + params[0] + params[1] + l.get(0) + l.get(0) + ", "
					+ l.get(0) + l.get(0) + l.get(1) + "\n"; 
			negEx = "Negative examples: " // AABCCA, BBCDAA, EDBAC
					+ params[0] + params[0] + params[1] + l.get(0) + l.get(0) + params[0] + ", "
					+ params[1] + params[1] + l.get(0) + l.get(1) + params[0] + params[0] + ", "
					+ l.get(2) + l.get(1) + params[1] + params[0] + l.get(0);
			
			break;
		case Choice:
			posEx = "Positive examples: " // CADAAC, BCD, ABCBBA
					+ l.get(0) + params[0] + l.get(1) + params[0] + params[0] + l.get(0) + ", "
					+ params[1] + l.get(0) + l.get(1) + ", "
					+ params[0] + params[1] + l.get(0) + params[1] + params[1] + params[0] + "\n"; 
			negEx = "Negative examples: " // CCDE, DEC
					+ l.get(0) + l.get(0) + l.get(1) + l.get(2) + ", "
					+ l.get(1) + l.get(2) + l.get(0);
			
			break;
		case Exclusive_Choice:
			posEx = "Positive examples: " // CADAAC, BCD
					+ l.get(0) + params[0] + l.get(1) + params[0] + params[0] + l.get(0) + ", "
					+ params[1] + l.get(0) + l.get(1) + "\n"; 
			negEx = "Negative examples: " // ABCBBA, DEC
					+ params[0] + params[1] + l.get(0) + params[1] + params[1] + params[0] + ", "
					+ l.get(1) + l.get(2) + l.get(0);
			
			break;
		case Not_Chain_Succession:
			posEx = "Positive examples: " // ABBAABCC, BBCCD
					+ params[0] + params[1] + params[1] + params[0] + params[0] + params[1] + l.get(0) + l.get(0) + ", "
					+ params[1] + params[1] + l.get(0) + l.get(0) + l.get(1) + "\n"; 
			negEx = "Negative examples: " // ABCC, ABABCD
					+ params[0] + params[1] + l.get(0) + l.get(0) + ", "
					+ params[0] + params[1] + params[0] + params[1] + l.get(0) + l.get(1);
			
			break;
		case Not_Succession:
			posEx = "Positive examples: " // BBAACC, BBCCBAD
					+ params[1] + params[1] + params[0] + params[0] + l.get(0) + l.get(0) + ", "
					+ params[1] + params[1] + l.get(0) + l.get(0) + params[1] + params[0] + l.get(1) + "\n"; 
			negEx = "Negative examples: " // AABBCC, ABBCD
					+ params[0] + params[0] + params[1] + params[1] + l.get(0) + l.get(0) + ", "
					+ params[0] + params[1] + params[1] + l.get(0) + l.get(1);
			
			break;
		case Not_CoExistence:
			posEx = "Positive examples: " // AACC, BBCCBD, CDE
					+ params[0] + params[0] + l.get(0) + l.get(0) + ", "
					+ params[1] + params[1] + l.get(0) + l.get(0) + params[1] + l.get(1) + ", "
					+ l.get(0) + l.get(1) + l.get(2) + "\n"; 
			negEx = "Negative examples: " // AABCC, ABBBCD
					+ params[0] + params[0] + params[1] + l.get(0) + l.get(0) + ", "
					+ params[0] + params[1] + params[1] + params[1] + l.get(0) + l.get(1);
			
			break;
		default:
			break;
		}
				
		return description + posEx + negEx;
	}
}
