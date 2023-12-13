#!/bin/sh

docker build . -t tetris-cljs

docker run \
    -v .:/tetris-cljs \
    -v npm-cache:/root/.npm \
    -v m2-cache:/home/circleci/.m2/repository \
    -p 127.0.0.1:3000:3000 \
    -p 127.0.0.1:9000:9000 \
    -p 127.0.0.1:9001:9001 \
    -p 127.0.0.1:9630:9630 \
    -it tetris-cljs
