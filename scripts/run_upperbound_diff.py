# A simple script to run the experiments in the repo
# it runs an example that shows the differences between the upper bound computed by prism and fairy
# the results are output to a folder "results_upperbound"
import os;

os.system("mkdir upperbound_results");
# we need to increase the number of iterations of prism
os.system("./prism -maxiters 1000000 -exportresults upperbound_results/res_prism_upperbound.txt ../fairness-examples/basics/test4.smg ../fairness-examples/basics/test4.props -prop 1 -const Q=0.99");
os.system("./fairy -v1 -erew -exportresults upperbound_results/res_fairy_upperbound.txt ../fairness-examples/basics/test4.smg  -const Q=0.99");
