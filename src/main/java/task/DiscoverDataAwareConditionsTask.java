package task;

import javafx.concurrent.Task;
import task.discovery.mp_enhancer.DeclareRulesDataEnhancer;

public class DiscoverDataAwareConditionsTask extends Task<String> {

    private DeclareRulesDataEnhancer enhancer;

    public DiscoverDataAwareConditionsTask (DeclareRulesDataEnhancer enhancer) {
        this.enhancer = enhancer;
    }

    @Override
    protected String call() throws Exception {
        String MPDeclareResults = enhancer.addDataAwareConditions();
        return MPDeclareResults;
    }
}
