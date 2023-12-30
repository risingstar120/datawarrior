## DataWarrior
*DataWarrior* is a program for interactive data analysis and visualization. While it is
widely used by data analysts in general, it is particular useful for cheminformaticians,
because chemical structures and reactions are native datatypes and because of its rich
cheminformatics functionality.

*DataWarrior* runs on all major platforms. Pre-built installers exist for Linux, Macintosh and Windows.
Software installers, documentation, sample data, and a support forum are available on
https://openmolecules.org/datawarrior.

*DataWarrior* is built on the open-source projects *OpenChemLib* and *FXMolViewer*. 

### Dependencies
Apart from a working JDK11 or higher that includes JavaFX, *DataWarrior* needs various free-to-use/open-source
dependencies. All required dependency files are provided as part of this project in the ./lib folder.
The most important ones are:
* OpenChemLib: Cheminformatics base functionality to handle molecules and reactions
* FXMolViewer: 3D-molecule & protein visualization, editing, interaction using JavaFX
  (includes Sunflow ray-tracer and MMTF to download and parse binary structure files from the PDB-database)
* Batik: Support for SVG vector graphics
* Database connectors for: MySQL, PostgreSQL, Oracle, Microsoft SQL-Server
* Opsin: IUPAC name conversion to chemical structures
* Substance Look&Feel: professionally designed user interface skin
* Java Expression Parser: for calculating new column data using custom equations

### How to download the project
git clone https://github.com/thsa/datawarrior.git

### How to build the project
On Linux or Macintosh just run the 'buildDataWarrior' shell script.

### How to run the project
After building the project just run the 'runDataWarrior' shell script.

### Platform Integration
Ideally, *DataWarrior* should be installed in a platform specific way that registers its file
extentions and causes proper file icon display. Installers for Linux, Macintosh, and Windows,
which include proper platform integration, can be downloaded from
https://openmolecules.org/datawarrior/download.html.

This platform integration is not (yet) part of this project. Thus, if you intend to run *DataWarrior*
from self compiled source code and if you don't do the platform integration yourself, then
you may still install *DataWarrior* with the official installer and afterwards replace the original
datawarrior.jar file with the freshly built one.

Unfortunately, this does not work on Windows platforms, because a proper platform integration on
Windows requires the application to be an .exe file. Therefore, on Windows the datawarrior.jar,
the application icon and all document icons are embedded in the DataWarrior.exe file as resources.

Note: The datawarrior.jar file of the platform specific installers are shrunk with *Proguard*,
to reduce the datawarrior.jar file size significantly be the removal of unused classes and methods
from the byte code.

### How to contribute
Contact the author under the e-mail shown on https://openmolecules.org/about.html


### License
*DataWarrior*. Copyright (C) 2023 Thomas Sander & Idorsia Pharmaceuticals Ltd.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.


### Supported by
<img alt="YourKit-logo" src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAALkAAAAsBAMAAADLKASfAAAAD1BMVEUAAAA7Pit5TSflaCD///9M0XeLAAAAAXRSTlMAQObYZgAAAi1JREFUWMPdlttxAyEMRaUOkDcN2FuBTQWecf81hYfQA7D3I+yHw2T8iMXhIi7SAvBA87p8IJU3CqfQqXLpFDzuBYv7lc6Ax3vGUnycIP4SK3aLaZXVeIppJGraQoyrc1OgmYqRl1mcl5hTwh/Wpp4alLb2QWxaHZrfP1+WEoclrgbzv/LcWDNCJUMpRR1dIg/oNbyjI2u+yx7CqCl8voo5oEqHfg6Lf2yddKsJjuh2DTenid9ZuqPTX+mSeZYePP0wMSBhNNsv8nnWs6XJNDimkxNh98umLK63Oo3F3tZ+7MJGuor3Otu0Zubg9lOSgPkvjZY/+UW3gk28l15D1W5lZRXKvxZ6GBZXw9PPk8U/7Km2KDaEnFdQBiqr7rqjp9dXGpL5eLPHTWSly22xDPkxuKOqwT+vMp5qG600Qieh43s6vKe/NqVHV2nkQnm6LDnRLncEG32mHQbBYaDLmQidHVY3P9K1+5G93ko33wFM2oar2ujPKd1d787pYG2Cczo0utSxXGtW00shu+7+UCd9oT/arswM9HKstUxea0WY001j87Wzp9syk03zpEanm7WM605z59t2So7eLJnhxGUmVZy7r/ABuqs6oYeu0yidCvzS6HTRQx1Md0APjm5ujNJtF7C2MPNcVzmmB2B6QNcFvARROqEz0BhTJgYQum90trWTPukjmUC0TjdVvc3L3zd9yKOVz6m1Xxo6LH+Ex239A+o/ofdO/0I6fRH9F/QFn5WZ2jM+AAAAAElFTkSuQmCC" />
YourKit supports open source projects with innovative and intelligent tools 
for monitoring and profiling Java and .NET applications.
YourKit is the creator of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/),
[YourKit .NET Profiler](https://www.yourkit.com/dotnet-profiler/),
and [YourKit YouMonitor](https://www.yourkit.com/youmonitor/).
