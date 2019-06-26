package it.polito.tdp.seriea.model;

public class TestModel {

	public static void main(String[] args) {
		
		Model model = new Model();
		
		Team t = new Team("Juventus");
		
		System.out.println(model.getSeasonsByTeam(t));
	}

}
