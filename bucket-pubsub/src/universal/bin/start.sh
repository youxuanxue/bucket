#!/usr/bin/env bash

INSTALL_FOLDER=~/install-bucket-pubsub
SERVICE=bucket-pubsub
VERSION=0.1
SERVICE_VERSION=${SERVICE}-${VERSION}
APP_FOLDER=${INSTALL_FOLDER}/${SERVICE_VERSION}

APP_NAME="PubsubApp"
MAIN_CLASS="com.yiyiyi.bucket.pubsub.${APP_NAME}"

function usage() {
    echo "Usage: bash $0 command env"
    echo "command:"
    echo "    start|stop|restart"
    echo "env:"
    echo "    debug|online"
    exit 1
}

if [ $# -eq 0 ];then
    usage
fi

command=$1
if [ ${command} == "start" ];then
    if [ $# -ne 2 ]; then
        usage
    fi
fi
if [ ${command} == "stop" ];then
    if [ $# -ne 2 ]; then
        usage
    fi
fi

env=$2

if [ ${env} != "debug" ]  && [ ${env} != "online" ];then
    usage
fi

echo "version: ${VERSION}"

function start() {
    echo "========= start ......."
    cd ${APP_FOLDER}/bin
    if [ ! -d ${APP_FOLDER}/logs ];then
        mkdir ${APP_FOLDER}/logs
    fi

    nohup ./${SERVICE} -main=${MAIN_CLASS} -Denv=${env} < /dev/null > ../logs/start.log 2>&1 &

    sleep 3
    tail -f ${APP_FOLDER}/logs/start.log
}

function get_pids() {
    PIDS=`jps | grep ${APP_NAME} | awk '{print $1}'`
#    PIDS=`ps x | grep "${id}" | grep -v grep | grep java | awk '{print $1}'`
    echo "stop ${APP_NAME} with pids: $PIDS"
}

function stop() {
    echo "========= stop ......."
    get_pids
    for PID in ${PIDS};do
        kill ${PID}
        echo "kill process from pid: ${PID}"
    done
    while [ ${PIDS} ]
    do
        sleep 1
        echo "waiting server to shutdown: ${PIDS}"
        get_pids
    done
}

case ${command} in
    "start")
        start ;;
    "stop")
        stop ;;
    "restart")
        stop && start ;;
esac