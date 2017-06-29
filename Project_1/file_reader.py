#!/usr/bin/python
import sys

for arg in sys.argv:
    if arg.split(".")[-1]=="csv": #necessary to prevent program from reading itself
        f = open(arg)
        o = open("drive.csv", 'r+') #output file; must already exist in working directory
        o.seek(0,2) #position write at end of existing file
        line = f.readline()
        vals = line.split(",")
        for x in range(len(vals)):
            if vals[x] == "smart_187_raw":
                sm = x
        idxs = (0, 2, 3, 4, sm)
        line = f.readline()
        while line:
            vals = line.split(",")
            keep = [vals[x] for x in idxs]
            nrow = ",".join(keep)
            o.write(nrow+'\n')
            line = f.readline()
        f.close()
        o.close()
