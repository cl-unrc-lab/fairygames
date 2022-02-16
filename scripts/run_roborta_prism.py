# A simple script to run the experiments in the repo
# it runs roborta with several parameters using prism-games
# the results are output to a folder "results"
import os;

# variables for running roborta experiments
roborta_grids = ["\\[1-8-60"+"\\]", "\\[1-16-120\\]", "\\[2-8-60\\]", "\\[2-16-120\\]", "\\[3-8-60\\]", "\\[3-16-120\\]"];
roborta_vers = ["roborta_A", "roborta_B", "roborta_C"];
roborta_pars = ["T=0.1:0.4:0.5,Q=0.1:0.4:0.5"];

os.system("mkdir prism_roborta_results")
for g in roborta_grids:
    for v in roborta_vers:
        for p in roborta_pars:
            os.system("./prism  -exportresults prism_roborta_results/res_"+v+g+".txt "+"../fairness-examples/roborta/"+v+g+".smg"+ " ../fairness-examples/roborta/roborta.props -prop 1 -const "+p);

#os.system("./fairy -erew -exportresults results/res_roborta_A\[1-8-60\].txt ../fairness-examples/roborta/roborta_A\[1-8-60\].smg -const T=0.1:0.4:0.5,Q=0.1:0.4:0.5");
#os.system("./fairy -erew -exportresults results/res.txt ../fairness-examples/roborta/roborta_A\[1-16-120\].smg -const T=0.1:0.4:0.5,Q=0.1:0.4:0.5");  