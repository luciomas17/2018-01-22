package it.polito.tdp.seriea.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import it.polito.tdp.seriea.db.SerieADAO;

public class Model {
	
	private SerieADAO dao;
	private Graph<Season, DefaultWeightedEdge> graph;
	private Map<Integer, Season> seasonsIdMap;
	private Map<String, Team> teamsIdMap;
	
	public Model() {
		this.dao = new SerieADAO();
		this.seasonsIdMap = dao.getSeasonsMap();
		this.teamsIdMap = dao.getTeamsMap();
	}
	
	private void createGraph(Team team) {
		this.graph = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
		
		List<SeasonByTeam> seasons = getSeasonsByTeam(team);
		
		if(seasons.size() == 1)
			this.graph.addVertex(seasons.get(0).getSeason());
		
		else {
			for(SeasonByTeam s1 : seasons) {
				for(SeasonByTeam s2 : seasons) {
					if(s1.getPoints() < s2.getPoints() && !s1.equals(s2)) {
						int weight = s2.getPoints() - s1.getPoints();
						Graphs.addEdgeWithVertices(this.graph, s1.getSeason(), s2.getSeason(), weight);
					}
				}
			}
		}
		
		System.out.println("Grafo creato.");
		System.out.println(String.format("%d vertici e %d archi.", this.graph.vertexSet().size(), this.graph.edgeSet().size()));
	}
	
	public String findBestSeason(Team team) {
		createGraph(team);
		
		List<Season> vertexes = new ArrayList<>(this.graph.vertexSet());
		
		if(vertexes.size() == 1)
			return String.format("Stagione %s, differenza pesi: %d\n", vertexes.get(0).getDescription(), 0);
		
		else {
			Season bestSeason = null;
			int bestWeightsDifference = 0;
			
			for(Season v : vertexes) {
				Set<DefaultWeightedEdge> incomingEdges = this.graph.incomingEdgesOf(v);
				Set<DefaultWeightedEdge> outgoingEdges = this.graph.outgoingEdgesOf(v);
				
				int incomingWeights = 0;
				for(DefaultWeightedEdge e : incomingEdges)
					incomingWeights += this.graph.getEdgeWeight(e);
				
				int outgoingWeights = 0;
				for(DefaultWeightedEdge e : outgoingEdges)
					outgoingWeights += this.graph.getEdgeWeight(e);
				
				int temp = incomingWeights - outgoingWeights;
				
				if(temp > bestWeightsDifference) {
					bestSeason = v;
					bestWeightsDifference = temp;
				}
			}
			
			return String.format("Stagione %s, differenza pesi: %d\n", bestSeason, bestWeightsDifference);
		}
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
