FROM cimg/openjdk:15.0-node

USER root

RUN mkdir -p /home/circleci/.m2/
RUN mkdir -p /tetris-cljs/
WORKDIR /tetris-cljs/

RUN npm i -g shadow-cljs

# http
EXPOSE 3000
# shadow-cljs server 
EXPOSE 9630
# nRepl
EXPOSE 9000
EXPOSE 9001

CMD ["npx", "shadow-cljs", "watch", "frontend"]