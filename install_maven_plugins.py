from os import listdir, getcwd, system
from os.path import isfile, join
from subprocess import call
import sys
VERSION = '2.0-early-access11'

# TODO only partially implemented comamnd line arg, do to absolute path not being truncated in the for loop
MYPATH = getcwd() if len(sys.argv) <= 1 else sys.argv[1]

onlyfiles = [ f for f in listdir(MYPATH) if isfile(join(MYPATH,f)) and (str(f)).split('.')[1] == "jar"]

for f in onlyfiles :
	command_args = " ".join(["mvn", "install:install-file", "-Dfile=" + str(f), 
		"-DgroupId=fr.x9c", "-DartifactId=" + str(f).split('.')[0],
		"-Dversion=" + VERSION, "-Dpackaging=jar"])
	system(command_args)
