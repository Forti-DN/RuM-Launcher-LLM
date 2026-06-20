package controller.editor;

public class Constraint {
    String relation;
    String activity_one;
    String activity_two;

    public Constraint(String relation, String activity_one, String activity_two) {
        this.relation = relation;
        this.activity_one = activity_one;
        this.activity_two = activity_two;
    }

    public String getRelation() {
        return relation;
    }

    public String getActivity_one() {
        return activity_one;
    }

    public String getActivity_two() {
        return activity_two;
    }

    @Override
    public String toString() {
        return "Constraint{" +
                "relation='" + relation + '\'' +
                ", activity_one='" + activity_one + '\'' +
                ", activity_two='" + activity_two + '\'' +
                '}';
    }
}
