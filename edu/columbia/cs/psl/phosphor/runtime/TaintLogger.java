package edu.columbia.cs.psl.phosphor.runtime;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;

public class TaintLogger {
	
	public static final Calendar TIME;
	static {
		TIME = Calendar.getInstance();
	}
	
	
	public static void logTaint(Taint taint){
		//mypath = "/Users/adambarson/Desktop/Research/phosphor/Phosphor/log.txt"
			try(FileWriter fw = new FileWriter("log.txt", true);
				    BufferedWriter bw = new BufferedWriter(fw);
					PrintWriter printLog = new PrintWriter(bw))
				{
					printLog.println(TIME.getTime());
				    printLog.println("Warning- possibly tainted value being passed to sink: " + taint);
				} catch (IOException e) {
					//TODO: DO NOT FORGET ABOUT THIS!
				    //throw new RuntimeException("LOG ERROR " + e.getMessage());
					System.err.println("Encountered error while logging: " + e.getMessage());
				}
		
	}
	
	public static void logCustom(String message){
		//mypath = "/Users/adambarson/Desktop/Research/phosphor/Phosphor/log.txt"
			try(FileWriter fw = new FileWriter("log_custom.txt", true);
				    BufferedWriter bw = new BufferedWriter(fw);
					PrintWriter printLog = new PrintWriter(bw))
				{
					printLog.println(TIME.getTime());
				    printLog.println(message);
				} catch (IOException e) {
					//TODO: DO NOT FORGET ABOUT THIS!
				    //throw new RuntimeException("LOG ERROR " + e.getMessage());
					System.err.println("Encountered error while logging: " + e.getMessage());
				}
		
	}
}
