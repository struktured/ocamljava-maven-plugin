#!/usr/bin/python
from os import listdir, getcwd, system
from os.path import isfile, join
import ntpath
import sys, getopt

OCAMLJAVA_ALPHA1 = '2.0-alpha1'

VERSIONS = [OCAMLJAVA_ALPHA1]
VERSION = OCAMLJAVA_ALPHA1

def show_help(exit_code=0) :
  print 'install_maven_dependencies.py -v <OCAML_VERSION> -o <IS_OFFLINE> -j <SPECIFIC_JAR>'
  print '\tknown versions are ' + str(VERSIONS)
  sys.exit(exit_code)

USER="ocj-tmp"
PASSWORD="ocpjui14"

def go(is_offline=False, version=VERSION, specific_jar=None):
    if is_offline:
        pass
    else:
            file_name_base = "ocamljava-" + version
            file_name_tgz = file_name_base + ".tar.gz"
	    url_root = "http://www.ocamljava.org/downloads/" 
	    url_file = "download-" + version + ("-bin" if VERSION == OCAMLJAVA_ALPHA1 else "") + ".php"  
	    url = url_root + url_file
	    command = " ".join(["wget", "--user", USER, "--password", PASSWORD, "-O", file_name_tgz, url])
	    system(command)
	    command = " ".join(["tar", "-zxvf", file_name_tgz])
	    print "Extracting files: " + command
	    system(command)
    mypath = join(getcwd(), "ocamljava-" + version, "lib")

    if specific_jar == None :
        jar_files = [f for f in listdir(mypath) if isfile(join(mypath, f)) and (str(f)).split('.')[1] == "jar"]
    else :
	jar_files = [specific_jar]

    for jar_file in jar_files:
      print "Installing " + str(jar_file)
      abs_jar_file = specific_jar if specific_jar != None else join(getcwd(), "ocamljava-" + version, "lib", str(jar_file))
      base_name = ntpath.basename(abs_jar_file).split('.')[0]
      pom_file = join(getcwd(), base_name + ".pom")
      command_args = " ".join(["mvn", "install:install-file",
	      "-Dfile=" + abs_jar_file, "-DpomFile=" + pom_file])
      print command_args
      system(command_args)


def main(argv):
    offline = False
    version = VERSION
    specific_jar = None
    try:
	    opts, args = getopt.getopt(argv,"hov:j:",)
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
       if opt in ("-j", "--jar"):
	       specific_jar = str(arg)
    go(is_offline=offline, version=version, specific_jar=specific_jar)

if __name__ == "__main__":
	main(sys.argv[1:])

