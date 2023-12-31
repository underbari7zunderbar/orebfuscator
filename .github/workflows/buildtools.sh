#!/bin/bash

build () {
  JAVA_PATH=$"JAVA_HOME_$2_X64"
  export JAVA_HOME=${!JAVA_PATH}

  echo "Building v$1 with java-$2 ($JAVA_HOME)"

  rm -rf $1
  mkdir $1
  cd $1

  curl -o BuildTools.jar https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar
  "$JAVA_HOME/bin/java" -jar BuildTools.jar --rev $1 --remapped

  cd ..
}

checkVersion () {
  echo Checking version $1

  if [ ! -d ~/.m2/repository/org/spigotmc/spigot/$1-R0.1-SNAPSHOT ]; then
    build $1 $2
  fi
}

checkVersion "1.20.4" "17"
