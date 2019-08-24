SOURCE_FILES = $(shell find src -type f -name '*.java')

.PHONY: classfiles all clean

all: MapGeneration.jar
clean:
	find src -type f -name '*.class' -delete
	rm MapGeneration.jar

classfiles:
	javac $(SOURCE_FILES)

MapGeneration.jar: classfiles
	cd src/; jar cmvf META-INF/MANIFEST.MF ../MapGeneration.jar *