# Copyright (C) 2013 AChep@xda <artemchep@gmail.com>
#
# This script is published under the terms of the GNU GPL v2.0 license.
# See http://www.gnu.org/licenses/gpl-2.0.html

import os
import shutil
import urllib
import errno
import time
import re
import sys
from zipfile import ZipFile

TRANSLATION_FILE_NAME = 'translations-snapshot'
MAX_DOWNLOADING_TRIES = 5

def make_sure_path_exists(path):
    try:
        os.makedirs(path)
    except OSError as exception:
        if exception.errno != errno.EEXIST:
            raise

# Try to download latest translations' build from
# Crowdin.
print 'Downloading latest build from Crowdin...'
for i in range(1, MAX_DOWNLOADING_TRIES):
    try:
        urllib.urlretrieve('https://crowdin.net/download/project/acdisplay.zip', TRANSLATION_FILE_NAME + '.zip')
        break
    except IOError as exception:
        pass
else:
    print 'E: Failed to download translations! Terminating...'
    sys.exit()

# Where to extract it?
translDir = os.path.abspath(TRANSLATION_FILE_NAME)
if os.path.exists(translDir):
        shutil.rmtree(translDir)

# Unzip translations archive
print 'Unpacking archive...'
zipTest = ZipFile(TRANSLATION_FILE_NAME + '.zip')
zipTest.extractall(translDir)
zipTest.close()

# Check if some translations are broken.
print 'Searching for broken translations:'
for path, dirs, files in os.walk(translDir):
    for filename in files:
        fullpath = os.path.join(path, filename)
        fullpathstr = str(os.path.join(path, filename))

        # Log every file.
        print 'Processing file: ', fullpathstr[
            fullpathstr.find(
                os.path.basename(translDir)):]
        for line in open(fullpath):
            if re.search('(\\$|%)\\s', line) or re.search('(<!|\[CDATA)\\s', line):
                print 'Problematic line: ' + line

# Copy translations to project
print 'Copying translations to project...'
localizedProjectDir = os.path.abspath('src/localized/res')
if os.path.exists(localizedProjectDir):
        shutil.rmtree(localizedProjectDir)
        time.sleep(0.5)
make_sure_path_exists(localizedProjectDir)
for filename in os.listdir(translDir):
        if filename == 'ru' or filename == 'en':
                # Those translations are default and provided by me.
                continue

        # Check for broken formatting
        os.rename(os.path.join(translDir, filename), os.path.join(localizedProjectDir, 'values-' + filename.replace('-', '-r')))

# Remove temp files
print 'Removing temp files...'
os.remove(TRANSLATION_FILE_NAME + '.zip')
shutil.rmtree(TRANSLATION_FILE_NAME)

print ''
print 'Build and enjoy. :)'
