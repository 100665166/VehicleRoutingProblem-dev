VRP solver details:

Relevant source code/data files:
	- problem1.json
	- problem2.json
	- DeliveryAgent
	- DeliveryAgentData
	- MasterRoutingAgent
	- Strings
	- Utilities
	- MainMenu
	- SolverInterface
	- VRP_Test3 (solver)

Program is run with Java 11.0.12, adopt-openjdk-11, Maven.

RUN THE PROGRAM using the main(String[] args) method of class VRP.MainMenu, runnable through the IDE with
working directory as the root directory of the project, VRP_Solver_Handin. This is because how the source
code accesses the data files in the program are relative to the root directory.

If you want to run a different data set (file or generated) close and reopen the program

IMPORTANT:

Component "com.tilab.jade, jade, 4.5.0" appears to no longer download from its Maven repository 
(https://jade.tilab.com/maven/) for our project.

As a work around if it won't download for you when Maven downloads pom.xml dependencies, 
we have provided a copy of jade 4.5.0 to manually place in your local Maven repository 
(C:/Users/[User]/.m2/repository/)

The "BackUpJadeLib" directory contains the copy of jade 4.5.0

Copy BackUpJadeLib/com/tilib/jade/jade/4.5.0/<all files> to
C:/Users/[User]/.m2/repository/com/tilib/jade/jade/4.5.0/<all files>
as a workaround.

Problem was discovered when clearing the .m2 repository and redownloading it.

BACKUP:

If any other components are not downloading, use the following google drive link to download a workable 
.m2 repository for the program: https://drive.google.com/file/d/1cyQ4UHCs1QE8aGGaOxQ9tEufKpsgCHny/view