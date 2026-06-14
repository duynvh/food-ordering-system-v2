#!/bin/bash

echo "Deleting Kafka and Zookeeper volumes"

yes | rm -r ./volumes/kafka/broker-1/*
yes | rm -r ./volumes/kafka/broker-2/*
yes | rm -r ./volumes/kafka/broker-3/*

yes | rm -r ./volumes/zookeeper/data/*
yes | rm -r ./volumes/zookeeper/transactions/*