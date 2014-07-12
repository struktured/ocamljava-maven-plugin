#!/usr/bin/python
from os import listdir, getcwd, system
from os.path import isfile, join
import ntpath

import sys, getopt

def show_help(exit_code=0) :
  print 'install_maven_dependencies.py -v <OCAML_VERSION>-o <IS_OFFLINE> '
  sys.exit(exit_code)

VERSION = '2.0-early-access11'

FILE_NAME_BASE = "download-" + VERSION
FILE_NAME_PHP = FILE_NAME_BASE + ".php"
FILE_NAME_TGZ = FILE_NAME_BASE + ".tar.gz"

def go(is_offline=False):
  if is_offline:
		pass
  else :
	  command = " ".join(["wget", "-O", FILE_NAME_TGZ,
		  "http://ocamljava.x9c.fr/preview/" + FILE_NAME_PHP])
	  system(command)

  command = " ".join(["tar", "-zxvf", FILE_NAME_TGZ])
  print "Extracting files: " + command
  system(command)
  mypath = join(getcwd(), "ocamljava-" + VERSION, "lib")

  only_files = [f for f in listdir(mypath) if
		  isfile(join(mypath, f)) and (str(f)).split('.')[1] == "jar"]

  for f in only_files:
      jarFile = join(getcwd(), "ocamljava-" + VERSION, "lib", str(f))
      baseName = ntpath.basename(jarFile).split('.')[0]
      pomFile = join(getcwd(), baseName + ".pom")
      command_args = " ".join(["mvn", "install:install-file",
	      "-Dfile=" + jarFile, "-DpomFile=" + pomFile])
      system(command_args)


def main(argv):
    offline = False
    try:
	    opts, arg = getopt.getopt(argv,"hov",)
    except getopt.GetoptError:
	    show_help(2)
    for opt, arg in opts :
       if opt == '-h' :
               show_help()
       if opt in ("-o", "--offline") :
	       offline=True
	       pass
    go(is_offline=offline)

if __name__ == "__main__":
	main(sys.argv[1:])


