# JMusicBot-ML Docker image
# Based on the JMusicBot-JP Dockerfile by CyberRex (CyberRex0); adapted to use a local jar.
#
# Usage:
#   1) Put the built JMusicBot-ML.jar next to this Dockerfile.
#   2) docker build -t jmusicbot-ml .
#   3) docker run -d --name jmusicbot-ml -v "$(pwd)/data:/opt/jmusicbot-ml/data" jmusicbot-ml
#      (config.txt, Playlists etc. are kept in /opt/jmusicbot-ml/data)

FROM eclipse-temurin:25-jre

RUN mkdir -p /opt/jmusicbot-ml/data

COPY JMusicBot-ML.jar /opt/jmusicbot-ml/jmusicbot-ml.jar

WORKDIR /opt/jmusicbot-ml/data

CMD ["java", "--enable-native-access=ALL-UNNAMED", "-Dnogui=true", "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-jar", "/opt/jmusicbot-ml/jmusicbot-ml.jar"]
