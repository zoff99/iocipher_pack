# Path to the IOCipher JNI jar located in the parent directory
IOCIPHER_JAR = ../iocipher_linux-1.0.31.jar

all: run

compile_test:
	@echo ">> Compiling IOCipher Unit Test files..."
	javac -cp "$(IOCIPHER_JAR):." IOCipherUnitTest.java Test*.java

run: compile_test
	@echo ">> Running IOCipher Unit Tests..."
	# Ensure JNI library paths are available if needed, though IOCipher usually bundles them
	java -cp ".:$(IOCIPHER_JAR)" IOCipherUnitTest

clean:
	@echo ">> Cleaning up..."
	# Remove all compiled test class files
	rm -f *.class
	# Remove the test container database file (in case of hard crashes)
	rm -f unit_test_iocipher.db
