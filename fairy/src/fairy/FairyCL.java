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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.BitSet;
import java.util.Arrays;


import common.StackTraceHelper;
import parser.Values;
import parser.ast.Expression;
import parser.ast.ExpressionReward;
import parser.ast.ModulesFile;
import parser.ast.PropertiesFile;
import parser.ast.Property;
import prism.Prism.StrategyExportType;
import simulator.GenerateSimulationPath;
import simulator.ModulesFileModelGenerator;
import simulator.method.ACIconfidence;
import simulator.method.ACIiterations;
import simulator.method.ACIwidth;
import simulator.method.APMCapproximation;
import simulator.method.APMCconfidence;
import simulator.method.APMCiterations;
import simulator.method.CIconfidence;
import simulator.method.CIiterations;
import simulator.method.CIwidth;
import simulator.method.SPRTMethod;
import simulator.method.SimulationMethod;
import strat.Strategies;
import strat.Strategy;
import prism.PrismModelListener;
import prism.PrismLog;
import prism.PrismFileLog;
import prism.PrismException;
import prism.Prism;
import parser.ast.ModulesFile;
import prism.ModelType;
import prism.PrismComponent;
import explicit.STPGExplicit;
import explicit.ModelCheckerResult;
import simulator.ModulesFileModelGenerator;
import prism.UndefinedConstants;
import prism.ResultsExporter;
import prism.ResultsCollection;
import parser.Values;

/**
 * @author Pablo
 * This class provides the command line behavior of Fairy
 */
public class FairyCL implements PrismModelListener {
	
	// basic filenames
	private String mainLogFilename = "stdout";
	private String settingsFilename = null;
	private String modelFilename = "";
	
	// params info
	private String[] paramLowerBounds = null;
	private String[] paramUpperBounds = null;
	private String[] paramNames = null;
	
	// main log
	private PrismLog mainLog = null;
	private boolean checkFairness = false;
	private boolean computeRewards = false;
	private String constSwitch = null; // for  dealing with constants
	private boolean exportResults = false; // true if export is enabled
	private String exportResultsFilename = null;
	private boolean v1 = false; // if version 1 of Baiers algorithm for computing the upper bound is used, by default the version used ins v2, this upper bound is tighter
	
	// model failure info
	boolean modelBuildFail = false;
	Exception modelBuildException = null;
	
	// prism object
	Fairy prism = null;
	
	
	@Override
	public void notifyModelBuildSuccessful(){
	}

	@Override
	public void notifyModelBuildFailed(PrismException e){
		modelBuildFail = true;
		modelBuildException = e;
	}
	
