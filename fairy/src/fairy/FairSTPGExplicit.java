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
import explicit.rewards.MDPRewardsSimple;
import explicit.rewards.*;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.LinkedList;

import prism.PrismException;


/**
 * This class extends STPGExplicit with methods useful for fairness computation
 * @author pablo
 *
 */
public class FairSTPGExplicit extends STPGExplicit {

	protected List<List<Distribution>> oldTrans; // it records the old distribution, useful for restoring the configuration of a game
	
	/**
	 * Constructor: empty STPG.
	 */
	public FairSTPGExplicit(){
		super(); // just call the super
		oldTrans = null; // just null 
	}

	/**
	 * Constructor: new STPG with fixed number of states.
	 */
	public FairSTPGExplicit(int numStates){
		super(numStates);
	}

	/**
	 * Construct an STPG from an existing one and a state index permutation,
	 * i.e. in which state index i becomes index permut[i].
	 */
	public FairSTPGExplicit(FairSTPGExplicit stpg, int permut[]){
		super(stpg, permut);
	}

	/**
	 * Copy constructor that takes a STPGExplicit
	 */
	public FairSTPGExplicit(STPGExplicit stpg){
		super(stpg);
		this.oldTrans = null;
	}

	
	/**
	 * 
	 * @param players	the player to which the distributions will be changed by uniform transtions
	 */
	public void transformToUniform(List<Integer> players){
		// Transform stpg into simple mdp
		// the current trans is saved
		this.oldTrans = this.trans; 
		
		// for the players in the list we change the transition to an uniform distribution
		// this transform de two player game into a MDP
		for (int p : players) {
			for (int i = 0; i < this.getNumStates(); i++){
				if (this.stateOwners.get(i) == p){ // replace minimizer actions for a unique distribution
					List<Distribution> act = this.trans.get(i);
					this.trans.set(i,new LinkedList<Distribution>());
					Distribution uniformDist = new Distribution();
					for (Distribution d : act){
						for (Integer s : d.getSupport()){
							if (d.get(s) > 0)
								uniformDist.set(s,d.get(s)/act.size());
						}
					}
					this.trans.get(i).add(uniformDist);
				}
			}
		}
	}
	
	
	public double[] computeUpperBoundVariant2(int player, STPGRewardsSimple rew, FairSTPGModelChecker mc) throws PrismException{
		double[] res = new double[this.getNumStates()];
		
		// the given player is the minimizer
		LinkedList<Integer> minimizer = new LinkedList<Integer>();
		minimizer.add(player);
		
		// transform the game to an MDP
		this.transformToUniform(minimizer);
		
				
		BitSet finalStates = new BitSet(this.getNumStates());
				
		// final states are those which only have them as successors
		for (int i=0; i< this.getNumStates(); i++){
			// we compute the successors
			BitSet succs = new BitSet(this.getNumStates());
			for (int j=0; j< this.getNumStates(); j++){
				for (Distribution dist : this.trans.get(i)){
					if (dist.get(j)>0)
						succs.set(j);
					}
				}	
			if (succs.get(i) && succs.cardinality() == 1)
				finalStates.set(i);
		}
		//mainLog.println("final states:"+finalStates.toString());
		
		// we compute the Ti's
		LinkedList<BitSet> ts = new LinkedList<BitSet>();
		// initial T0
		BitSet init = new BitSet(this.getNumStates()); // T_0
		init.or(finalStates); // the final states are added
		ts.add(init);
		BitSet S = new BitSet(this.getNumStates());
		S.set(0,this.getNumStates(),true); // S contains all the states
		while (true){
			BitSet S0 = (BitSet) S.clone();
			S0.andNot(ts.getLast());// the complement with the last T: S_i= S_{i-1} \ T
			BitSet S1 = new BitSet(this.getNumStates());
			for (int i=0; i<this.getNumStates();i++){
				if (S0.get(i) && uniformSuccsIn(i, ts.getLast()))
					S1.set(i);
			}
			//S = S1;
			BitSet T = (BitSet) ts.getLast().clone();
			T.or(S1);
			if (T.equals(ts.getLast()))
				break;
			ts.addLast(T);
		}
		//mainLog.println("ts:"+ts.toString());

		// we compute the SCCS
		// Compute strongly connected components (SCCs)
		SCCConsumerStore sccStore = new SCCConsumerStore();
		SCCComputer sccComputer = SCCComputer.createSCCComputer(mc, this, sccStore);
		sccComputer.computeSCCs();
		List<BitSet> sccs = sccStore.getSCCs();
		BitSet notInAnySCCs = sccStore.getNotInSCCs();
		// Consider singletons as SCCs
		for (int i = 0; i < this.getNumStates(); i++){
			if (notInAnySCCs.get(i)){
				BitSet bs = new BitSet(this.getNumStates());
				bs.set(i);
				sccs.add(bs);
			}
		}
		int numSCCs = sccs.size();
		//mainLog.println("not in any sccs: "+notInAnySCCs);

		// we store in an array the SCC corresponding to each state, i.e., whichSCC[i] returns which is the SCC of state i
		int[] whichSCC = new int[this.getNumStates()];
		for (int i = 0; i<sccs.size();i++){
			for (int j=0; j<this.getNumStates(); j++){
				if (sccs.get(i).get(j))
					whichSCC[j] = i; // the corresponding scc was found
			}
		}
		
		// now we compute the ds
		// the terminal states are easy
		double[] d = new double[this.getNumStates()];
		for (int i=0; i < this.getNumStates(); i++){
			if (finalStates.get(i))
				d[i] = 1;
		}
		for (int i=1; i<ts.size(); i++){
			for (int j=0; j<this.getNumStates(); j++){
				if (ts.get(i).get(j)){ // if the state is in T_i
					d[j] = Double.POSITIVE_INFINITY;
					for (Distribution dist : this.trans.get(j)){ // for each choice in that state
						for (int k=0; k<this.getNumStates(); k++){
							if (dist.get(k) > 0 && ts.get(i-1).get(k)){
								double dut = whichSCC[j] != whichSCC[k]?1:d[k];
								d[j] = Math.min(d[j], dist.get(k) * dut);
							}
						}
					}
				}
			}
		}
		
		// now, we compute de zetas
		double[] zetas = new double[this.getNumStates()];
		for (int i=0; i< this.getNumStates(); i++){
			zetas[i] = 1/d[i];
		}
				
		// this should be only take into account  the reachable states
		double upperBoundV2 = 0;
		for (int i=0; i<this.getNumStates(); i++){
			res[i] = 0;
			// we calculate the reachable set
			LinkedList<Integer> reachable = this.getReachableFrom(i);
			for (Integer j : reachable){
				//res[i] += rew.getStateRewards().get(j)* zetas[j];
				res[i] += rew.getStateReward(j) * zetas[j];
			}
		}
		//for (int i : stpg.getInitialStates()){
		//	mainLog.println("initial state:"+ i);
		//}
		//mainLog.println("zetas V2:"+Arrays.toString(zetas));
		//mainLog.println("Upper Bound with Baier's Method V2:"+res);
				
		this.restoreSTPG();
		return res;
		
	}
	
