#!/usr/bin/python
from os import listdir, getcwd, system, getenv
from os.path import isfile, join
import sys
import os
from shutil import copy, rmtree


VERSION = '2.0-early-access11'

M2_REPO = join(getenv("HOME"), ".m2", "repository", "fr", "x9c")

def find_artifacts() :
    return [str(f).split('.')[0] for f in listdir(getcwd()) if
       isfile(join(getcwd(), f)) and len(str(f).split('.')) > 1 and
       str(f).split('.')[1] == "pom"]

TARGET_ARTIFACTS = sys.argv[1] if len(sys.argv) > 1 else find_artifacts()
def ensure_dir(directory):
    if os.path.exists(directory):
        rmtree(directory)
    if not os.path.exists(directory):
        os.makedirs(directory)

def sign(file_name):
    print "Signing: " + file_name
    command = " ".join(["gpg", "-ab", file_name])
    system(command)

def create_fake_jar(base_name, to_path):
    full_path = join(to_path, base_name + ".jar")
    print "Creating fake jar: " + full_path
    command = " ".join(["jar", "cf", full_path, "README"])
    system(command)

def create_bundled_jar(artifact, bundle_path):
    artifact_with_version = artifact + "-" + VERSION
    jar_to_create = join(getcwd(), artifact_with_version + "-bundle.jar")
    print "Creating bundled jar: " + jar_to_create
    command = " ".join(["jar", "cvf", jar_to_create,
        "-C", bundle_path, artifact_with_version + ".jar",
	"-C", bundle_path, artifact_with_version + ".jar.asc",
	"-C", bundle_path, artifact_with_version + ".pom",
	"-C", bundle_path, artifact_with_version + ".pom.asc",
	"-C", bundle_path, artifact_with_version + "-sources.jar",
	"-C", bundle_path, artifact_with_version + "-sources.jar.asc",
	"-C", bundle_path, artifact_with_version + "-javadoc.jar",
	"-C", bundle_path, artifact_with_version + "-javadoc.jar.asc"])
    system(command)

def bundle(artifact):
    print "Starting bundle routing for " + artifact
    bundle_staging_folder = join(getcwd(), artifact + "-bundle")
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

    jar_prefix = artifact + "-" + VERSION + "-sources"
    create_fake_jar(jar_prefix, bundle_staging_folder)
    sign(join(bundle_staging_folder, jar_prefix + ".jar"))

    jar_prefix = artifact + "-" + VERSION + "-javadoc"
    create_fake_jar(jar_prefix, bundle_staging_folder)
    sign(join(bundle_staging_folder, jar_prefix + ".jar"))

    create_bundled_jar(artifact, bundle_staging_folder)

for t in TARGET_ARTIFACTS:
    bundle(t)
