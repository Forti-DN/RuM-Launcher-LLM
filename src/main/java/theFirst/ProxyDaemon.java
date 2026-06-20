package theFirst;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import controller.monitoring.MonitoringMethod;
import theFirst.MoBuConClient.ProxySimulator;

import ltl2aut.formula.conjunction.BlockingQueue;


public class ProxyDaemon extends Thread {
    private ServerSocket s;

    //private ProxyDaemon defaultInstance;
    private MoBuConClient client;
    private MonitoringMethod monitoringMethod;
//	public synchronized static ProxyDaemon getDefaultInstance() throws Exception {
//		if (defaultInstance == null) {
//			defaultInstance = new ProxyDaemon(2098, null);
//			defaultInstance.start();
//		}
//		return defaultInstance;
//	}

    public ProxyDaemon(final int port, MoBuConClient client, MonitoringMethod monitoringMethod) throws Exception {
        super("Proxy Simulator (port = " + port + ")");
        setDaemon(true);
        simulators = new BlockingQueue<ProxySimulator>();
        this.client = client;
        this.monitoringMethod = monitoringMethod;
        try {
            s = new ServerSocket(port);
        } catch (final Exception e) {
            throw new Exception(
                    "Could not start Proxy Daemon. Most likely CPN Tools is running. Try shutting down CPN Tools (including any cpnmld.* processes) and try again.");
        }
    }

    private final BlockingQueue<ProxySimulator> simulators;

    /**
     * Get the next ProxySimulator connecting
     *
     * @return
     */
    public ProxySimulator getNext() {
        return simulators.get();
    }

    /**
     * Clear the queue of ProxySimulators. Only ensures that at some point after calling the queue
     * will be empty. Suggested use: clear the queue, display a message to the user to connect,
     * getNext to (most likely) get the one the user connected.
     */
    public void clear() {
        while (simulators.size() > 0) {
            simulators.get();
        }
    }



    @Override
    public void run() {
        while (true) {
            try {
                final Socket socket = s.accept();
                final InputStream inStream = socket.getInputStream();
                final OutputStream outStream = socket.getOutputStream();
                socket.setTcpNoDelay(true);
                socket.setReceiveBufferSize(1);
                ProxySimulator xydataset = client.new ProxySimulator(socket, outStream, inStream, monitoringMethod);
                xydataset.start();
            } catch (SocketException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }
}