FROM centos:centos7

RUN sed -i 's/mirrorlist/#mirrorlist/g' /etc/yum.repos.d/CentOS-*
RUN sed -i 's|#baseurl=http://mirror.centos.org|baseurl=http://vault.centos.org|g' /etc/yum.repos.d/CentOS-*


#RUN yum update -y
RUN yum install -y iproute \
  net-tools \
  tcpdump \
  curl \
  unzip \
  sysvinit-tools \
  openssh-clients
RUN rm -rf /var/cache/yum
RUN yum clean all

WORKDIR /opt/appdynamics/netviz

# copy NetViz agent contents
COPY appd-netviz-*.zip .
RUN unzip -oq appd-netviz-*.zip

# run the agent install script
RUN ./install.sh \
  && sed -i -e "s|enable_netlib = 1|enable_netlib = 0|g" ./conf/agent_config.lua \
  && sed -i -e "s|WEBSERVICE_IP=.*|WEBSERVICE_IP=\"0.0.0.0\"|g" ./conf/agent_config.lua

# cleanup
RUN rm -f appd-netviz-*.zip 

# default command to run for the agent
CMD  ./bin/appd-netagent -c ./conf -l ./logs -r ./run