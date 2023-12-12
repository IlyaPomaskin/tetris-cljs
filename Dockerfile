FROM cimg/openjdk:15.0-node

USER root

RUN mkdir -p /home/circleci/.m2/
RUN mkdir -p /tetris-cljs/
WORKDIR /tetris-cljs/

RUN npm i -g shadow-cljs
RUN shadow-cljs info

EXPOSE 3000 # http
EXPOSE 9630 # shadow-cljs server 
EXPOSE 9000 # nRepl

CMD ["npx", "shadow-cljs", "watch", "frontend"]