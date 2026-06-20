package task;

import controller.ConfigurationMPDeclareController;
import org.processmining.plugins.declareminer.visualizing.DeclareMinerOutput;
import javafx.concurrent.Task;

public class DiscoverMPDeclareTask extends Task<DeclareMinerOutput> {
    private ConfigurationMPDeclareController controller;

    public DiscoverMPDeclareTask(ConfigurationMPDeclareController controller) {
        this.controller = controller;
    }
    @Override
    protected DeclareMinerOutput call() throws Exception {   //DeclareMinerOutput , MinerfulResult
        return controller.getDiscoveryResult();
    }

}



