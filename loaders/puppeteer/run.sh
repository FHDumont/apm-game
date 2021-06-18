#!/bin/bash
# npm install

env LOAD_CONFIG="$(<../example.json)" APM_CONFIG="$(<../../nodes/appdynamics.json)" node index.js
