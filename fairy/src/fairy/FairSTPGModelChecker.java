
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
import explicit.*;
import explicit.rewards.*;
import prism.PrismException;
import java.util.BitSet;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import common.IterableBitSet;
import prism.PrismComponent;
import prism.PrismUtils;
import prism.PrismFileLog;
import prism.PrismLog;
import prism.PrismNotSupportedException;
import strat.BoundedRewardDeterministicStrategy;
import strat.MemorylessDeterministicStrategy;
import strat.StepBoundedDeterministicStrategy;
import java.util.Arrays;
import parser.ast.Expression;
import prism.AccuracyFactory;

/**
 * This class extends Prism STPGModelChecker class with fairness behavior
 */
public class FairSTPGModelChecker extends STPGModelChecker{
	
	/**
	 * A basic constructor, it only calls to super constructor
	 * @param parent	
	 * @throws PrismException
	 */
	public FairSTPGModelChecker(PrismComponent parent) throws PrismException{
		super(parent);
	}
	
	/**
	 * @param stpg	
	 * @param rewards
	 * @param target
	 * @param min1
	 * @param min2
	 * @param init
	 * @param known
	 * @param unreachingSemantics
	 * @param v1
	 * @return
	 * @throws PrismException
	 */
	public FairyResult computeFairReachRewards(FairSTPGExplicit stpg, STPGRewards rewards, BitSet target, boolean min1, boolean min2, double init[], BitSet known, boolean v1) throws PrismException
	{
		FairyResult res = null;
		BitSet inf;
		int n, numTarget, numInf;
		long timer, timerProb1, timerApprox;

		// Start expected reachability
		timer = System.currentTimeMillis();
		timerApprox = System.currentTimeMillis();
		if (verbosity >= 1)
			mainLog.println("\n method computeFairReachRewards: Starting expected reachability using fairness assumptions...");

		// Check for deadlocks in non-target state (because breaks e.g. prob1)
		stpg.checkForDeadlocks(target);

		// Store num states
		n = stpg.getNumStates();

		// Optimise by enlarging target set (if more info is available)
		if (init != null && known != null && !known.isEmpty()){
			BitSet targetNew = (BitSet) target.clone();
			for (int i : new IterableBitSet(known)) {
				if (init[i] == 1.0) {
					targetNew.set(i);
				}
			}
			target = targetNew;
		}

		timerProb1 = System.currentTimeMillis();
		
		// identify infinite values
		// we assume that this cannot happens
		inf = prob1(stpg, null, target, !min1, !min2);
		inf.flip(0, n);

		timerProb1 = System.currentTimeMillis() - timerProb1;

		// Print results of precomputation
		numTarget = target.cardinality();
		numInf = inf.cardinality();
		if (verbosity >= 1)
			mainLog.println("target=" + numTarget + ", inf=" + numInf + ", rest=" + (n - (numTarget + numInf)));

		timerApprox = System.currentTimeMillis() - timerApprox;

			
		if (verbosity >= 1) 
			mainLog.println("Computing the GFP.");
			

		// Compute real rewards 
		// Modified: we use GFP computation
		switch (solnMethod) {
		case VALUE_ITERATION:
			res = computeReachRewardsGFPValIter(stpg, rewards, target, inf, min1, min2, init, known, v1);
			break;
		default:
			throw new PrismException("Unknown STPG solution method " + solnMethod);
		}

		// Finished expected reachability
		timer = System.currentTimeMillis() - timer;
		if (verbosity >= 1)
			mainLog.println("Expected reachability under fairness took " + timer / 1000.0 + " seconds.");

		// Update time taken
		res.timeTaken = timer / 1000.0;
		res.timePre = timerProb1 / 1000.0;

		return res;
	}
	
	public FairyResult checkStoppingUnderFairness(FairSTPGExplicit stpg) {
		return stpg.stoppingUnderFairness(this.findTerminalStates(stpg));
	}
	
