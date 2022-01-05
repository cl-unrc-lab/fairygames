/******************************************************************************
 * Copyright (c) 2021-
 * Authors:
 * Pablo Castro <pcastro@dc.exa.unrc.edu.ar> (National University of Rio Cuarto - Argentina)
 * Luciano Putruele <lputruele@dc.exa.unrc.edu.ar> (National University of Rio Cuarto - Argentina)
 *
 * This file is part of FairyGames
 *
 * FairyGames is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * FairyGames is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FairyGames; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package fairy;

import java.nio.file.Files;
import java.util.ArrayList;
import java.io.IOException;
import java.nio.file.*;

import dv.DoubleVector;
import hybrid.PrismHybrid;
import jdd.JDD;
import mtbdd.PrismMTBDD;
import odd.ODDUtils;
import prism.Prism;
import prism.PrismLog;
import sparse.PrismSparse;
import prism.PrismException;
import prism.PrismUtils;
import prism.PrismFileLog;

/**
 * This provides basic settings for the tool, it inherits most methods from Prism
 * only the methods with info about the tool are defined
 * @author pablo
 *
 */

public class Fairy extends Prism {

	
	/**
	 * Construct a new Prism object.
	 * @param mainLog PrismLog where all output will be sent.
	 */
	public Fairy(PrismLog mainLog)
	{
		super(mainLog);
	}

	/**
	 * This method hide the original method of Prism
	 * @return	the name of the tool
	 */
	public static String getToolName(){
		return "Fairy Tool";
	}
	
	/**
	 * Get the name of the command-line version of this tool.
	 * it hides the original methods of prism 
	 */
	public static String getCommandLineName(){
		return "fairy";
	}
	
	/**
	 * Get current version number, as a string. 
	 */
	public static String getVersion(){
		return "1.0";
	}
	
	/**
	 * Initialise Fairy.
	 */
	@Override
	public void initialise() throws PrismException
	{
		// a little hack, we cannot override getToolName because it is static...
		PrismFileLog tempLog = new PrismFileLog("log");
		PrismLog oldLog = this.getLog();
		this.setMainLog(tempLog);
		super.initialise();
		Path path  = Paths.get("log");
		try {
			Files.deleteIfExists(path);
		}catch(IOException e) {
			
		}
		this.setMainLog(oldLog); // old log is recovered
		boolean verbose = this.getSettings().getBoolean("PRISM_VERBOSE");
		mainLog.setVerbosityLevel(verbose ? PrismLog.VL_ALL : PrismLog.VL_DEFAULT);
		mainLog.print(getToolName() + "\n");
		mainLog.print(new String(new char[getToolName().length()]).replace("\0", "=") + "\n");
		mainLog.print("\nVersion: " + getVersion() + " (based on PRISM " + getBaseVersion() + ")\n");
		mainLog.print("Date: " + new java.util.Date() + "\n");
		try {
			String h = java.net.InetAddress.getLocalHost().getHostName();
			mainLog.print("Hostname: " + h + "\n");
		} catch (java.net.UnknownHostException e) {
		}
		mainLog.print("Memory limits: cudd=" + getCUDDMaxMem());
		mainLog.println(", java(heap)=" + PrismUtils.convertBytesToMemoryString(Runtime.getRuntime().maxMemory()));
	}
	
}
