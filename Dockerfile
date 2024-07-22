FROM azul/zulu-openjdk-alpine:21-jre-headless-latest AS prod
LABEL authors="ryanmoelter"

WORKDIR /splity
COPY ./build/install/splity/ .
RUN ln -s /splity/bin/splity /splity/splity
RUN mkdir /splity/cache
ENTRYPOINT ["/splity/splity"]
