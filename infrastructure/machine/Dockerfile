FROM openjdk:11-jre-slim AS builder

ARG APPD_AGENT_SHA256
ARG APPD_AGENT_VERSION 

RUN apt-get update -y; \
    apt-get install -y unzip;

COPY machineagent-bundle-64bit*.zip /
RUN if [ "x${APPD_AGENT_SHA256}" != "x" ]; then \ 
	echo "${APPD_AGENT_SHA256} *machineagent-${APPD_AGENT_VERSION}.zip" >> appd_checksum \
    && sha256sum -c appd_checksum \
    && rm appd_checksum \
    && unzip -oq machineagent-bundle-64bit*.zip -d /tmp; \
    else \
     unzip -oq machineagent-bundle-64bit*.zip -d /tmp; \
    fi

FROM openjdk:11-jre-slim

RUN apt-get update -y; \
    apt-get upgrade -y; \
    apt-get install -y --no-install-recommends iproute2 procps dumb-init coreutils sed; \
	rm -rf /var/lib/apt/lists/*

RUN apt-get clean autoclean
RUN apt-get autoremove --yes
RUN rm -rf /var/lib/{apt,dpkg,cache,log}/

COPY --from=builder /tmp /opt/appdynamics

ENV MACHINE_AGENT_HOME /opt/appdynamics

WORKDIR ${MACHINE_AGENT_HOME}

COPY updateAnalyticsAgent.sh ./updateAnalyticsAgent.sh
RUN chmod +x ./updateAnalyticsAgent.sh

COPY startup.sh ./startup.sh
RUN chmod +x ./startup.sh

EXPOSE 9090

CMD "./startup.sh"