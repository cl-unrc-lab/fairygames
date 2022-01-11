#!/usr/bin/python

import sys, getopt
import random
import math

roads = []
checkpoints = []
rewards = []

def genRndBoard(seed) :
    
    probDanger = 20
    # construct the road map    
    random.seed(seed)
    for i in range(maxWaypoint) :
        checkpoints.append(random.randrange(0,2))
        roads.append([])
        for j in range(maxWaypoint) :
            roads[i].append(0)
    for i in range(maxWaypoint) :
        for j in range(maxWaypoint) :
            if i!=j:
                r = random.randrange(0,100) #0:no road,1:road,2:dangerous road
                if (r < probDanger):
                    roads[i][j] = 2
                    roads[j][i] = 2
                elif (r < probDanger + (100-probDanger)/2):
                    roads[i][j] = 1
                    roads[j][i] = 1
                else:
                    roads[i][j] = 0
                    roads[j][i] = 0
    for i in range(maxWaypoint) :
        rewards.append(random.randrange(0,10))


def writePreamble(mFile) :

    # a depiction of the board as a comment
    mFile.write("// Map:\n//")
    for i in range(maxWaypoint) :
        mFile.write("\n// ")
        for j in range(maxWaypoint-i) :
            if (roads[i][j+i] > 0):
                mFile.write("[w" + str(i+1) + "-w" + str(j+i+1) + " danger?:"+str(roads[i][j+i] == 2) + "] ")
    mFile.write("// Checkpoints:\n//")
    for i in range(maxWaypoint) :
        if (checkpoints[i] > 0):
            mFile.write("[w" + str(i+1) +"]")
    mFile.write("// Rewards:\n//")
    for i in range(maxWaypoint) :
        mFile.write("[w" + str(i+1) +"]("+str(rewards[i])+")")
            
    mFile.write("\n\n")


