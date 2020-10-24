#!/bin/bash

# Configure environment
export DISPLAY=:1
cd /home/ericballard7/Desktop/

# Update SSLChat
wget -O SSLChat.jar https://www.dropbox.com/s/m0ixozualnhrvkm/SSLChat.jar?dl=1

# Launch
java -jar SSLChat.jar