package theFirst;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import controller.monitoring.MonitoringMethod;

public class ServerRunner implements Runnable{
	private Thread t;
	private String threadName;
	private MonitoringMethod monitoringMethod;

	public ServerRunner(String name, MonitoringMethod monitoringMethod){
		threadName = name;
		this.monitoringMethod = monitoringMethod;
		System.out.println("Creating ServerRunner Thread");
	}
	@Override
	public void run() {
		String cmd = null;
		
		switch (monitoringMethod) {
		case MP_DECLARE_ALLOY:
			System.out.println("Server runner is not intended to be used with " + monitoringMethod.name());
			break;
		case MOBUCON_LTL:
			cmd = "java -jar MPMoBuConLTL.jar ";
			break;
		case ONLINE_DECLARE:
			cmd = "java -jar OnlineDeclareAnalyzerPlugin.jar ";
			break;
		case FLLOAT:
			cmd = "java -jar MPFlloatAutomaton.jar ";
			break;
		case PROBDECLARE:
			cmd = "java -jar MP_LTL2Automaton.jar";
			break;
		default:
			System.out.println("Unknown monitoring method");
			break;
		}
		
        try {
        	ProcessBuilder pb = new ProcessBuilder(cmd.split(" "));        	
    		Process p = pb.start();
    		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
    		String line = null;
    		while ( (line = reader.readLine()) != null) {
    			System.out.println(line);
    		}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println(e);
		}		
	}
	public void start () {
		System.out.println("Starting " +  threadName );
		if (t == null) {
			t = new Thread (this, threadName);
		    t.start ();
		    }
		}
}
