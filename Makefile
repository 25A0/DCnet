
all:
	make dcnet

dcnet: 
	javac -d bin/ -cp src/ src/component/Main.java

server:
	make dcnet
	./dcnet.sh 