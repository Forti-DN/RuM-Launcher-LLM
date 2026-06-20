package util;

import java.util.List;

public class TraceElement {
	
	private String color;
	
	private String text;
	
	private List<String> attributes;
	
	public TraceElement(String color, String text) {
		this.color = color;
		this.text = text;
	}
	
	public TraceElement() {
		
	}
	
	public void setColor(String color) {
		this.color = color;
	}
	public void setText(String text) {
		this.text = text;
	}
	public String getColor() {
		return color;
	}
	public String getText() {
		return text;
	}
	public void setAttributes(List<String> attributes) {
		this.attributes = attributes;
	}
	public List<String> getAttributes() {
		return attributes;
	}

}