	/**
	 * An useful version of computeFairReachRewards where terminal are taken as targets
	 * and rewards are computed automatically
	 * @param stpg
	 * @param rewards
	 * @param min1
	 * @param min2
	 * @param init
	 * @param known
	 * @param v1 	if true, version 1 of Baier's algorithm is used 
	 * @return
	 * @throws PrismException
	 */
	public FairyResult computeFairReachRewards(FairSTPGExplicit stpg,  boolean min1, boolean min2, boolean v1) throws PrismException
	{
		STPGRewards rew = (STPGRewards) constructRewards(stpg, 0);
		return computeFairReachRewards(stpg, rew, this.findTerminalStates(stpg), min1, min2, null, null, v1);
	}
	
	
	/**
	 * Compute expected reachability rewards using value iteration from an Upper Bound
	 * this is the modified version of iteration value defined in the paper.
	 * @param stpg The STPG
	 * @param rewards The rewards
	 * @param target Target states
	 * @param inf States for which reward is infinite
	 * @param min1 Min or max rewards for player 1 (true=min, false=max)
	 * @param min2 Min or max rewards for player 2 (true=min, false=max)
	 * @param init Optionally, an initial solution vector (will be overwritten) 
	 * @param known Optionally, a set of states for which the exact answer is known
	 * @param v1 it says if the upperbound is computed with version 1 or version 2, by default it uses v2
	 * Note: if 'known' is specified (i.e. is non-null, 'init' must also be given and is used for the exact values.
	 */
	public FairyResult computeReachRewardsGFPValIter(FairSTPGExplicit stpg, STPGRewards rewards, BitSet target, BitSet inf, boolean min1, boolean min2,
		double init[], BitSet known, boolean v1) throws PrismException {
		FairyResult res;
		BitSet unknown, notInf;
		int i, n, iters;
		double soln[], soln2[], tmpsoln[];
		int adv[] = null;
		boolean genAdv, done;
		long timer;
		// LP: Calculate Upper Bound
		//upperBound = computeUpperBound((STPGExplicit)stpg, (SMGRewardsSimple)rewards, min1, min2);
		//mainLog.println("Baiers Second Upper Bound:"+Arrays.toString(computeUpperBoundV2((STPGExplicit)stpg, (SMGRewardsSimple)rewards, min1, min2)));
		//init = computeUpperBound((STPGExplicit)stpg, (SMGRewardsSimple) rewards);
		// we use the second method for computing the upper bound
		//init = computeUpperBoundVariant2(stpg, (SMGRewardsSimple)rewards, min1, min2);
		if (!v1)
			init = stpg.computeUpperBoundVariant2(2, (STPGRewardsSimple)rewards, this);
		else
			init = stpg.computeUpperBoundVariant1(2, (STPGRewardsSimple)rewards, this);	
		//mainLog.println("Target set: " + target);
		//mainLog.println("Upper Bound Computed with Baier's Method: "+ Arrays.toString(init));
	
		// Are we generating an optimal adversary?
		genAdv = exportAdv || generateStrategy;

		// Start value iteration
		timer = System.currentTimeMillis();
		if (verbosity >= 1)
			mainLog.println("Starting value iteration (" + (min1 ? "min" : "max") + (min2 ? "min" : "max") + ")...");

		// Store num states
		n = stpg.getNumStates();

		// Create solution vector(s)
		soln = new double[n];
		soln2 = (init == null) ? new double[n] : init;

		// we initialize the computation of the upper bound using the upper bound computed with uniform MDP + Baier's method
		for (i = 0; i < n; i++){
			soln[i] = soln2[i] = target.get(i) ? 0.0 : init[i];
		}

		// Determine set of states actually need to compute values for
		unknown = new BitSet();
		unknown.set(0, n);
		unknown.andNot(target);
		unknown.andNot(inf);
		if (known != null)
			unknown.andNot(known);

		// constructing not infinity set
		notInf = (BitSet) inf.clone();
		notInf.flip(0, n);

		// Create/initialise adversary storage
		if (genAdv) {
			adv = new int[n];
			for (i = 0; i < n; i++) {
				adv[i] = -1;
			}

			int s;
			for (i = 0; i < inf.length(); i++) {
				s = inf.nextSetBit(i);
				for (int c = 0; c < stpg.getNumChoices(s); c++) {
					// for player 1 check
					if (stpg.getPlayer(s) == 1 && !stpg.allSuccessorsInSet(s, c, notInf)) {
						adv[i] = c;
						break;
					}
				}
			}
		}

		// Start iterations
		iters = 0;
		done = false;
		while (!done && iters < maxIters) {
			
			//if (iters%500==0) mainLog.println(soln);
			//mainLog.println(rewards);
			//mainLog.println(min1);
			//mainLog.println(min2);
			//mainLog.println(soln2);
			//mainLog.println(unknown);
			//mainLog.println(genAdv);
			
			iters++;
			// Matrix-vector multiply and min/max ops
			stpg.mvMultRewMinMax(soln, rewards, min1, min2, soln2, unknown, false, genAdv ? adv : null, useDiscounting ? discountFactor : 1.0);

			

			// Check termination
			done = PrismUtils.doublesAreClose(soln, soln2, termCritParam, termCrit == TermCrit.ABSOLUTE);
		
			// We avoid overflow with these lines	
			for (i=0;i<n;i++){
				if (soln2[i] > init[i]){
					soln2[i] = Double.min(soln2[i], init[i]);
					done = false; // PC: we need to avoid worng stopping since this modification
				}
			}

			// Swap vectors for next iteration
			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
		}

		// Finished value iteration
		timer = System.currentTimeMillis() - timer;
		if (verbosity >= 1) {
			mainLog.print("Value iteration (" + (min1 ? "min" : "max") + (min2 ? "min" : "max") + ")");
			mainLog.println(" took " + iters + " iterations and " + timer / 1000.0 + " seconds.");
		}

		// Print adversary
		if (genAdv) {
			PrismLog out = new PrismFileLog(exportAdvFilename);
			if (exportAdvFilename.lastIndexOf('.') != -1 && exportAdvFilename.substring(exportAdvFilename.lastIndexOf('.') + 1).equals("dot")) {
				stpg.exportToDotFileWithStrat(out, null, adv);
			} else {
				for (i = 0; i < n; i++) {
					out.println(i + " " + (adv[i] != -1 ? stpg.getAction(i, adv[i]) : "-"));
				}
				out.println();
			}
			out.close();
		}

		// Non-convergence is an error (usually), kept the original messages from prism
		if (!done && errorOnNonConverge) {
			String msg = "Iterative method did not converge within " + iters + " iterations.";
			msg += "\nConsider using a different numerical method or increasing the maximum number of iterations";
			throw new PrismException(msg);
		}
		//PC: print result
		//mainLog.println("Solution:"+Arrays.toString(soln));
		// Store results/strategy
		res = new FairyResult();
		res.soln = soln;
		double maxDiff = PrismUtils.measureSupNorm(soln, soln2, termCrit == TermCrit.ABSOLUTE);
		res.accuracy = AccuracyFactory.valueIteration(termCritParam, maxDiff, termCrit == TermCrit.ABSOLUTE);
		res.numIters = iters;
		res.timeTaken = timer / 1000.0;
		if (generateStrategy) {
			res.strat = new MemorylessDeterministicStrategy(adv);
		}

		return res;
	}
	
		
	
}
