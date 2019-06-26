package it.polito.tdp.seriea.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import it.polito.tdp.seriea.db.SerieADAO;

public class Model {
	
	private SerieADAO dao;
	private Map<Integer, Season> seasonsIdMap;
	private Map<String, Team> teamsIdMap;
	
	public Model() {
		this.dao = new SerieADAO();
		this.seasonsIdMap = dao.getSeasonsMap();
		this.teamsIdMap = dao.getTeamsMap();
	}

	public List<Team> getTeamsList() {
		return dao.listTeams();
	}
	
	public List<SeasonByTeam> getSeasonsByTeam(Team team) {
		List<SeasonByTeam> result = new ArrayList<>();
		List<Match> matches = dao.listMatchesByTeam(team, seasonsIdMap, teamsIdMap);
		
		int points = 0;
		Season season = null;
		
		for(int i = 0; i < matches.size(); i ++) {
			Match m = matches.get(i);
			
			if(i == 0) 
				season = m.getSeason();
			
			else if(!m.getSeason().equals(matches.get(i-1).getSeason()) || i == matches.size()-1) {
				SeasonByTeam temp = new SeasonByTeam(team, season, points);
				result.add(temp);
				
				season = m.getSeason();
				points = 0;
			}
			
			if(m.getHomeTeam().equals(team)) {
				switch (m.getFtr()) {
					case "H":
						points += 3;
						break;
					case "D":
						points += 1;
						break;
					default:
						break;
				}	
			} else if(m.getAwayTeam().equals(team)) {
				switch (m.getFtr()) {
					case "A":
						points += 3;
						break;
					case "D":
						points += 1;
						break;
					default:
						break;
				}	
			}
		}
		
		return result;
	}
}
