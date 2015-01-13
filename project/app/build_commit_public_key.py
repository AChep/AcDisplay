# Copyright (C) 2014 AChep@xda <artemchep@gmail.com>
#
# This script is published under the terms of the GNU GPL v2.0 license.
# See http://www.gnu.org/licenses/gpl-2.0.html

# Python 3 is required

from colorama import init, Fore

import os
import sys

# Initialize colorama
init()

def print_info(text):
    print(Fore.CYAN + text + Fore.RESET)

# Script need only three arguments:
# 1. Dir to replace in.
# 2. Encrypted public key.
# 3. Encrypted public key's salt.
assert(len(sys.argv) == 3)

for path, dirs, files in os.walk(os.path.abspath(str(sys.argv[0]))):
    for filename in files:
        filepath = os.path.join(path, filename)
        with open(filepath) as f:
            s = f.read()
        n = s.replace('\%BUILD_SCRIPT\%:ENCRYPTED_PUBLIC_KEY', str(sys.argv[1]))
        n = n.replace('\%BUILD_SCRIPT\%:ENCRYPTED_PUBLIC_KEY_SALT', str(sys.argv[2]))
        if n != s:
            with open(filepath, "w") as f:
                print_info('Rewriting ' + filepath + ' ...')
                f.write(s)

print('Success!')
