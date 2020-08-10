package org.web.labs.inside.jerry.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

interface Node{
}

class Element implements Node{
	ElementData element;
	ArrayList<Node> children;

	public Element(ElementData element, ArrayList<Node> children) {
		this.element = element;
		this.children = children;
	}
}

class Text implements Node{
	String text;

	public Text(String text) {
		this.text = text;
	}
}

class ElementData{
	String tag_name;
	HashMap<String, String> attributes;
	
	public ElementData(String tag_name, HashMap<String, String> attributes) {
		super();
		this.tag_name = tag_name;
		this.attributes = attributes;
	}
	
	public String id(){
		return this.attributes.get("id");
	}
	
	public HashSet<String> classes(){
		String attributes = this.attributes.get("class");
		HashSet<String> clazzlist = new HashSet<String>();
		if(attributes == null){
			return clazzlist;
		}
		
		String[] classlist = attributes.split(" ");
		
		
		for(String clzz : classlist){
			clazzlist.add(clzz);
		}
		return clazzlist;
	}
}

public class DomElement {

}