	/**
	 * It says if from a state we can reach a set of states for any possible choice
	 * @param k
	 * @param s
	 * @return
	 */
	private boolean uniformSuccsIn(int k, BitSet s){
		boolean result = true;
		for (Distribution dist :this.trans.get(k)){
			if (!dist.containsOneOf(s)) // we found a distribution that does not contain a succ in S
				return false;
		}
		return result;
	}
	
	/**
	 * 
	 * @param s	the starting state
	 * @return	the list of state reachable from s
	 */
	public LinkedList<Integer> getReachableFrom(Integer s){
		boolean[] visited = new boolean[this.getNumStates()];
		LinkedList<Integer> toVisit = new LinkedList<Integer>(); 
		LinkedList<Integer> res = new LinkedList<Integer>();
		toVisit.add(s);
		while (!toVisit.isEmpty()){
			Integer current = toVisit.removeLast();
			visited[current] = true;
			for (Distribution d : this.trans.get(current)){
				for (Integer succ : d.getSupport()){
					if (!visited[succ]){
						toVisit.add(succ);
					}
				}
			}
		}
		for (int i = 0; i < this.getNumStates(); i++){
			if (visited[i])
				res.add(i);
		}
		return res;
	}
	
	/**
	 * A simple method for restoring the configuration of the STPG
	 */
	public void restoreSTPG() {
		if (oldTrans != null) {
			this.trans = this.oldTrans;
			oldTrans = null;
		}
	}
	
