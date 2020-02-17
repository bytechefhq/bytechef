#!/bin/bash 

docker run -d \
	   --name=rabbit \
	   -p 5672:5672 \
	   -p 15672:15672 \
	   creactiviti/rabbitmq:3.8.2-management
