from os import listdir, getcwd, system
from os.path import isfile, join
from subprocess import call
import sys
VERSION = '2.0-early-access11'

# TODO make this portable
SEPARATOR = "/"

file_name_base = "download-" + VERSION
file_name_php = file_name_base + ".php"
file_name_tgz = file_name_base + " .tar.gZ"

system("wget".join(["http://ocamljava.x9c.fr/preview/" + file_name_php]))
system("tar".join(["-xvf", file_name_tgz]))

MYPATH = getcwd() 

onlyfiles = [f for f in listdir(MYPATH) if isfile(join(MYPATH,f)) and (str(f)).split('.')[1] == "xml"]

for f in onlyfiles :
	pomFile = str(f)
	inferedJar = getcwd() + SEPARATOR + "ocamljava-" + VERSION + SEPARATOR + "lib" + SEPARATOR + pomFile

	command_args = " ".join(["mvn", "install:install-file", "-Dfile=" + inferedJar, "-DpomFile=" + pomFile])
	system(command_args)