	public void run(String[] args){
		
		
		// deals with the arguments
		parseArgs(args);
		ModulesFile modulesFile = null;
		try {
			// we initialize everything perhaps another methods for this will be better
			this.mainLog = new PrismFileLog("stdout");
		
			// create prism object(s)
			this.prism = new Fairy(mainLog);
			prism.addModelListener(this);
		
			// initialise
			prism.initialise();
			prism.setEngine(Prism.EXPLICIT); 
		}
		catch (PrismException e) {
			errorAndExit(e.getMessage());
		}
		
		// the file with the model is parsed
		try {
			mainLog.print("\nParsing model file \"" + modelFilename + "\"...\n");
			modulesFile = prism.parseModelFile(new File(modelFilename), null);	
			prism.loadPRISMModel(modulesFile);
		}catch (FileNotFoundException e) {
			errorAndExit("File \"" + modelFilename + "\" not found");
		}catch (PrismException e) {
			errorAndExit(e.getMessage());
		}
		
		// check if model type is correct
		if (!((prism.getModelType() == ModelType.SMG) || (prism.getModelType() == ModelType.STPG))){
			errorAndExit("Model type must be SMG or STPG");
		}
		
		// a variable to keep track of the results
		ResultsCollection results = null;
		
		try {
			// TBD: process info about undefined constants
			UndefinedConstants undefinedMFConstants = new UndefinedConstants(modulesFile, null); // set the undefined constants
			undefinedMFConstants.defineUsingConstSwitch(constSwitch);
			// a variable to keep track of the results
			results = new ResultsCollection(undefinedMFConstants);
			
			for (int i=0; i < undefinedMFConstants.getNumModelIterations(); i++) { // for every value of the constans
				Values definedMFConstants = undefinedMFConstants.getMFConstantValues();
				prism.setPRISMModelConstants(definedMFConstants, false);
				// define the model checker
				FairSTPGModelChecker mc = null;
				try {
					mc = new FairSTPGModelChecker(prism);
				}catch(PrismException e) {
					errorAndExit("Error in Prism Model:"+e.getMessage());
				}
				if (mc == null)
					errorAndExit("Error while building the Model Checker...");
				
				prism.buildModel();
				FairSTPGExplicit game = new FairSTPGExplicit((STPGExplicit) prism.getBuiltModelExplicit());
				
				ModulesFileModelGenerator currentModelGenerator = null;
				try {
					currentModelGenerator = new ModulesFileModelGenerator(modulesFile, prism);	
				}catch(PrismException e) {
					errorAndExit(e.getMessage());
				}
				mc.setModelCheckingInfo(currentModelGenerator, null, currentModelGenerator);
				
				if (this.checkFairness){
					FairyResult result =  mc.checkStoppingUnderFairness(game);
					//ModulesFileModelGenerator currentModelGenerator = new ModulesFileModelGenerator(modulesFile, prism);	
					//System.out.println(result?"the game is stopping under fairness":"the game is not stopping under fairness");
					mainLog.println(result.terminating?"the game is stopping under fairness":"the game is not stopping under fairness");
					mainLog.println("Time taken for the verification: "+result.timeTaken);
				
				}
				if (this.computeRewards) {
					try {
						// the result is computed, v1 ise used to check which methods it needs to use
						FairyResult result = this.v1?mc.computeFairReachRewards(game, false, true, true):mc.computeFairReachRewards(game, false, true, false);
						Values definedConstants = undefinedMFConstants.getMFConstantValues(); // get the values of the constants
						mainLog.print("Value of the game at initial states: ");
						for (int s : game.getInitialStates()) {
							mainLog.print((s==0?" ":", ")+result.soln[s]);
							results.setResult(definedConstants, result.soln[s]); // set the for only one initial state?
						}
						mainLog.println("");
						mainLog.println("Time taken: " + result.timeTaken);
						mainLog.println("Number of Its.:" + result.numIters);
						//System.out.println(Arrays.toString(result.soln));
					}catch(PrismException e){
						errorAndExit(e.getMessage());
					}
				}
				undefinedMFConstants.iterateModel();
			}
			//prism.buildModel();
		}
		catch(PrismException e) {
			errorAndExit("Error in Prism Model:"+e.getMessage());
		}
		
		// we export the results if needed
		if (exportResults) {
			ResultsExporter exporter = new ResultsExporter("plain", "string");
			mainLog.print("\nExporting results ");
			mainLog.println(exportResultsFilename.equals("stdout") ? "below:\n" : "to file \"" + exportResultsFilename + "\"...");
			PrismFileLog tmpLog = new PrismFileLog(exportResultsFilename);
			if (!tmpLog.ready()) {
				errorAndExit("Couldn't open file \"" + exportResultsFilename + "\" for output");
			}
			tmpLog.println(results.export(exporter).getExportString());
			tmpLog.close();	
		}
		
		/**
		FairSTPGModelChecker mc = null;
		try {
			mc = new FairSTPGModelChecker(prism);
		}catch(PrismException e) {
			errorAndExit("Error in Prism Model:"+e.getMessage());
		}
		if (mc == null)
			errorAndExit("Error while building the Model Checker...");
		

		FairSTPGExplicit game = new FairSTPGExplicit((STPGExplicit) prism.getBuiltModelExplicit());
		ModulesFileModelGenerator currentModelGenerator = null;
		try {
			currentModelGenerator = new ModulesFileModelGenerator(modulesFile, prism);	
		}catch(PrismException e) {
			errorAndExit(e.getMessage());
		}
		mc.setModelCheckingInfo(currentModelGenerator, null, currentModelGenerator);
		//mainLog.println("reward structure: "+currentModelGenerator.getRewardStructIndex("r1"));
		if (this.checkFairness){
			FairyResult result =  mc.checkStoppingUnderFairness(game);
			//ModulesFileModelGenerator currentModelGenerator = new ModulesFileModelGenerator(modulesFile, prism);	
			//System.out.println(result?"the game is stopping under fairness":"the game is not stopping under fairness");
			mainLog.println(result.terminating?"the game is stopping under fairness":"the game is not stopping under fairness");
			mainLog.println("Time taken for the verification: "+result.timeTaken);
		
		}
		if (this.computeRewards) {
			try {
				FairyResult result = mc.computeFairReachRewards(game, false, true);
				mainLog.print("Value of the game at initial states: ");
				for (int s : game.getInitialStates()) {
					mainLog.print((s==0?" ":", ")+result.soln[s]);
				}
				mainLog.println("");
				mainLog.println("Time taken: " + result.timeTaken);
				mainLog.println("Number of Its.:" + result.numIters);
				//System.out.println(Arrays.toString(result.soln));
			}catch(PrismException e){
				errorAndExit(e.getMessage());
			}
		}
		**/
	}
		
