JAVA=java
JAVAC=javac

JAVAFX_PATH=D:/POO/javafx-sdk-24.0.1/lib
SRC_DIRS=controller io model simulator view
BIN=bin
JAR=jSerialComm-2.11.0.jar
MAIN_CLASS=main

CLASSPATH=$(JAR)
MODULES=javafx.controls,javafx.fxml

SOURCES=$(wildcard $(SRC_DIRS:%=%/*.java)) main.java

# Detectar sistema operativo
ifeq ($(OS),Windows_NT)
    DETECTED_OS := Windows
    RM_DIR := if exist $(BIN) rmdir /s /q $(BIN)
    MK_DIR := if not exist $(BIN) mkdir $(BIN)
    PATH_SEP := ;
else
    DETECTED_OS := Unix
    RM_DIR := rm -rf $(BIN)
    MK_DIR := mkdir -p $(BIN)
    PATH_SEP := :
endif

.PHONY: all run clean

all: $(BIN)/$(MAIN_CLASS).class

$(BIN)/$(MAIN_CLASS).class: $(SOURCES)
	@echo "Compilando en $(DETECTED_OS)..."
	$(MK_DIR)
	$(JAVAC) --module-path $(JAVAFX_PATH) --add-modules $(MODULES) \
	  -cp $(CLASSPATH) -d $(BIN) $(SOURCES)

run:
	@echo "Ejecutando en $(DETECTED_OS)..."
	$(JAVA) --module-path $(JAVAFX_PATH) --add-modules $(MODULES) \
	  -cp "$(BIN)$(PATH_SEP)$(JAR)" $(MAIN_CLASS)

clean:
	@echo "Limpiando en $(DETECTED_OS)..."
	$(RM_DIR)
