# JTableFX

JTableFX is an open-source JavaFX library for building high-performance, spreadsheet-style table views with rich interaction and editable data operations. It is designed for desktop applications that need more than a basic table widget, with support for undoable editing, resizing, reordering, hiding & unhiding, sorting, filtering, inserting and deleting rows and columns.

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Release](https://img.shields.io/github/v/release/dazzle50/JTableFX)](https://github.com/dazzle50/JTableFX/releases)
[![DeepWiki](https://img.shields.io/badge/DeepWiki-dazzle50%2FJTableFX-blue.svg?logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACwAAAAyCAYAAAAnWDnqAAAAAXNSR0IArs4c6QAAA05JREFUaEPtmUtyEzEQhtWTQyQLHNak2AB7ZnyXZMEjXMGeK/AIi+QuHrMnbChYY7MIh8g01fJoopFb0uhhEqqcbWTp06/uv1saEDv4O3n3dV60RfP947Mm9/SQc0ICFQgzfc4CYZoTPAswgSJCCUJUnAAoRHOAUOcATwbmVLWdGoH//PB8mnKqScAhsD0kYP3j/Yt5LPQe2KvcXmGvRHcDnpxfL2zOYJ1mFwrryWTz0advv1Ut4CJgf5uhDuDj5eUcAUoahrdY/56ebRWeraTjMt/00Sh3UDtjgHtQNHwcRGOC98BJEAEymycmYcWwOprTgcB6VZ5JK5TAJ+fXGLBm3FDAmn6oPPjR4rKCAoJCal2eAiQp2x0vxTPB3ALO2CRkwmDy5WohzBDwSEFKRwPbknEggCPB/imwrycgxX2NzoMCHhPkDwqYMr9tRcP5qNrMZHkVnOjRMWwLCcr8ohBVb1OMjxLwGCvjTikrsBOiA6fNyCrm8V1rP93iVPpwaE+gO0SsWmPiXB+jikdf6SizrT5qKasx5j8ABbHpFTx+vFXp9EnYQmLx02h1QTTrl6eDqxLnGjporxl3NL3agEvXdT0WmEost648sQOYAeJS9Q7bfUVoMGnjo4AZdUMQku50McDcMWcBPvr0SzbTAFDfvJqwLzgxwATnCgnp4wDl6Aa+Ax283gghmj+vj7feE2KBBRMW3FzOpLOADl0Isb5587h/U4gGvkt5v60Z1VLG8BhYjbzRwyQZemwAd6cCR5/XFWLYZRIMpX39AR0tjaGGiGzLVyhse5C9RKC6ai42ppWPKiBagOvaYk8lO7DajerabOZP46Lby5wKjw1HCRx7p9sVMOWGzb/vA1hwiWc6jm3MvQDTogQkiqIhJV0nBQBTU+3okKCFDy9WwferkHjtxib7t3xIUQtHxnIwtx4mpg26/HfwVNVDb4oI9RHmx5WGelRVlrtiw43zboCLaxv46AZeB3IlTkwouebTr1y2NjSpHz68WNFjHvupy3q8TFn3Hos2IAk4Ju5dCo8B3wP7VPr/FGaKiG+T+v+TQqIrOqMTL1VdWV1DdmcbO8KXBz6esmYWYKPwDL5b5FA1a0hwapHiom0r/cKaoqr+27/XcrS5UwSMbQAAAABJRU5ErkJggg==)](https://deepwiki.com/dazzle50/JTableFX)
[![X (formerly Twitter) Follow](https://img.shields.io/twitter/follow/JTableFX)](https://x.com/JTableFX)

## Why JTableFX?

JTableFX is intended for applications that need spreadsheet-style behaviour inside a JavaFX UI. It focuses on editable grid data, rich row and column operations, undoable changes, and responsive handling of larger datasets.


## Features

- **Spreadsheet-style table interaction**
  - Cell, row, and column selection
  - Mouse and keyboard navigation
  - Zoom support (`Ctrl +/-/0`)

- **Editable and undoable table operations**
  - Built-in support for multiple cell value types with specialised editors
  - Resize, reorder, hide, unhide, sort, and filter rows and columns
  - Insert, delete, and append table data
  - Undo/redo framework supporting all operations

- **Performance-oriented design**
  - Efficient rendering and interaction for larger datasets
  - Responsive handling of slower operations
  - Optimised internal index, mapping, and layout behaviour

- **Extensible architecture**
  - Model-view-controller design supporting multiple views from single data sources
  - Flexible APIs for integrating custom data models and behaviours
  - Utility classes including date, time, and datetime handling and parsing

## Demo Application

The demo application showcases JTableFX in a range of scenarios, including standard table usage, larger datasets, editable data, and undo/redo support.

### Default table
![Demo](images/Demo-app.png "Demo application - default table")

### Large dataset view
![Demo](images/Demo-large.png "Demo application - large table")

### Editable table example
![Demo](images/Demo-edit.png "Demo application - edit table")

![Demo](images/Demo-edit2.png "Demo application - edit table")

### Undo stack window
![Demo](images/Undostack-window.png "Undostack window")

## Getting Started

### Requirements

- **Java 21+**
- **JavaFX 21+** if your JDK does not bundle JavaFX

A JDK with bundled JavaFX can simplify setup, such as [Azul Zulu builds](https://www.azul.com/downloads/?package=jdk-fx) with JavaFX included.

### Running the demo from the command line

Download the latest JAR from the [Releases](https://github.com/dazzle50/JTableFX/releases) page and run it from the command line.

If your Java runtime does not include JavaFX, supply the JavaFX module path:
```
java --module-path %PATH_TO_FX% --add-modules=javafx.controls JTableFX.jar
```

### Eclipse setup

If you are using Eclipse and your JDK does not bundle JavaFX:

1. Install a JDK 21 or newer.
2. Download the JavaFX SDK from [Gluon](https://gluonhq.com/products/javafx/).
3. In Eclipse, create a new *User Library*:
   - `Window -> Preferences -> Java -> Build Path -> User Libraries -> New`
4. Add the JARs from the JavaFX SDK `lib` folder to that library.
5. Add the library to your project's *Modulepath*:
   - `Project -> Properties -> Java Build Path -> Libraries -> Add Library`
6. Import the JTableFX source and build/run the project.

### Running from Windows Explorer

For easier launching of JavaFX JAR files from Windows, you can use the provided [run_javafx.cmd](run_javafx.cmd) helper script.

1. Create an environment variable named `PATH_TO_FX` pointing to the JavaFX SDK `lib` directory.
2. Place `run_javafx.cmd` in a suitable location.
3. Associate `.jar` files with `run_javafx.cmd`.

Double-clicking a runnable JAR should then open a command window and launch the JavaFX application.

## Built with

* [Java](https://en.wikipedia.org/wiki/Java_(programming_language)) - General-purpose computer programming language
* [JavaFX](https://en.wikipedia.org/wiki/JavaFX)  - Java software library used to generate the application's interactive graphical user interface
* [Eclipse](https://en.wikipedia.org/wiki/Eclipse_(software)) - Integrated development environment (IDE) used to develop the project
