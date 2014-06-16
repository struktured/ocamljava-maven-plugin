#!/usr/bin/python
from os import listdir, getcwd, system, getenv
from os.path import isfile, join
import ntpath
import sys
import os
from shutil import copy, rmtree

TARGET_ARTIFACT = sys.argv[1]

BUNDLE_STAGING_FOLDER = TARGET_ARTIFACT + "-bundle"
M2_REPO = join(getenv("HOME"), ".m2", "repository", "fr", "x9c")

VERSION = '2.0-early-access11'

def ensure_dir(directory):
    if os.path.exists(directory):
        rmtree(directory)
    if not os.path.exists(directory):
        os.makedirs(directory)

def sign(file_name):
    print "Signing: " + file_name
    command = " ".join(["gpg", "-ab", file_name])
    system(command)

def bundle(artifact):
    print "Starting bundle routing for " + artifact
    bundle_staging_folder = join(getcwd(), BUNDLE_STAGING_FOLDER)
    ensure_dir(bundle_staging_folder)

    dest_jar = artifact + "-" + VERSION + ".jar"
    jar_to_copy = join(M2_REPO, artifact, VERSION, dest_jar)
    print "Copying: " + jar_to_copy + " to " + bundle_staging_folder
    copy(jar_to_copy, bundle_staging_folder)
    sign(join(bundle_staging_folder, dest_jar))

    dest_pom = artifact + "-" + VERSION + ".pom"
    pom_to_copy = join(M2_REPO, artifact, VERSION, dest_pom)
    print "Copying: " + pom_to_copy + " to " + bundle_staging_folder
    copy(pom_to_copy, bundle_staging_folder)
    sign(join(bundle_staging_folder, dest_pom))

bundle(TARGET_ARTIFACT)
