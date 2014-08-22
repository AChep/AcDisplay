# Copyright (C) 2013 AChep@xda <artemchep@gmail.com>
#
# This script is published under the terms of the GNU GPL v2.0 license.
# See http://www.gnu.org/licenses/gpl-2.0.html

# Python 3 is required

from zipfile import ZipFile
from bs4 import BeautifulSoup
from colorama import init, Fore
import urllib.request
import shutil

import os
import errno
import time
import re
import sys

# Initialize colorama
init()

# Script will compare strings and detect formatting issues.
VALIDATE_LOCALES = True

translations_folder_name = 'translations-crowdin'
translations_folder_path = os.path.abspath(translations_folder_name)
translations_file_zip_name = '%s.zip' % translations_folder_name;

def print_warning(text):
    print(Fore.RED + text + Fore.RESET)

def print_info(text):
    print(Fore.CYAN + text + Fore.RESET)

def make_sure_path_exists(path):
    try:
        os.makedirs(path)
    except OSError as e:
        if e.errno != errno.EEXIST:
            raise

# Try to download latest translations' build from
# Crowdin.
print('Downloading latest build from Crowdin...')
for i in range(1, 5):
    try:
        url = 'https://crowdin.net/download/project/acdisplay.zip'
        with urllib.request.urlopen(url) as response, open(translations_file_zip_name, 'wb') as out_file:
            shutil.copyfileobj(response, out_file)
            break
    except IOError as e:
        pass
else:
    print_warning('Failed to download translations! Terminating...')
    sys.exit()

# Make sure that old translations are deleted.
if os.path.exists(translations_folder_path):
    print('Removing old translations...')
    shutil.rmtree(translations_folder_path)

# Unzip downloaded archive.
print('Unpacking archive...')
archive = ZipFile(translations_file_zip_name)
archive.extractall(translations_folder_path)
archive.close()
os.remove(translations_file_zip_name)

if VALIDATE_LOCALES:
    print_info('Validating resources...')

    string_res_validating = r'strings.*'
    string_res_origin = {}

    # Searches for printf-style format strings. For example:
    # > String.format("Hello %1$s! My name is %2$s.", "world", "AcDisplay")
    # Hello world! My name is AcDisplay.
    regex = r'%[1-9]{1,2}\$[a-zA-Z]'

    successful = True

    for path, dirs, files in os.walk(os.path.abspath('src/main/res/values')):
        for filename in files:
            # Validate only selected files
            if not re.match(string_res_validating, filename):
                continue

            with open(os.path.join(path, filename)) as f:
                bs = BeautifulSoup(f.read())
                for string in bs.resources.findAll('string'):
                    string_contents = str(string.string)
                    string_res_origin[string['name']] = {'count': len(re.findall(regex, string_contents)), 'contents': string_contents,}

    for path, dirs, files in os.walk(translations_folder_path):
        for filename in files:
            # Validate only selected files
            if not re.match(string_res_validating, filename):
                continue

            filepath = os.path.join(path, filename)
            with open(filepath) as f:
                bs = BeautifulSoup(f.read())
                try:
                    for string in bs.resources.findAll('string'):       
                        string_name = string['name']
                        string_contents = str(string.string)
                        if string_res_origin[string_name]['count'] != len(re.findall(regex, string_contents)):
                            print_warning('Problematic string resource found!')
                            print('\tlang:\"%s\"' % re.findall(translations_folder_name + r'\/(\w{2,3}|\w{2,3}\-\w{2,3})\/', filepath)[0])
                            print('\tname:\"%s\"' % string_name)
                            print('\torigin:\"%s\"' % string_res_origin[string_name]['contents'])
                            print('\tbroken:\"%s\"' % string_contents)
                            successful = False
                except Exception as e:
                    successful = False
                    print_warning(str(e))

            for line in open(filepath):
                if re.search(r'(<!|\[CDATA)\s', line):
                    print_warning('Problematic string resource found!')
                    print('\tlang:\"%s\"' % re.findall(translations_folder_name + r'\/(\w{2,3}|\w{2,3}\-\w{2,3})\/', filepath)[0])
                    print('\tproblematic_line:\"%s\"' % line)
                    successful = False
    if not successful:
        print_warning('Validating failed! Terminating...')
        shutil.rmtree(translations_folder_path)
        sys.exit()

print('Reorganizing files...')

files_raw = [ 'faq.html' ]
files_to_remove = [ 'play_store.txt' ]

for locale in os.listdir(translations_folder_path):
    suffix = locale.replace('-', '-r')

    # Create path for current locale directory
    path_locale = os.path.join(os.path.join(translations_folder_path, locale))

    # Create pathes for RAW and VALUES directories
    path_raw = os.path.join(translations_folder_path, 'raw-%s' % suffix)
    path_values = os.path.join(translations_folder_path, 'values-%s' % suffix)

    # Move files to raw directory
    os.makedirs(path_raw)
    for filename in files_raw:
        os.rename(os.path.join(path_locale, filename), os.path.join(path_raw, filename))

    # Remove extra files
    for filename in files_to_remove:
        os.remove(os.path.join(path_locale, filename))

    # Put the rest to values directory
    os.rename(path_locale, path_values)

print('Success!')