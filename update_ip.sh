#!/bin/bash
if [ -z "$1" ]; then
    echo "用法: ./update_ip.sh <新IP地址>"
    echo "示例: ./update_ip.sh 192.168.1.100"
    exit 1
fi

python3 update_ip.py "$1"
