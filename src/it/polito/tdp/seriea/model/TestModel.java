package it.polito.tdp.seriea.model;

public class TestModel {

	public static void main(String[] args) {
		
		Model model = new Model();
		
		Team t = new Team("Cesena");
		
		System.out.println(model.getSeasonsByTeam(t));
		
		System.out.println("");
		System.out.println(model.findBestSeason(t));
		
		System.out.println(model.findBestPath(t));
	}

}
