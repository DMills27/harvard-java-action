package com.realdecoy.util;

public class IdUtil {
	
	private int id = 1;
	
	public String getId() {
		int id = this.id;
		this.id += 1;
		return String.valueOf(id); 
	}
	
}
