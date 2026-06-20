package util;

import java.util.ArrayList;
import java.util.List;

import org.processmining.plugins.declareminer.enumtypes.DeclareTemplate;

public class Templates {
	
	public static List<String> getMinerFulTemplates() {
		List<String> templates = new ArrayList<String>();
		for(DeclareTemplate dt : DeclareTemplate.values()) {
			if(dt != DeclareTemplate.Choice && 
					dt != DeclareTemplate.Exclusive_Choice && 
					dt != DeclareTemplate.Absence &&
					dt != DeclareTemplate.Absence3 &&
					dt != DeclareTemplate.Existence2 &&
					dt != DeclareTemplate.Existence3 &&
					dt != DeclareTemplate.Exactly2 &&
					dt != DeclareTemplate.Not_Chain_Precedence &&
					dt != DeclareTemplate.Not_Chain_Response && 
					dt != DeclareTemplate.Not_Precedence &&
					dt != DeclareTemplate.Not_Response &&
					dt != DeclareTemplate.Not_Responded_Existence) {
				String tname = getModelName(dt.name()).replace('_', ' ');
				templates.add(tname);
			}
		}
		templates.add("End[A]");
		return templates;
	}
	
	private static String getModelName(String template) {
		DeclareTemplate d = DeclareTemplate.valueOf(template);
		switch(d) {
			case Absence2:
				return "AtMost1[A]";
			case Existence:
				return "AtLeast1[A]";
			case Init:
				return "Init[A]";
			case Exactly1:
				return "Exactly1[A]";
			case CoExistence:
				return "Co-Existence[A, B]";
			case Not_CoExistence:
				return "Not Co-Existence[A, B]";
			default:
				return template+"[A, B]";
		}
	}
	
	public static boolean isParentOf(String selected, String discovered) {
		switch(selected) {
		case "AtLeast1":
			return discovered.equals("Init") 
					|| discovered.equals("Existence") 
					|| discovered.equals("End") 
					|| discovered.equals("Exactly1");
			
		case "Responded Existence":
			return discovered.equals("Responded Existence") 
					|| discovered.equals("Response")
					|| discovered.equals("Alternate Response")
					|| discovered.equals("Chain Response")
					|| discovered.equals("Co-Existence")
					|| discovered.equals("Succession")
					|| discovered.equals("Alternate Succession")
					|| discovered.equals("Chain Succession");
			
		case "Response":
			return discovered.equals("Response")
					|| discovered.equals("Alternate Response")
					|| discovered.equals("Chain Response")
					|| discovered.equals("Succession")
					|| discovered.equals("Alternate Succession")
					|| discovered.equals("Chain Succession");
			
		case "Alternate Response":
			return discovered.equals("Alternate Response")
					|| discovered.equals("Chain Response")
					|| discovered.equals("Alternate Succession")
					|| discovered.equals("Chain Succession");
			
		case "Chain Response":
			return discovered.equals("Chain Response")
					|| discovered.equals("Chain Succession");
			
		case "Precedence":
			return discovered.equals("Precedence")
					|| discovered.equals("Alternate Precedence")
					|| discovered.equals("Chain Precedence")
					|| discovered.equals("Succession")
					|| discovered.equals("Alternate Succession")
					|| discovered.equals("Chain Succession");
			
		case "Alternate Precedence":
			return discovered.equals("Alternate Precedence")
					|| discovered.equals("Chain Precedence")
					|| discovered.equals("Alternate Succession")
					|| discovered.equals("Chain Succession");
			
		case "Chain Precedence":
			return discovered.equals("Chain Precedence")
					|| discovered.equals("Chain Succession");
			
		case "Co-Existence":
			return discovered.equals("Co-Existence")
					|| discovered.equals("Succession")
					|| discovered.equals("Alternate Succession")
					|| discovered.equals("Chain Succession");
			
		case "Succession":
			return discovered.equals("Succession")
					|| discovered.equals("Alternate Succession")
					|| discovered.equals("Chain Succession");
			
		case "Alternate Succession":
			return discovered.equals("Alternate Succession")
					|| discovered.equals("Chain Succession");
			
		case "Not Chain Succession":
			return discovered.equals("Not Co-Existence")
					|| discovered.equals("Not Succession")
					|| discovered.equals("Not Chain Succession");
			
		case "Not Succession":
			return discovered.equals("Not Co-Existence")
					|| discovered.equals("Not Succession");
			
		default:
			return selected.equals(discovered);
		}
	}

}
