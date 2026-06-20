package theFirst;

public class CheckDeclareReplayer {
	
	public static void main(String[] args) throws Exception{
		for(int i=10; i<=100; i = i+10) {
			System.out.println(getColorFrom(i/100.0, 1));
		}
		
		/*String[] contributed= {"A","B"};
		String[] solved= {"A","B"};
		String result = "";
		if (contributed.length+solved.length==0)
			result = "";
		String[] contributed2=new String[contributed.length+solved.length];
		int j=0;
		for(String x : contributed)
			contributed2[j++]=x;
		for(String x : solved)
			contributed2[j++]=x;		
		StringBuffer sb=new StringBuffer("<html><table border=\"1\">");
		if (contributed2.length>0)
		{
			sb.append("<tr><td rowspan=\"");
			sb.append(contributed2.length);
			sb.append("\"><b>Contributes to Solve:</b></td><td>"+contributed2[0]+"</td></tr>");
			for(int i=1;i<contributed2.length;i++)
			{
				sb.append("<tr><td style=\"border: none\">");
				sb.append(contributed2[i]);
				sb.append("</td></tr>");
			}
		}
		result = sb.toString();
		System.out.println(result);
		/*ResultReplayDeclare rrd = Replayer.run(args[0], args[1]);
		XLog alignedLog = rrd.getAlignedLog();
		List<String> traceList = new ArrayList<String>();
		for(int i=0; i<alignedLog.size(); i++) {
			XTrace xt = alignedLog.get(i);
			traceList.add(xt.getAttributes().get("concept:name").toString());
		}
		for(String t: traceList) {
			System.out.println(t);
			Alignment alignment = rrd.getAlignmentByTraceName(t);
			List<DataAwareStepTypes> steps = alignment.getStepTypes();
			for(int i=0; i<steps.size(); i++) {
				String step = steps.get(i).toString();
				if(step.endsWith("Log")) {
					System.out.print(alignment.getLogTrace().get(i).getActivity()+"-LC ");
				}
				else if(step.endsWith("Model")) {
					System.out.print(alignment.getProcessTrace().get(i).getActivity()+"-MC ");
				}
				else if(step.endsWith("Both")) {
					System.out.print(alignment.getLogTrace().get(i).getActivity()+"-BC ");
				}
			}
			System.out.println();
		}*/
	}
	
	private static String getHexValue(long value) {
		long b1 = value / 16;
		long b2 = value % 16;
		String s = "";
		if(b1 == 0) s = "0";
		if(b1 == 1) s = "1";
		if(b1 == 2) s = "2";
		if(b1 == 3) s = "3";
		if(b1 == 4) s = "4";
		if(b1 == 5) s = "5";
		if(b1 == 6) s = "6";
		if(b1 == 7) s = "7";
		if(b1 == 8) s = "8";
		if(b1 == 9) s = "9";
		if(b1 == 10) s = "a";
		if(b1 == 11) s = "b";
		if(b1 == 12) s = "c";
		if(b1 == 13) s = "d";
		if(b1 == 14) s = "e";
		if(b1 == 15) s = "f";
		
		if(b2 == 0) s += "0";
		if(b2 == 1) s += "1";
		if(b2 == 2) s += "2";
		if(b2 == 3) s += "3";
		if(b2 == 4) s += "4";
		if(b2 == 5) s += "5";
		if(b2 == 6) s += "6";
		if(b2 == 7) s += "7";
		if(b2 == 8) s += "8";
		if(b2 == 9) s += "9";
		if(b2 == 10) s += "a";
		if(b2 == 11) s += "b";
		if(b2 == 12) s += "c";
		if(b2 == 13) s += "d";
		if(b2 == 14) s += "e";
		if(b2 == 15) s += "f";
		
		return s;
	}
	
	private static String getColorFrom(double supp,int size) {
		double res = 51 + 26 * (1-supp) * 27.46;
		double portion = 1.5 / (size+1.5); 
		String color = "";
		if(res > 255) {
			long remaining = Math.round((res - 255) / 2);
			color = "#"+getHexValue(remaining)+getHexValue(remaining)+"ff";
		}
		else {
			color = "#0000"+getHexValue(Math.round(res));
		}
		String fc = "#e6e600";
		return "fillcolor=\""+color+";"+portion+":#808080\" gradientangle=90 fontcolor=\""+fc+"\"";
	}

}
