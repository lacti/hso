package org.poolc.knight;

import java.util.List;

import org.poolc.hso.KeyField;
import org.poolc.hso.SynchronizeManager;
import org.poolc.hso.SynchronizeManager.DefaultContext;

public class Knight {
	@KeyField
	private int index;
	
	private String id;
	private String pw;
	
	private String name;
	private int portrait;
	
	private String comment;
	
	private int level;
	private int experience;
	private int ap;
	
	private int hp;
	private int str;
	private int def;
	private int agile;
	private int luck;
	
	private int rank;
	private int victory;
	private int defeat;
	
	private int armor;
	private int helmet;
	private int weapon;
	private int shield;
	private int shoes;
	private int accessary;
	
	public Knight() {
		super();
	}
	
	public static void main(String[] args) {
		SynchronizeManager.create(new DefaultContext("http://165.132.121.34/~lacti/hso", "az"));
		Knight k = new Knight();
//
		SynchronizeManager.getInstance().find(k, (String) "name", (String) "zerna");
//		System.out.println(SynchronizeManager.getInstance().create(k));
//		System.out.println(k);
		
		k.id = "lacti";
		System.out.println(SynchronizeManager.getInstance().synchronize(k, new String[] {"id"}, new String[] {"id"}));
		System.out.println(k);

//		System.out.println(SynchronizeManager.getInstance().read(k, 1));
//		System.out.println(SynchronizeManager.getInstance().delete(k));
//		System.out.println(k);
		
//		List<Knight> ks = SynchronizeManager.getInstance().list(Knight.class, "name", "zerna");
//		System.out.println(ks);
//		Knight k = new Knight();
//		SynchronizeManager.getInstance().find(k, (String) "name", (String) "zerna");
//		System.out.println(k);
	}

	@Override
	public String toString() {
		return "Knight [index=" + index + ", id=" + id + ", pw=" + pw
				+ ", name=" + name + ", portrait=" + portrait + ", comment="
				+ comment + ", level=" + level + ", experience=" + experience
				+ ", ap=" + ap + ", hp=" + hp + ", str=" + str + ", def=" + def
				+ ", agile=" + agile + ", luck=" + luck + ", rank=" + rank
				+ ", victory=" + victory + ", defeat=" + defeat + ", armor="
				+ armor + ", helmet=" + helmet + ", weapon=" + weapon
				+ ", shield=" + shield + ", shoes=" + shoes + ", accessary="
				+ accessary + "]";
	}
}
