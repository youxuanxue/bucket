#!/usr/bin/env bash


HOST_DEBUG=127.0.0.1
SERVICE=bucket-pubsub
VERSION=0.1
SERVICE_VERSION=${SERVICE}-${VERSION}
INSTALL_FOLDER=install-bucket-pubsub

SHELL_FILE=$0
SHELL_FOLDER=`dirname $0`

ZIP_FILE=${SERVICE_VERSION}.zip

TODAY=`date +%Y-%m-%d`
TODAY_ZIP_FILE=${TODAY}-${ZIP_FILE}

cd ${SHELL_FOLDER}/../../../..

function usage() {
    echo "Usage: bash $0 action [host] [env, default debug]"
    echo "action:"
    echo "      zip | install | zip_install | start | stop | restart"
    echo "env:"
    echo "      debug|online"
    echo "example:"
    echo "      bash $0 zip_start 47.93.175.164 online"
    echo "      bash $0 zip_start 10.144.114.191 online"
    echo "      bash $0 zip_start 59.110.47.10 debug"
    echo "      bash $0 zip_start 10.27.243.35 debug"
    exit 1
}

function args() {
    if [ $# -lt 1 ];then
        usage
    fi

    action=$1 && shift
    echo "action: ${action}"

    host=${HOST_DEBUG}
    if [ $# -gt 0 ];then
        host=$1
        shift
    fi
    echo "host: ${host}"
    env=debug
    if [ $# -gt 0 ];then
        env=$1
        shift
    fi
    echo "env: ${env}"
    echo "version: ${VERSION}"
    echo "service: ${SERVICE}"
}


function zip() {
    echo "========= zip ......"
    sbt clean ${SERVICE}/dist
}

function install() {
    echo "copy zip file to $host:${INSTALL_FOLDER}/${TODAY_ZIP_FILE}"
    ssh -l work ${host} "mkdir -p ${INSTALL_FOLDER}"
    scp ${SERVICE}/target/universal/${ZIP_FILE} work@${host}:${INSTALL_FOLDER}/${TODAY_ZIP_FILE}
    ssh -l work ${host} "cd ${INSTALL_FOLDER};unzip -oq ${TODAY_ZIP_FILE};rm -rf ${SERVICE};ln -s ${SERVICE_VERSION} ${SERVICE}"
}

function restart() {
    stop
    start
}

function start() {
    ssh -l work ${host} "cd ~;sh ${INSTALL_FOLDER}/${SERVICE}/bin/start.sh start ${env}"
}

function stop() {
    ssh -l work ${host} "cd ~;sh ${INSTALL_FOLDER}/${SERVICE}/bin/start.sh stop ${env}"
}

args $@

case ${action} in
    "zip")
        zip ;;
    "install")
        install ;;
    "zip_install")
        zip && install ;;
    "zip_start")
        zip && install && restart ;;
    "start")
        start ;;
    "restart")
        restart ;;
    "stop")
        stop ;;
esac
