#!/bin/bash

# Warning load this file using:
# source `dirname $0`/javaenv.sh

BASE=`dirname $0`/../

case `uname` in
  CYGWIN*)
    CP=$( echo "$BASE"/lib/*jar "$BASE/target" "$BASE/target/classes" "$BASE"/target/dependency/*.jar . | sed 's/ /;/g')
    ;;
  *)
    CP=$( echo "$BASE"/lib/*jar "$BASE/target" "$BASE/target/classes" "$BASE"/target/dependency/*.jar . | sed 's/ /:/g')
esac
#echo $CP

# Find Java
if [ "$JAVA_HOME" = "" ] ; then
    JAVA="java -server"
else
    JAVA="$JAVA_HOME/bin/java -server"
fi