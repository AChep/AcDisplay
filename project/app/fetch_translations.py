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
import re
import sys

VALIDATE_LOCALES = False

translations_folder_name = 'translations-crowdin'
translations_folder_path = os.path.abspath(translations_folder_name)
translations_file_zip_name = '%s.zip' % translations_folder_name


def print_warning(text):
    print(Fore.RED + text + Fore.RESET)


def print_info(text):
    print(Fore.CYAN + text + Fore.RESET)


def download(url, name):
    for i in range(1, 5):
        try:
            with urllib.request.urlopen(url) as r, open(name, 'wb') as out_file:
                shutil.copyfileobj(r, out_file)
                return True
        except IOError as e:
            pass
    return False

# Initialize colorama
init()

# Make sure that old translations are deleted.
if os.path.exists(translations_folder_path) or os.path.exists(translations_file_zip_name):
    print('Removing old translations...')
    if os.path.exists(translations_folder_path):
        shutil.rmtree(translations_folder_path)
    if os.path.exists(translations_file_zip_name):
        os.remove(translations_file_zip_name)

# Download the latest build of translations from crowdin.net
print('Downloading latest build from <translate.acdisplay.org>...')
if not download('http://translate.acdisplay.org/download/project/acdisplay.zip', translations_file_zip_name):
    print_warning('Failed to download translations! Terminating...')
    sys.exit()

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

            with open(os.path.join(path, filename), encoding="utf8") as f:
                bs = BeautifulSoup(f.read())
                for string in bs.resources.findAll('string'):
                    string_contents = str(string.string)
                    string_res_origin[string['name']] = {'count': len(re.findall(regex, string_contents)),
                                                         'contents': string_contents, }

    for path, dirs, files in os.walk(translations_folder_path):
        for filename in files:
            # Validate only selected files
            if not re.match(string_res_validating, filename):
                continue

            filepath = os.path.join(path, filename)
            with open(filepath, encoding="utf8") as f:
                bs = BeautifulSoup(f.read())
                try:
                    for string in bs.resources.findAll('string'):
                        string_name = string['name']
                        string_contents = str(string.string)
                        if string_res_origin[string_name]['count'] != len(re.findall(regex, string_contents)):
                            print_warning('Problematic string resource found!')
                            print('\tlang:\"%s\"' %
                                  re.findall(translations_folder_name + r'/(\w{2,3}|\w{2,3}\-\w{2,3})/', filepath)[0])
                            print('\tname:\"%s\"' % string_name)
                            print('\torigin:\"%s\"' % string_res_origin[string_name]['contents'])
                            print('\tbroken:\"%s\"' % string_contents)
                            successful = False
                except Exception as e:
                    successful = False
                    print_warning(str(e))

            for line in open(filepath, encoding="utf8"):
                if re.search(r'(<!|\[CDATA)\s', line):
                    print_warning('Problematic string resource found!')
                    print('\tlang:\"%s\"' %
                          re.findall(translations_folder_name + r'/(\w{2,3}|\w{2,3}\-\w{2,3})/', filepath)[0])
                    print('\tproblematic_line:\"%s\"' % line)
                    successful = False
    if not successful:
        print_warning('Validating failed! Terminating...')
        shutil.rmtree(translations_folder_path)
        sys.exit()

print('Reorganizing files...')
files_raw = ['faq.html']
files_to_remove = ['play_store.txt']
l = {locale: [locale.replace('-', '-r')] for locale in os.listdir(translations_folder_path)}
l['es-ES'].append('es')
for dir, v in l.items():
    dir_path = os.path.join(os.path.join(translations_folder_path, dir))
    # Remove extra files
    for k in files_to_remove:
        os.remove(os.path.join(dir_path, k))
    for locale in v:
        raw_path = os.path.join(translations_folder_path, 'raw-%s' % locale)
        values_path = os.path.join(translations_folder_path, 'values-%s' % locale)
        # Copy all to values directory
        shutil.copytree(dir_path, values_path)
        # Move files to raw directory
        os.makedirs(raw_path)
        for k in files_raw:
            shutil.copy(os.path.join(dir_path, k), os.path.join(raw_path, k))
            os.remove(os.path.join(values_path, k))
    shutil.rmtree(dir_path)
print('Success!')
