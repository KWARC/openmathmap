M = csvread('similarities.txt'); Y = mdscale(M,2); csvwrite('mdscale.txt',Y);
