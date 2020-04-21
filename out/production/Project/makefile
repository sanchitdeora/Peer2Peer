JFLAGS = -g
JC = javac

default: classes

classes: clean
        $(JC) $(JFLAGS) ./*.java
        mkdir peerProcess
        cp ./* peerProcess
        
clean:
        rm -rf ./*.class
        rm -rf peerProcess
