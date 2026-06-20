package theFirst;

import java.util.Hashtable;

import it.unibo.ai.rec.model.Fluent;
import it.unibo.ai.rec.model.FluentState;
import it.unibo.ai.rec.model.FluentsModel;
import it.unibo.ai.rec.model.TrendMetric;

public class QueryProbabilityMetric implements TrendMetric {

	//private static String violStateId = "viol";
	//private static String satStateId = "sat";
	private double gradient;
	private boolean countAll;
	private Hashtable<Long, Double> queryProbability;

	public QueryProbabilityMetric() {
		this(false);
	}

	public QueryProbabilityMetric(boolean countAll) {
		this(countAll, 1);
	}

	public QueryProbabilityMetric(int gradient) {
		this(false, gradient);
	}

	public QueryProbabilityMetric(boolean countAll, int gradient) {
		this.countAll = countAll;
		this.gradient = gradient;
	}

	public QueryProbabilityMetric(Hashtable<Long, Double> queryProbability) {
		this.queryProbability = queryProbability;
	}

	private boolean inState(Fluent f, String stateId, long time) {
		FluentState state = f.getStates().get(stateId);
		return state != null && state.holdsAt2(time);
	}

	@Override
	public double getValue(FluentsModel monitoringState, long time) {
		if(time == -1){
			return 1.;
		}
		return queryProbability.get(time);
	}


	@Override
	public String getName() {
		return "Query probability";
	}


}