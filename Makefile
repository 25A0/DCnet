
all:
	make dcnet

dcnet: 
	mkdir -p bin
	javac -d bin/ -cp src/ src/component/Main.java
