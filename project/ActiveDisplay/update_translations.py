import os
import shutil
import urllib
from zipfile import ZipFile
import errno
import time

def make_sure_path_exists(path):
    try:
        os.makedirs(path)
    except OSError as exception:
        if exception.errno != errno.EEXIST:
            raise

# Download latest translations' build
urllib.urlretrieve('https://crowdin.net/download/project/acdisplay.zip', 'translations-snapshot.zip')

# Where to extract it?
translDir = os.path.abspath('translations-snapshot')
if os.path.exists(translDir):
        shutil.rmtree(translDir)

# Unzip translations archive
zipTest = ZipFile('translations-snapshot.zip')
zipTest.extractall(translDir)
zipTest.close()

# Copy translations to project
localizedProjectDir = os.path.abspath('src/localized/res')
if os.path.exists(localizedProjectDir):
        shutil.rmtree(localizedProjectDir)
        time.sleep(0.5)
make_sure_path_exists(localizedProjectDir)
for filename in os.listdir(translDir):
        if filename == 'ru' or filename == 'en':
                # Those translations are default and provided by me.
                continue        
        os.rename(os.path.join(translDir, filename), os.path.join(localizedProjectDir, 'values-' + filename.replace('-', '-r')))

# Remove temp files
os.remove('translations-snapshot.zip')
shutil.rmtree(translDir)
