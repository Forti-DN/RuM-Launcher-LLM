package theFirst;

public class InstanceInfo {

    private String id;
    private boolean violated;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isViolated() {
        return violated;
    }

    public void setViolated(boolean violated) {
        this.violated = violated;
    }

	@Override
	public String toString() {
		return id;
	}

}
