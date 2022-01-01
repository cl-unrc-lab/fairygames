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

/**
 * @author Pablo
 * This class provides the command line behavior of Fairy
 */
public class FairyCL {
	
	public static void main(String[] args){
		
		if (args.length == 0){
			System.out.println("Proper Usage is: java fairy [options] <model filename>");
	        System.exit(0);
		}
		
		// the last argument is the model file
		
		// a simple parsing for the options
		for (int i=0; i<args.length-1; i++) {
			
		}
		
	}

	
}
