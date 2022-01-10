#!/usr/bin/python

import sys, getopt
import random
import math

moves = []
rewards = []

moveSyntax = ["<-","<>","->"]

def genRndBoard(seed) :

    maxReward = 6
    
    # construct the board    
    random.seed(seed)
    for i in range(length) :
        moves.append([])
        rewards.append([])
        for j in range(width) :
            moves[i].append(random.randrange(0,3))   
            rewards[i].append(math.floor(-math.log(1.0/2.0**(maxReward+1)+random.random()*(1.0-1.0/2.0**(maxReward+1)))/math.log(2.0)))

def writePreamble(mFile) :

    # a depiction of the board as a comment
    mFile.write("// Board:\n//")
    for i in range(length) :
        mFile.write("\n//   ")
        for j in range(width) :
            mFile.write("[" + str(int(rewards[i][j])) + "|" + moveSyntax[moves[i][j]] + "] ")

    mFile.write("\n\n")


def writeRobotA(fileName) :

    mFile = open(fileName,"w")

    writePreamble(mFile)

    mFile.write("smg\n")
    mFile.write("// This is the roborta example, modelled as in the paper\n")

    mFile.write("player p1\n")
    mFile.write("Roborta, [move_left], [move_forward], [move_right]\n")
    mFile.write("endplayer\n")
    mFile.write("player p2\n")
    mFile.write("Light, [signal_green], [signal_yellow]\n")
    mFile.write("endplayer\n")
    mFile.write("const int WIDTH ="+str(width)+";\n")
    mFile.write("const int LENGTH ="+str(length)+";\n")

    mFile.write("// T models the fault probability of the robot\n")
    mFile.write("const double T;\n")
    mFile.write("// Q models the fault probability of the light\n")
    mFile.write("const double Q; \n")
    mFile.write("global light : [0..3] init 0;\n")

    mFile.write("// cells where the robot can only move to the right\n")
    mFile.write("formula MOVES_R =")
    for i in range(length) :
        for j in range(width) :
            if moves[i][j] == 2 :
                mFile.write("(row="+str(i)+" & "+"col="+str(j)+") |")
    mFile.write("false;\n")
    mFile.write("// cells where the robot can only move to the left\n")
    mFile.write("formula MOVES_L =\n")
    for i in range(length) :
        for j in range(width) :
            if moves[i][j] == 0 :
                mFile.write("(row="+str(i)+" & "+"col="+str(j)+") |")
    mFile.write("false;\n")
    mFile.write("// the robot can move sideways freely in any other cell\n")


    mFile.write("// Roborta model\n")
    mFile.write("module Roborta\n")
    mFile.write("       col : [0..WIDTH-1] init 0;\n")
    mFile.write("       row : [0..LENGTH-1] init 0;\n")
    mFile.write("       [move_left] ((light=1) | (light=3)) & !MOVES_R & (row < LENGTH) -> (1-T) : (light'=0) & (col'=mod((col-1),WIDTH)) + (T) : (light'=0);\n")
    mFile.write("       [move_right] ((light=1) | (light=3)) & !MOVES_L & (row < LENGTH)  -> (1-T) : (light'=0) & (col'=mod((col+1), WIDTH)) + (T) : (light'=0) ;\n")
    mFile.write("       [move_forward] ((light=2) | (light=3)) & (row < LENGTH) -> (1-T) : (light'=0) & (row'=row+1) + (T) : (light'=0);\n")
    mFile.write("endmodule\n")

    mFile.write("// Light model\n")
    mFile.write("module Light\n")
    mFile.write("       [signal_green] (light=0) & (row < LENGTH) -> (1-Q) : (light'=2) + Q : (light'=3);\n")
    mFile.write("       [signal_yellow] (light=0) & (row < LENGTH) -> (1-Q) : (light'=1) + Q : (light'=3);\n")
    mFile.write("endmodule\n")

    mFile.write("rewards \"r\"\n")
    for i in range(length) :
        for j in range(width) :
            mFile.write("       !(light = 0) & row = "+str(i)+" & col = "+str(j)+" : "+str(int(rewards[i][j]))+" ;\n")
    mFile.write("       row = "+str(length)+" : 0 ;\n")
    mFile.write("       light = 0 : 0 ;\n")
    mFile.write("endrewards\n")

    mFile.close()




def usage(exitVal) :

    print("\nusage robot_gen.py [-h] [-s <int> ] [-w <int>] [-l <int>]\n")
    print("-h :\n")
    print("  Print this help\n")
    print("-s num :\n")
    print("  Sets the seed for the pseudo-random number generator to 'num'. 'num' must be")
    print("  a non-negative integer (0 or higher) (default = 0)\n")
    print("-w num :\n")
    print("  Sets the width of the board to 'num'. 'num' must be a positive integer")
    print("  (greater than 0) (default = 5)\n")
    print("-l num :\n")
    print("  Sets the length of the board to 'num'. 'num' must be a positive integer")
    print("  (greater than 0) (default = 10)\n")
    sys.exit(exitVal)



def main(argv):

    global width
    global length
    global probRobot
    global probLight

    width = 4
    length = 4
    seed = 0
    path = ""


    try:
        opts, args = getopt.getopt(argv,"hs:w:l:",["seed=","width=","length="])
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
        elif opt in ("-w","width=") :
            try :
                width = int(arg)
            except ValueError :
                print("The width must be a positive integer")
                sys.exit(2)            
            if width <= 0 :
                print("The width must be a positive integer")
                sys.exit(2)
        elif opt in ("-l","length=") :
            try :
                length = int(arg)
            except ValueError :
                print("The length must be a positive integer")
                sys.exit(2)            
            if length <= 0 :
                print("The length must be a positive integer")
                sys.exit(2)

    genRndBoard(seed)

    writeRobotA(path+"roborta["+str(seed)+"-"+str(width)+"-"+str(length)+"].prism")



main(sys.argv[1:])
