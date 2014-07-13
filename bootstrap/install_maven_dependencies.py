#!/usr/bin/python
from os import listdir, getcwd, system
from os.path import isfile, join
import ntpath
import sys, getopt

OCAMLJAVA_PRIVATE_ACCESS12 = '2.0-early-access12'
OCAMLJAVA_ACCESS11 = '2.0-early-access11'
VERSIONS = [OCAMLJAVA_ACCESS11, OCAMLJAVA_PRIVATE_ACCESS12]
VERSION = OCAMLJAVA_PRIVATE_ACCESS12

def show_help(exit_code=0) :
  print 'install_maven_dependencies.py -v <OCAML_VERSION> -o <IS_OFFLINE> '
  print '\tknown versions are ' + str(VERSIONS)
  sys.exit(exit_code)

USER="ocj-tmp"
PASSWORD="ocpjui14"

def go(is_offline=False, version=VERSION):
    if is_offline:
        pass
    else:
            file_name_base = "ocamljava-" + version
            file_name_tgz = file_name_base + ".tar.gz"
	    url_root = "http://ocamljava.x9c.fr/preview/tmp/" if version == OCAMLJAVA_PRIVATE_ACCESS12 else "http://ocamljava.x9c.fr/preview/"
	    url_file = file_name_tgz if version == OCAMLJAVA_PRIVATE_ACCESS12 else "download-" + version + ".php"  
	    url = url_root + url_file
	    command = " ".join(["wget", "--user", USER, "--password", PASSWORD, "-O", file_name_tgz, url])
	    system(command)
	    command = " ".join(["tar", "-zxvf", file_name_tgz])
	    print "Extracting files: " + command
	    system(command)
    mypath = join(getcwd(), "ocamljava-" + version, "lib")

    jar_files = [f for f in listdir(mypath) if
		  isfile(join(mypath, f)) and (str(f)).split('.')[1] == "jar"]

    for jar_file in jar_files:
      print "Installing " + str(jar_file)
      abs_jar_file = join(getcwd(), "ocamljava-" + version, "lib", str(jar_file))
      base_name = ntpath.basename(abs_jar_file).split('.')[0]
      pom_file = join(getcwd(), base_name + ".pom")
      command_args = " ".join(["mvn", "install:install-file",
	      "-Dfile=" + abs_jar_file, "-DpomFile=" + pom_file])
      print command_args
      system(command_args)


def main(argv):
    offline = False
    version = VERSION
    try:
	    opts, args = getopt.getopt(argv,"hov:",)
    except getopt.GetoptError:
	    show_help(2)
    for opt, arg in opts:
       if opt == '-h':
               show_help()
       if opt in ("-o", "--offline"): 
	       offline=True
	       pass
       if opt in ("-v", "--version"):
	       version = str(arg)
	       pass
    go(is_offline=offline, version=version)

if __name__ == "__main__":
	main(sys.argv[1:])

