package task.discovery.mp_enhancer;

import java.util.ArrayList;
import java.util.List;

import task.discovery.data.Predicate;

public class Cluster {
	Predicate label;
    List<String> rules;
    List<FeatureVector> elements;
    String clusterType;

    public Cluster(Predicate label, List<String> rules, List<FeatureVector> elements, String type) {
        this.label = label;
        this.rules = new ArrayList<>(rules);
        this.elements = new ArrayList<>(elements);
        this.clusterType = type;
    }

    public Cluster(Predicate label) {
        this.label = label;
        this.rules = new ArrayList<>();
        this.elements = new ArrayList<>();
        this.clusterType = null;
    }

    public Cluster() {
        this.label = null;
        this.rules = new ArrayList<>();
        this.elements = new ArrayList<>();
        this.clusterType = null;
    }
    /*
    private static Cluster[] InitializeClusters(int ClustersCount)
    {
        Cluster[] clusters = new Cluster[ClustersCount];
        for(int i = 0; i < ClustersCount; i++)
        {
            clusters[i] = new Cluster();
        }
        return clusters;
    }
	*/
    public String toString() {
        return "label: " + label + ", elements: " + elements;
    }

    public Predicate getLabel(){
        return this.label;
    }

    public void setLabel(Predicate label){
        this.label = label;
    }

    public List<String> getRules(){
        return this.rules;
    }

    public void setRules(List<String> rules){
        this.rules = rules;
    }

    public void giveLabels(){
        for (FeatureVector element : elements)
            element.label = this.label;
    }

    public List<FeatureVector> getElements(){
        return this.elements;
    }

    public void setElements(List<FeatureVector> elements){
        this.elements = elements;
    }
}