def writeUAV_1(fileName) :

    mFile = open(fileName,"w")

    writePreamble(mFile)

    mFile.write("smg\n")
    mFile.write("const double pb;\n")
    mFile.write("const double del;\n")
    mFile.write("// UAV variables\n")
    #mFile.write("global stop : bool init false; // done visiting all waypoints\n")
    mFile.write("global state : [0..4] init 0; // 0:init,1:good_img,2:del,3:no_del,4:stop\n")
    mFile.write("global stop : bool init false;\n")
    mFile.write("global w : [0.."+str(maxWaypoint)+"] init 1;\n")
    for i in range(maxWaypoint) :
        mFile.write("global w"+(str(i+1))+" : bool init false ;  // visited w"+(str(i+1))+"?\n")
    mFile.write("// Players\n")
    mFile.write("global pl : [1..2] init 2; // 1 ... UAV, 2 ... operator\n")
    mFile.write("player p1\n")
    mFile.write("UAV, [uav_stop]")
    for i in range(maxWaypoint) :
        for j in range(maxWaypoint) :
            if roads[i][j] == 1 or roads[i][j] == 2:
                mFile.write(",[uav_fly_"+str(i+1)+"_"+str(j+1)+"]")

    mFile.write("\nendplayer\n")
    mFile.write("player p2\n")
    mFile.write("Operator, [image], [delegate], [op_fly]\n")
    mFile.write("endplayer\n")
    mFile.write("\n")

    mFile.write("//UAV model\n")
    mFile.write("module UAV\n")
    mFile.write("\n")
    for i in range(maxWaypoint) :
        for j in range(maxWaypoint) :
            if roads[i][j] == 1:
                mFile.write("    [uav_fly_"+str(i+1)+"_"+str(j+1)+"] pl = 1 & state = 3 & w = "+str(i+1)+" -> "+"1-pb"+": (pl' = 2) & (state' = 0) & (w' = "+str(j+1)+") + "+"pb"+": (pl' = 2) & (state' = 4) & (stop'=true);\n")
            if roads[i][j] == 2:
                mFile.write("    [uav_fly_"+str(i+1)+"_"+str(j+1)+"] pl = 1 & state = 3 & w = "+str(i+1)+" -> (state' = 4) & (stop'=true);\n")
    mFile.write("    [uav_stop] pl=1 & ")
    for i in range(maxWaypoint) :
        if (i==maxWaypoint-1):
            mFile.write("w"+str(i+1) + " -> (state' = 4) & (stop'=true);\n")
        else:
            mFile.write("w"+str(i+1)+" & ")
    mFile.write("    [uav_stop] pl=1 & stop -> true;\n")
    mFile.write("endmodule\n")

    mFile.write("//Operator model\n")
    mFile.write("module Operator\n")
    mFile.write("\n")
    mFile.write("    [image] pl=2 & state = 0 -> (state' = 0); //bad image\n")
    mFile.write("    [image] pl=2 & state = 0 -> (state' = 1); //good image\n")
    for i in range(maxWaypoint) :
        if checkpoints[i]==1:
            mFile.write("    [delegate] pl=2 & state = 1 & w = "+str(i+1)+" -> "+"del"+": (state' = 3) & (pl' = 1) & (w"+str(i+1)+"' = true) + "+"1-del"+": (state' = 2) & (w"+str(i+1)+"' = true);\n")
        else:
            mFile.write("    [delegate] pl=2 & state = 1 & w = "+str(i+1)+" -> (state' = 3) & (pl' = 1) & (w"+str(i+1)+"' = true);\n")
     
    for i in range(maxWaypoint) :
        for j in range(maxWaypoint) :
            if roads[i][j] == 1:
                mFile.write("    [op_fly] pl=2 & state = 2 & w = "+str(i+1)+" -> "+"1-pb"+": (state' = 0) & (w' = "+str(j+1)+") + "+"pb"+": (state' = 4) & (stop'=true) & (pl'=1);\n")
            if roads[i][j] == 2:
                mFile.write("    [op_fly] pl=2 & state = 2 & w = "+str(i+1)+" -> (state' = 4) & (stop'=true) & (pl'=1);\n")
    mFile.write("\n")
    mFile.write("endmodule\n")

    

    mFile.write("rewards \"visited\"\n")
    for i in range(maxWaypoint) :
            mFile.write("       state=1 & !w"+str(i+1)+" & w="+str(i+1)+" : "+str(rewards[i])+";\n")
    mFile.write("endrewards\n")


    mFile.close()


def usage(exitVal) :

    print("\nusage uavOperator_gen.py [-h] [-s <int> ] [-w <int>]\n")
    print("-h :\n")
    print("  Print this help\n")
    print("-s num :\n")
    print("  Sets the seed for the pseudo-random number generator to 'num'. 'num' must be")
    print("  a non-negative integer (0 or higher) (default = 0)\n")
    print("-w num :\n")
    print("  Sets the number of waypoints of the map to 'num'. 'num' must be a positive integer")
    print("  (greater than 0) (default = 6)\n")
    sys.exit(exitVal)



def main(argv):

    global maxWaypoint

    maxWaypoint = 6
    seed = 0
    path = ""


    try:
        opts, args = getopt.getopt(argv,"hs:w:",["seed=","waypoints="])
    except getopt.GetoptError:
        usage(2)
    for opt, arg in opts:
        if opt == '-h':
            usage(0)
        elif opt in ("-s","seed=") :
            try :
                seed = int(arg)
            except ValueError :
                print("The width must be a nonnegative integer")
                sys.exit(2)            
            if seed < 0 :
                print("The width must be a nonnegative integer")
                sys.exit(2)
        elif opt in ("-w","waypoints=") :
            try :
                maxWaypoint = int(arg)
            except ValueError :
                print("The number of waypoints must be a positive integer")
                sys.exit(2)            
            if maxWaypoint <= 0 :
                print("The number of waypoints must be a positive integer")
                sys.exit(2)

    genRndBoard(seed)

    writeUAV_1(path+"uav["+str(seed)+"-"+str(maxWaypoint)+"].smg")



main(sys.argv[1:])
