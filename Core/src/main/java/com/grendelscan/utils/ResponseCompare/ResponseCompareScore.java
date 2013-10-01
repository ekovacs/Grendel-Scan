package com.grendelscan.utils.ResponseCompare;

public class ResponseCompareScore
{
	private int maxScore = 0;
	private int score = 0;
	
	void incScore(int inc)
	{
		score += inc;
	}
	
	void incMaxScore(int inc)
	{
		maxScore += inc;
	}
	
	double getPercentage()
	{
		if (score < 0)
		{
			return 0;
		}
		return (double) score / (double) maxScore;
	}
}
