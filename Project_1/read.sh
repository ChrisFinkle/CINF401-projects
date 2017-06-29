#!/bin/bash
PAR="/bigdata/data/backblaze/"
echo "Date,Model,Capacity,Failure,Smart_187_raw" > drive.csv
for year in `ls $PAR`; do
	for x in `ls $PAR/$year`; do 
		echo $x
		./file_reader.py $PAR/$year/$x; 
	done
done
