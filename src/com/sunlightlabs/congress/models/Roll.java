package com.sunlightlabs.congress.models;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Roll implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public static final String YEA = "Yea";
	public static final String NAY = "Nay";
	public static final String NOT_VOTING = "Not Voting";
	public static final String PRESENT = "Present";

	// convenience flag, trip if there are non-standard votes (Speaker of the House election)
	public boolean otherVotes = false;
	
	// basic
	public String id, how, chamber, vote_type, roll_type, passage_type;
	public String question, result, bill_id, required;
	public int session, number, year;
	public Date voted_at;
	public Map<String,Integer> voteBreakdown = new HashMap<String,Integer>();
	
	// bill
	public Bill bill;
	
	// voters
	public Map<String,Vote> voters;
	
	// voter_ids
	public Map<String,Vote> voter_ids;
	
	/**
	 * Represents the vote of a legislator in a roll call. In almost all cases, votes will be 
	 * "Yea", "Nay", "Present", or "Not Voting". There are constants for these as well, since 
	 * they have official meanings.
	 * 
	 * In one case, the election of the Speaker of the House, votes are recorded as the last name
	 * of the candidate.
	 * 
	 * The 'legislator' field may be null here, in which case you will need to use the bioguide_id
	 * to look up more information about the legislator.
	 */
	public static class Vote implements Comparable<Vote>, Serializable {
		private static final long serialVersionUID = 1L;
		
		public String voter_id; // bioguide ID
		public String vote;
		
		public Legislator voter;
		
		public Vote() {}
		
		public int compareTo(Vote another) {
			return this.voter.compareTo(another.voter);
		}
	}
	
	// splits a roll into chamber, number, and year, returned in a barebones Roll object
	public static Roll splitRollId(String roll_id) {
		Pattern pattern = Pattern.compile("^([a-z]+)(\\d+)-(\\d{4})$");
		Matcher matcher = pattern.matcher(roll_id);
		if (!matcher.matches())
			return null;
		Roll roll = new Roll();
		
		String chamber = matcher.group(1);
		if (chamber.equals("h"))
			roll.chamber = "house";
		else // if (chamber.equals("s")
			roll.chamber = "senate";
		roll.number = Integer.parseInt(matcher.group(2));
		roll.year = Integer.parseInt(matcher.group(3));
		
		return roll;
	}
}