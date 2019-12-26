#!/bin/sh   

BIN_PATH=/root/C0/bin
SRC_PATH=/root/C0/src

rm -f $SRC_PATH/sources.list
find $SRC_PATH -name *.java > $SRC_PATH/sources.list

rm -rf $BIN_PATH
mkdir $BIN_PATH

javac -d $BIN_PATH  @$SRC_PATH/sources.list -Xlint
