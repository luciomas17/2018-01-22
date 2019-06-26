package it.polito.tdp.seriea.model;

import java.util.ArrayList;
import java.util.Collections;
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
	private List<SeasonByTeam> bestPath;
	
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
	
	public List<SeasonByTeam> findBestPath(Team team) {
		List<Season> seasons = new ArrayList<>(this.graph.vertexSet());
		Collections.sort(seasons);
		
		this.bestPath = new ArrayList<>();
		
		for(Season s : seasons) {
			List<SeasonByTeam> partial = new ArrayList<>();
			partial.add(getSeasonByTeamFromSeason(team, s));
			recursive(team, seasons, partial, 0);
		}
		
		return bestPath;
	}

	private void recursive(Team team, List<Season> seasons, List<SeasonByTeam> partial, int level) {
		if(partial.size() > bestPath.size())
			bestPath = new ArrayList<>(partial);
		
		for(Season s : seasons) {
			SeasonByTeam sbt = getSeasonByTeamFromSeason(team, s);
			
			if(!partial.contains(sbt)) {			
				SeasonByTeam prev = partial.get(partial.size()-1);
				
				if(isConsecutive(sbt, prev) && isBetter(sbt, prev)) {
					partial.add(sbt);
					recursive(team, seasons, partial, level+1);
					partial.remove(sbt);
				}
			}
		}
	}

	private boolean isBetter(SeasonByTeam sbt, SeasonByTeam prev) {
		List<Season> successors = Graphs.successorListOf(this.graph, prev.getSeason());
		
		if(successors.size() == 0)
			return false;
		
		if(successors.contains(sbt.getSeason()))
			return true;
		else
			return false;
	}

	private boolean isConsecutive(SeasonByTeam sbt, SeasonByTeam prev) {
		List<SeasonByTeam> seasonsInSerieAByTeam = getSeasonsByTeam(prev.getTeam());
		List<Season> seasonsInSerieA = new ArrayList<>();
		for(SeasonByTeam s : seasonsInSerieAByTeam)
			seasonsInSerieA.add(s.getSeason());
		Collections.sort(seasonsInSerieA);
		
		if(seasonsInSerieA.size() == 0)
			return false;
		
		int index = -1;
		for(int i = 0; i < seasonsInSerieA.size(); i ++) {
			if(seasonsInSerieA.get(i) == prev.getSeason()) {
				index = i;
				break;
			}
		}
		
		if(index == seasonsInSerieA.size()-1)
			return false;
		
		if(sbt.getSeason() == seasonsInSerieA.get(index+1))
			return true;
		else
			return false;
	}

	private SeasonByTeam getSeasonByTeamFromSeason(Team team, Season s) {
		List<SeasonByTeam> seasons = getSeasonsByTeam(team);
		SeasonByTeam temp = null;
		
		for(SeasonByTeam season : seasons) {
			if(season.getSeason().equals(s)) {
				temp = season;
				break;
			}
		}
			
		return temp;
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
