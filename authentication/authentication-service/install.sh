#!/bin/bash

#installs the pre-requisites.
set -e

current_module=$1

echo "Downloading pre-requisites install scripts"
wget https://raw.githubusercontent.com/$git_user_env/$code_repo_env/$code_branch_env/authentication/$current_module/configure_biosdk.sh -O configure_biosdk.sh
wget https://raw.githubusercontent.com/$git_user_env/$code_repo_env/$code_branch_env/authentication/$current_module/configure_softhsm.sh -O configure_softhsm.sh

echo "Installating pre-requisites.."
/bin/bash configure_biosdk.sh
/bin/bash configure_softhsm.sh

echo "Installating pre-requisites completed."


exec "$@"