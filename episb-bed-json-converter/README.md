commands to run code: 

sbt "run <regions/ directory from LOLA core db or a directory complying to such a format>

The program will create matching .jsonld fiules for each bed file found in index.txt listing as per LOLA database. It will also attempt to load the json documents into elasticsearch server on localhost.

Code is VERY beta - needs a lot more polishing, refactoring, cleaning up and error checking.
