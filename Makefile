JAVAC=javac
JAVA=java
SRC_DIR=src/main/java
BIN_DIR=build/classes
MAIN_CLASS=traffic.TrafficSimulatorApp

SOURCES := $(shell find $(SRC_DIR) -name "*.java")

all: $(BIN_DIR)
	$(JAVAC) -d $(BIN_DIR) $(SOURCES)

$(BIN_DIR):
	mkdir -p $(BIN_DIR)

run: all
	$(JAVA) -cp $(BIN_DIR) $(MAIN_CLASS)

clean:
	rm -rf $(BIN_DIR)

.PHONY: all clean run
