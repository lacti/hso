package org.poolc.knight;

import org.poolc.hso.KeyField;

public class Item {

	@KeyField
	private int index;
	
	private int type;
	private int image;
	private String name;

	private int hp;
	private int str;
	private int def;
	private int agile;
	private int luck;
	
	private int price;
	
	public Item() {
		super();
	}

	@Override
	public String toString() {
		return "Item [index=" + index + ", type=" + type + ", image=" + image
				+ ", name=" + name + ", hp=" + hp + ", str=" + str + ", def="
				+ def + ", agile=" + agile + ", luck=" + luck + ", price="
				+ price + "]";
	}
}
