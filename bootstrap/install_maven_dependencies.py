#!/usr/bin/python
from os import listdir, getcwd, system
from os.path import isfile, join
import ntpath

VERSION = '2.0-early-access11'

FILE_NAME_BASE = "download-" + VERSION
FILE_NAME_PHP = FILE_NAME_BASE + ".php"
FILE_NAME_TGZ = FILE_NAME_BASE + ".tar.gz"

COMMAND = " ".join(["wget", "-O", FILE_NAME_TGZ,
	"http://ocamljava.x9c.fr/preview/" + FILE_NAME_PHP])

print "Getting ocamljava: " + COMMAND
system(COMMAND)

COMMAND = " ".join(["tar", "-zxvf", FILE_NAME_TGZ])

print "Extracting files: " + COMMAND
system(COMMAND)

MYPATH = join(getcwd(), "ocamljava-" + VERSION, "lib")

ONLY_FILES = [f for f in listdir(MYPATH) if
		isfile(join(MYPATH, f)) and (str(f)).split('.')[1] == "jar"]

for f in ONLY_FILES:
    jarFile = join(getcwd(), "ocamljava-" + VERSION, "lib", str(f))
    baseName = ntpath.basename(jarFile).split('.')[0]
    pomFile = join(getcwd(), baseName + ".pom")
    command_args = " ".join(["mvn", "install:install-file",
	    "-Dfile=" + jarFile, "-DpomFile=" + pomFile])
    system(command_args)