	private void parseArgs(String[] args){
		
		// only two options are possible
		//if (args.length != 2){
		// 	System.out.println("Proper Usage is: java fairy [options] <model filename>");
		//	System.exit(0);
		//}
		
		// the last argument is the model filename
		//this.modelFilename = args[args.length-1];
		
		// the first argument is the option: check stopping under fairness or compute rewards
		//switch(args[0]){
		//case "-check":
		//	this.checkFairness = true;
		//	break;
		//case "-erew":
		//	this.computeRewards = true;
		//	break;
		//default:
		//	System.out.println("Proper Usage is: java fairy [options] <model filename>");
		//	System.exit(0);
		//}
		
		constSwitch = "";
		
		for (int i=0; i<args.length; i++) {
			switch(args[i]){
			case "-v1":
				this.v1 = true;
				break;
			case "-check":
				this.checkFairness = true;
				break;
			case "-erew":
				this.computeRewards = true;
				break;
			case "-help": 
				printHelp();
				System.exit(0);
				break; // little sense never reached...
			case "-const":
				if (i < args.length - 1) {
					// store argument for later use (append if already partially specified)
					if ("".equals(constSwitch))
						constSwitch = args[++i].trim();
					else
						constSwitch += "," + args[++i].trim();
				} else {
					errorAndExit("Incomplete constant information");
				}
				break;
			case "-exportresults":
				this.exportResults = true;
				if (i < args.length - 1) {
					// store argument for later use (append if already partially specified)
					this.exportResultsFilename = args[++i].trim();
				} else {
					errorAndExit("Provide a file name for exporting the results.");
				}
				break;
			default:
				if (this.modelFilename.equals("")) // in this case it must be the file name
					this.modelFilename = args[i];
				else { // otherwise it is a unrecognized option
					System.out.println("Proper Usage is: java fairy [options] <model filename>");
					System.exit(0);
				}
			}
		}
		System.out.println(constSwitch);
		
		// at this point the argument were parsed.
	}
	
	private void printHelp() {
		
		System.out.println("Proper Usage is: java fairy [options] <model filename>");
		System.out.println("where [options] are:");
		System.out.println("-erew: compute expected rewards under fairness assumptions.");
		System.out.println("-check: checks wether the model is stopping under fairness.");
		System.out.println("-const: to give values to constants undefined in the model.");
		System.out.println("-help: prints this help.");
	}
	
	/**
	 * Report a (fatal) error and exit cleanly (with exit code 1).
	 */
	private void errorAndExit(String s)
	{
		prism.closeDown(false);
		mainLog.println("\nError: " + s + ".");
		mainLog.flush();
		System.exit(1);
	}

	/**
	 * Exit cleanly (with exit code 0).
	 */
	private void exit()
	{
		prism.closeDown(true);
		System.exit(0);
	}

	// pretty simple main, it only runs fairy
	public static void main(String[] args){
		new FairyCL().run(args);
	}
	
	
}
