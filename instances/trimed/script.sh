#!/bin/bash

PROJECT_BASE=../../
for ins in `ls ${PROJECT_BASE}/instances/Instance*.txt`;
do
  java -cp ${PROJECT_BASE}/target/coptool-0.3-jar-with-dependencies.jar \
    jp.ac.meiji.igusso.scheduling.SchedulingProblemUtil \
    ${ins} 1 > "`basename ${ins%.txt}`_1week.txt"
done
