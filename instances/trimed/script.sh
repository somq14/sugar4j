#!/bin/bash

PROJECT_BASE=../../
for ins in `ls ${PROJECT_BASE}/instances/Instance*.txt`;
do
  echo "`basename ${ins%.txt}`_1week.txt"
  java -cp ${PROJECT_BASE}/target/coptool-0.4-jar-with-dependencies.jar \
    jp.ac.meiji.igusso.scheduling.SchedulingProblemUtil \
    ${ins} 1 > "`basename ${ins%.txt}`_1week.txt"
done

for ins in `ls ${PROJECT_BASE}/instances/Instance*.txt`;
do
  echo "`basename ${ins%.txt}`_2week.txt"
  java -cp ${PROJECT_BASE}/target/coptool-0.4-jar-with-dependencies.jar \
    jp.ac.meiji.igusso.scheduling.SchedulingProblemUtil \
    ${ins} 2 > "`basename ${ins%.txt}`_2week.txt"
done

for ins in `ls ${PROJECT_BASE}/instances/Instance*.txt`;
do
  echo "`basename ${ins%.txt}`_3week.txt"
  java -cp ${PROJECT_BASE}/target/coptool-0.4-jar-with-dependencies.jar \
    jp.ac.meiji.igusso.scheduling.SchedulingProblemUtil \
    ${ins} 3 > "`basename ${ins%.txt}`_3week.txt"
done
