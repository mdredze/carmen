// Copyright 2012-2013 Mark Dredze. All rights reserved.
// This software is released under the 2-clause BSD license.
// Mark Dredze, mdredze@cs.jhu.edu
package carmen.utils;

public class Timer {
	private long _end_time;
	private long _start_time;
	private long _total_elapsed = 0;
	
	public void start() {
		this._start_time = System.currentTimeMillis();
	}
	
	public void restart() {
		reset();
		this._start_time = System.currentTimeMillis();
	}
	
	public void stop() {
		this._end_time = System.currentTimeMillis();
		this._total_elapsed += this._end_time - this._start_time;
	}
	
	public void printFullTime() {
		System.out.print(getFullTime());
	}

	public String getFullTime() {
		return formatFullTime(this._total_elapsed);
	}

	private String formatFullTime(long elapsed) {
		long ms = elapsed;
		long seconds = elapsed / 1000; ms %= 1000;
		long minutes = seconds / 60;	seconds %= 60;
		long hours = minutes / 60;	minutes %= 60;
		long days = hours / 24;	hours %= 24;
		
		StringBuffer sb = new StringBuffer();
		if (days != 0) { sb.append(days); sb.append(" days "); }
		if (hours != 0) { sb.append(hours); sb.append(" hours "); }
		if (minutes != 0) { sb.append(minutes); sb.append(" minutes "); }
		if (seconds != 0) { sb.append(seconds); sb.append(" seconds "); }
		sb.append(ms); sb.append(" ms");
		
		
		return sb.toString();
	}
	
	public String getMilliseconds() {
		return String.valueOf(this._total_elapsed);
	}
	
	public long getMillisecondsLong() {
		return this._total_elapsed;
	}
	
	public String getSeconds() {
		return String.valueOf(this._total_elapsed / 1000);
	}
	
	public void reset() {
		this._total_elapsed = 0;
	}

	public String getFullTimeSoFar() {
		return formatFullTime(System.currentTimeMillis() + this._total_elapsed - this._start_time);
	}
}