	/**
	 * 
	 * @param i	the state
	 * @return	the state owner of the given state
	 */
	public int getStateOwner(int i) {
		return this.stateOwners.get(i);
	}
	
	/**
	 * 
	 * @param target	the target (terminal states)
	 * @return	@code{True} iff the game is stopping under fairness
	 */
	public FairyResult stoppingUnderFairness(BitSet target){
		
		double timer = System.currentTimeMillis(); // for computing the time taken by the method
		FairyResult res = new FairyResult();
		
		LinkedList<Integer> t = new LinkedList<Integer>();
		for (int i = 0; i < this.getNumStates(); i++){
			if (target.get(i)){
				t.add(i);
			}
		}
		int oldSize = 0;
		while (t.size() != oldSize){
			oldSize = t.size();
			for (Integer i : getPre(t,true)){
				if (!t.contains(i))
					t.add(i);
			}
		}

		LinkedList<Integer> notT = new LinkedList<Integer>();
		for (int i = 0; i < this.getNumStates(); i++){
			if (!t.contains(i)){
				notT.add(i);
			}
		}
		oldSize = 0;
		while (notT.size() != oldSize){
			oldSize = notT.size();
			for (Integer i : getPre(notT,false)){
				if (!notT.contains(i))
					notT.add(i);
			}
		}
		
		timer = System.currentTimeMillis() - timer;
		res.timeTaken = timer;
		res.terminating = notT.isEmpty();
		//return notT.isEmpty();
		return res;
	}
	
	// LP: Added method to get predecessor states from a given set of states
	private List<Integer> getPre(List<Integer> s, boolean forall){
		LinkedList<Integer> res = new LinkedList<Integer>();
		if (!forall){
			for (int i=0;i<this.getNumStates();i++){
				boolean addIt = false;
				List<Distribution> act = this.trans.get(i);
				for (int j=0;j<act.size() && !addIt;j++){
					for (Integer succ : act.get(j).getSupport()){
						if (s.contains(succ)){
							res.add(i);
							addIt = true;
							break;
						}
					}
				}
			}
		}
		else{
			for (int i=0;i<this.getNumStates();i++){
				if (this.stateOwners.get(i)==1){
					boolean addIt = true;
					List<Distribution> act = this.trans.get(i);
					for (int j=0;j<act.size() && addIt;j++){
						boolean atLeastOneSupport = false;
						for (Integer succ : act.get(j).getSupport()){
							if (s.contains(succ)){
								atLeastOneSupport = true;
								break;
							}
						}
						if (!atLeastOneSupport){
							addIt = false;
							break;
						}
					}
					if (addIt)
						res.add(i);
				}
				else{
					boolean addIt = false;
					List<Distribution> act = this.trans.get(i);
					for (int j=0;j<act.size() && !addIt;j++){
						for (Integer succ : act.get(j).getSupport()){
							if (s.contains(succ)){
								res.add(i);
								addIt = true;
								break;
							}
						}
					}
				}
			}
		}
		return res;
	}
	
}// end class
